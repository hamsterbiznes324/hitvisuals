package com.example.hitvisuals;

import net.fabricmc.api.ModInitializer;

public class HitVisualsMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ModParticles.registerTypes();
    }
}
