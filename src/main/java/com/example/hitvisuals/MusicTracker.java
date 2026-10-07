package com.example.hitvisuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Читает, какая музыка сейчас играет в Windows (Spotify, браузер и т.д.),
 * и подгружает синхронный текст песни с сервиса lrclib.net.
 */
public final class MusicTracker {
    /** Текст песни с временными метками (в секундах). */
    public record Lyrics(double[] times, String[] lines) {}

    public static final int LY_NONE = 0;
    public static final int LY_LOADING = 1;
    public static final int LY_OK = 2;

    private static final String SCRIPT = """
            [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
            Add-Type -AssemblyName System.Runtime.WindowsRuntime
            $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
            function Await($op, $type) {
              $m = $asTaskGeneric.MakeGenericMethod($type)
              $t = $m.Invoke($null, @($op))
              $t.Wait(-1) | Out-Null
              $t.Result
            }
            [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime]
            [void][Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties, Windows.Media.Control, ContentType=WindowsRuntime]
            $mgr = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
            while ($true) {
              try {
                $s = $mgr.GetCurrentSession()
                if ($s -ne $null) {
                  $p = Await ($s.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
                  $tl = $s.GetTimelineProperties()
                  $pb = $s.GetPlaybackInfo()
                  $o = [ordered]@{ title = [string]$p.Title; artist = [string]$p.Artist; playing = ($pb.PlaybackStatus -eq 'Playing'); pos = [double]$tl.Position.TotalSeconds; dur = [double](($tl.EndTime - $tl.StartTime).TotalSeconds) }
                  Write-Output ($o | ConvertTo-Json -Compress)
                } else {
                  Write-Output '{"none":true}'
                }
              } catch {
                Write-Output '{"none":true}'
              }
              Start-Sleep -Milliseconds 700
            }
            """;

