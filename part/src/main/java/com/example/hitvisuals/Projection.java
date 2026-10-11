package com.example.hitvisuals;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;

/** Переводит точку мира в координаты на экране (без графических API, только математика). */
public final class Projection {
    public static final class P {
        public double x;
        public double y;
        public double depth;
        public double focal;
        public boolean front;
    }

    public static P project(MinecraftClient mc, int sw, int sh, double wx, double wy, double wz) {
        Camera cam = mc.gameRenderer.getCamera();
        Vec3d cp = cam.getPos();
        double yaw = Math.toRadians(cam.getYaw());
        double pitch = Math.toRadians(cam.getPitch());
        double cyaw = Math.cos(yaw);
        double syaw = Math.sin(yaw);
        double cpit = Math.cos(pitch);
        double spit = Math.sin(pitch);

        double fx = -syaw * cpit;
        double fy = -spit;
        double fz = cyaw * cpit;
        double rx = -cyaw;
        double rz = -syaw;
        double ux = -rz * fy;
        double uy = rz * fx - rx * fz;
        double uz = rx * fy;

        double dx = wx - cp.x;
        double dy = wy - cp.y;
        double dz = wz - cp.z;
        double xc = dx * rx + dz * rz;
        double yc = dx * ux + dy * uy + dz * uz;
        double zc = dx * fx + dy * fy + dz * fz;

        double fov = Math.toRadians(mc.options.getFov().getValue());
        double focal = (sh / 2.0) / Math.tan(fov / 2.0);

        P p = new P();
        p.focal = focal;
        p.depth = zc;
        p.front = zc > 0.05;
        if (p.front) {
            p.x = sw / 2.0 + xc / zc * focal;
            p.y = sh / 2.0 - yc / zc * focal;
        }
        return p;
    }

    private Projection() {}
}
