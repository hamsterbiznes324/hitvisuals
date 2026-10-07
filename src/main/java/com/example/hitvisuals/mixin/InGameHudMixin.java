package com.example.hitvisuals.mixin;

import com.example.hitvisuals.VisualsConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Прячет стандартный прицел, когда включён свой. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void hitvisuals$crosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        VisualsConfig c = VisualsConfig.I;
        if (c.customCrosshair && c.hideVanillaCross) {
            ci.cancel();
        }
    }
}
