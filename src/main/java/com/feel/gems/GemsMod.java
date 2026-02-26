package com.feel.gems;

import com.feel.gems.command.GemsCommands;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.GemsPayloads;
import com.feel.gems.net.ServerAbilityNetworking;
import com.feel.gems.net.ServerAugmentNetworking;
import com.feel.gems.net.ServerBonusNetworking;
import com.feel.gems.net.ServerBountyNetworking;
import com.feel.gems.net.ServerClientConfigNetworking;
import com.feel.gems.net.ServerInscriptionNetworking;
import com.feel.gems.net.ServerLegendaryNetworking;
import com.feel.gems.net.ServerPrismNetworking;
import com.feel.gems.net.ServerSpyObservedNetworking;
import com.feel.gems.net.ServerSummonerNetworking;
import com.feel.gems.net.ServerTitleSelectionNetworking;
import com.feel.gems.net.ServerTrophyNecklaceNetworking;
import com.feel.gems.particle.ModParticles;
import com.feel.gems.screen.ModScreenHandlers;
import com.feel.gems.sound.ModSounds;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Standalone Gems initializer. Campaign content is registered by Second Chance. */
public final class GemsMod implements ModInitializer {
    public static final String MOD_ID = "gems";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Gems");
        GemRegistry.init();
        GemsBalance.init();
        GemsDisables.init();
        ModSounds.init();
        ModParticles.init();
        ModEntities.init();
        GemsPayloads.register();
        ModScreenHandlers.init();
        ModItems.init();
        ServerAbilityNetworking.register();
        ServerClientConfigNetworking.register();
        ServerLegendaryNetworking.register();
        ServerSummonerNetworking.register();
        ServerBonusNetworking.register();
        ServerPrismNetworking.register();
        ServerTrophyNecklaceNetworking.register();
        ServerSpyObservedNetworking.register();
        ServerAugmentNetworking.register();
        ServerInscriptionNetworking.register();
        ServerTitleSelectionNetworking.register();
        ServerBountyNetworking.register();
        GemsModEvents.register();
        GemsCommands.register();
    }
}
