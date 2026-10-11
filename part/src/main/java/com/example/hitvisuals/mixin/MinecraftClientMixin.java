package com.example.hitvisuals.mixin;

import com.example.hitvisuals.CustomTitleScreen;
import com.example.hitvisuals.VisualsConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Подменяет стандартное главное меню на своё. */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private Screen hitvisuals$title(Screen screen) {
        if (VisualsConfig.I.customTitle && screen instanceof TitleScreen) {
            return new CustomTitleScreen();
        }
        return screen;
    }
}
