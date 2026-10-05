package com.example.hitvisuals.mixin;

import com.example.hitvisuals.VisualsConfig;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Прячет огонь на экране, когда горит сам игрок. */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "isOnFire", at = @At("RETURN"), cancellable = true, require = 0)
    private void hitvisuals$noFire(CallbackInfoReturnable<Boolean> cir) {
        if (VisualsConfig.I.hideFire && cir.getReturnValue() && (Object) this instanceof ClientPlayerEntity) {
            cir.setReturnValue(false);
        }
    }
}
