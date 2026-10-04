package com.example.hitvisuals.mixin;

import com.example.hitvisuals.SkyPresets;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.world.ClientWorld;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Меняет цвет тумана у горизонта, чтобы он сочетался с небом. */
@Mixin(BackgroundRenderer.class)
public class BackgroundRendererMixin {
    @Inject(method = "getFogColor", at = @At("RETURN"), cancellable = true, require = 0)
    private static void hitvisuals$fog(Camera camera, float tickDelta, ClientWorld world,
                                       int clampedViewDistance, float skyDarkness,
                                       CallbackInfoReturnable<Vector4f> cir) {
        if (SkyPresets.active()) {
            int rgb = SkyPresets.fogRgb();
            float r = ((rgb >> 16) & 0xFF) / 255f;
            float g = ((rgb >> 8) & 0xFF) / 255f;
            float b = (rgb & 0xFF) / 255f;
            cir.setReturnValue(new Vector4f(r, g, b, 1f));
        }
    }
}