    private static final Pattern LRC = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\]\\s*(.*)$");

    private static Process process;
    private static boolean failed = false;
    private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "HamsterVisuals-Lyrics");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean present = false;
    private static volatile boolean playing = false;
    private static volatile String title = "";
    private static volatile String artist = "";
    private static volatile double pos = 0;
    private static volatile double dur = 0;
    private static volatile long stampNanos = 0;
    private static volatile Lyrics lyrics = null;
    private static volatile int lyricsState = LY_NONE;
    private static volatile String lyricsKey = "";
    private static double lastReported = -1;

    public static boolean present() {
        return present;
    }

    public static boolean playing() {
        return playing;
    }

    public static String title() {
        return title;
    }

    public static String artist() {
        return artist;
    }

    public static double duration() {
        return dur;
    }

    public static Lyrics lyrics() {
        return lyrics;
    }

    public static int lyricsState() {
        return lyricsState;
    }

    /** Текущая позиция трека в секундах (с поправкой на прошедшее время). */
    public static double position() {
        double p = pos;
        if (playing) {
            p += (System.nanoTime() - stampNanos) / 1e9;
        }
        if (dur > 0) p = Math.min(p, dur);
        return Math.max(0, p);
    }

    /** Включает или выключает фоновое чтение музыки. Вызывается каждый тик. */
    public static synchronized void update(boolean enabled) {
        if (enabled) {
            if (process == null && !failed) start();
        } else if (process != null) {
            stop();
        }
    }

    public static synchronized void shutdown() {
        stop();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void start() {
        if (!isWindows()) {
            failed = true;
            return;
        }
        try {
            String b64 = Base64.getEncoder().encodeToString(SCRIPT.getBytes(StandardCharsets.UTF_16LE));
            ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive",
                    "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-EncodedCommand", b64);
            pb.redirectErrorStream(true);
            final Process p = pb.start();
            process = p;
            Thread t = new Thread(() -> readLoop(p), "HamsterVisuals-Music");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            failed = true;
            System.err.println("[HamsterVisuals] Не удалось запустить чтение музыки: " + e);
        }
    }

    private static void stop() {
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
        present = false;
    }

    private static void readLoop(Process p) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("{")) {
                    handle(line);
                }
            }
        } catch (Exception ignored) {
        }
        present = false;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return "";
        try {
            return e.getAsString();
        } catch (Exception ex) {
            return "";
        }
    }

    private static double num(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) return 0;
        try {
            return e.getAsDouble();
        } catch (Exception ex) {
            return 0;
        }
    }

    private static void handle(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("none")) {
                present = false;
                return;
            }
            String t = str(o, "title");
            String a = str(o, "artist");
            if (t.isEmpty()) {
                present = false;
                return;
            }
            double reported = num(o, "pos");
            dur = num(o, "dur");
            boolean nowPlaying = o.has("playing") && o.get("playing").getAsBoolean();
            long now = System.nanoTime();
            String key0 = t + "|" + a;
            if (!key0.equals(lyricsKey)) {
                // новый трек: считаем с начала
                pos = reported;
                stampNanos = now;
                lastReported = reported;
            } else if (Math.abs(reported - lastReported) > 0.001) {
                // плеер сообщил новую позицию, подстраиваемся под неё
                pos = reported;
                stampNanos = now;
                lastReported = reported;
            } else if (playing != nowPlaying) {
                // пауза или продолжение: запоминаем, где остановились
                pos = position();
                stampNanos = now;
            }
            if (dur > 0 && nowPlaying && position() > dur + 2.0) {
                // трек пошёл по кругу
                pos = 0;
                stampNanos = now;
            }
            playing = nowPlaying;
            title = t;
            artist = a;
            present = true;

            String key = t + "|" + a;
            if (!key.equals(lyricsKey)) {
                lyricsKey = key;
                lyrics = null;
                lyricsState = LY_NONE;
                if (VisualsConfig.I.musicLyrics) {
                    lyricsState = LY_LOADING;
                    final String ft = t;
                    final String fa = a;
                    POOL.submit(() -> fetchLyrics(ft, fa, key));
                }
            }
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------ тексты песен

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static JsonElement getJson(HttpClient client, String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "HamsterVisuals/1.1")
                    .GET().build();
            HttpResponse<String> r = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (r.statusCode() != 200) return null;
            return JsonParser.parseString(r.body());
        } catch (Exception e) {
            return null;
        }
    }

    private static void fetchLyrics(String t, String a, String key) {
        String synced = "";
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
            JsonElement el = getJson(client, "https://lrclib.net/api/get?track_name=" + enc(t) + "&artist_name=" + enc(a));
            if (el != null && el.isJsonObject()) {
                synced = str(el.getAsJsonObject(), "syncedLyrics");
            }
            if (synced.isEmpty()) {
                JsonElement sr = getJson(client, "https://lrclib.net/api/search?q=" + enc((a + " " + t).trim()));
                if (sr != null && sr.isJsonArray()) {
                    JsonArray arr = sr.getAsJsonArray();
                    for (JsonElement item : arr) {
                        if (!item.isJsonObject()) continue;
                        String s = str(item.getAsJsonObject(), "syncedLyrics");
                        if (!s.isEmpty()) {
                            synced = s;
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (!key.equals(lyricsKey)) return;
        Lyrics parsed = parse(synced);
        lyrics = parsed;
        lyricsState = parsed == null ? LY_NONE : LY_OK;
    }

    private static Lyrics parse(String lrc) {
        if (lrc == null || lrc.isEmpty()) return null;
        List<Double> times = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        for (String raw : lrc.split("\n")) {
            Matcher m = LRC.matcher(raw.trim());
            if (!m.matches()) continue;
            try {
                double sec = Integer.parseInt(m.group(1)) * 60.0 + Double.parseDouble(m.group(2));
                times.add(sec);
                lines.add(m.group(3).trim());
            } catch (Exception ignored) {
            }
        }
        if (times.isEmpty()) return null;
        double[] ts = new double[times.size()];
        for (int i = 0; i < ts.length; i++) ts[i] = times.get(i);
        return new Lyrics(ts, lines.toArray(new String[0]));
    }

    private MusicTracker() {}
}
