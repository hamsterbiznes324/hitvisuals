package com.example.hitvisuals.mixin;

import com.example.hitvisuals.SkyPresets;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Меняет цвет неба. */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void hitvisuals$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        if (SkyPresets.active()) {
            cir.setReturnValue(0xFF000000 | SkyPresets.skyRgb());
        }
    }
}
