package com.example.hitvisuals;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.awt.Color;

/** Цветная подсветка блока, на который наведён прицел. */
public final class BlockHighlight {
    public static void render(WorldRenderContext ctx) {
        VisualsConfig c = VisualsConfig.I;
        if (!c.blockHighlight) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) return;
        HitResult hr = mc.crosshairTarget;
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = bhr.getBlockPos();
        BlockState state = mc.world.getBlockState(pos);
        VoxelShape shape = state.getOutlineShape(mc.world, pos);
        if (shape.isEmpty()) return;

        MatrixStack ms = ctx.matrixStack();
        VertexConsumerProvider vcp = ctx.consumers();
        if (ms == null || vcp == null) return;

        int rgb;
        if (c.blockColorMode == 1) {
            rgb = Color.HSBtoRGB((System.currentTimeMillis() % 4000L) / 4000f, 0.7f, 1f) & 0xFFFFFF;
        } else if (c.blockColorMode == 2) {
            rgb = 0xFFFFFF;
        } else {
            rgb = Ui.accent();
        }
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;

        Box box = shape.getBoundingBox().offset(pos).expand(0.003);
        Vec3d cam = ctx.camera().getPos();
        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);
        MatrixStack.Entry e = ms.peek();

        VertexConsumer lines = vcp.getBuffer(RenderLayer.getLines());
        // нижняя и верхняя рамки
        line(lines, e, x1, y1, z1, x2, y1, z1, r, g, b, 235);
        line(lines, e, x2, y1, z1, x2, y1, z2, r, g, b, 235);
        line(lines, e, x2, y1, z2, x1, y1, z2, r, g, b, 235);
        line(lines, e, x1, y1, z2, x1, y1, z1, r, g, b, 235);
        line(lines, e, x1, y2, z1, x2, y2, z1, r, g, b, 235);
        line(lines, e, x2, y2, z1, x2, y2, z2, r, g, b, 235);
        line(lines, e, x2, y2, z2, x1, y2, z2, r, g, b, 235);
        line(lines, e, x1, y2, z2, x1, y2, z1, r, g, b, 235);
        // вертикальные рёбра
        line(lines, e, x1, y1, z1, x1, y2, z1, r, g, b, 235);
        line(lines, e, x2, y1, z1, x2, y2, z1, r, g, b, 235);
        line(lines, e, x2, y1, z2, x2, y2, z2, r, g, b, 235);
        line(lines, e, x1, y1, z2, x1, y2, z2, r, g, b, 235);

        int fa = (int) (255 * c.blockFill);
        if (fa > 2) {
            VertexConsumer q = vcp.getBuffer(RenderLayer.getDebugQuads());
            quad(q, e, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, r, g, b, fa);
            quad(q, e, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, r, g, b, fa);
            quad(q, e, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, r, g, b, fa);
            quad(q, e, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, r, g, b, fa);
            quad(q, e, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, r, g, b, fa);
            quad(q, e, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, r, g, b, fa);
        }
        ms.pop();
    }

    private static void line(VertexConsumer vc, MatrixStack.Entry e,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             int r, int g, int b, int a) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) return;
        nx /= len;
        ny /= len;
        nz /= len;
        vc.vertex(e, x1, y1, z1).color(r, g, b, a).normal(e, nx, ny, nz);
        vc.vertex(e, x2, y2, z2).color(r, g, b, a).normal(e, nx, ny, nz);
    }

    private static void quad(VertexConsumer q, MatrixStack.Entry e,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             int r, int g, int b, int a) {
        q.vertex(e, ax, ay, az).color(r, g, b, a);
        q.vertex(e, bx, by, bz).color(r, g, b, a);
        q.vertex(e, cx, cy, cz).color(r, g, b, a);
        q.vertex(e, dx, dy, dz).color(r, g, b, a);
    }

    private BlockHighlight() {}
}
