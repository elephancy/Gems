package com.feel.gems.client;

import com.feel.gems.client.entity.HunterPackEntityRenderer;
import com.feel.gems.client.entity.ShadowCloneEntityRenderer;
import com.feel.gems.client.entity.TestDummyEntityRenderer;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.net.ClientPassiveTogglePayload;
import com.feel.gems.particle.ModParticles;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.particle.EndRodParticle;

/** Standalone Gems client hooks. */
public final class GemsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        GemsClientConfigManager.loadOrCreate();
        GemsKeybinds.register();
        ParticleProviderRegistry.getInstance().register(ModParticles.GEM_STAR, EndRodParticle.Provider::new);
        ParticleProviderRegistry.getInstance().register(ModParticles.GEM_RING, EndRodParticle.Provider::new);
        ClientTickEvents.END_CLIENT_TICK.register(GemsKeybinds::tick);
        com.feel.gems.client.hud.GemsHud.register();
        com.feel.gems.client.hud.GemsTooltips.register();
        MenuScreens.register(com.feel.gems.screen.ModScreenHandlers.TRADER, com.feel.gems.client.screen.TraderScreen::new);
        MenuScreens.register(com.feel.gems.screen.ModScreenHandlers.GEM_SEER, com.feel.gems.client.screen.GemSeerScreen::new);
        EntityRendererRegistry.register(ModEntities.SHADOW_CLONE, ShadowCloneEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.HUNTER_PACK, HunterPackEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.TEST_DUMMY, TestDummyEntityRenderer::new);
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            GemsClientConfig cfg = GemsClientConfigManager.config();
            ClientPlayNetworking.send(new ClientPassiveTogglePayload(cfg.passivesEnabled));
        });
    }
}
