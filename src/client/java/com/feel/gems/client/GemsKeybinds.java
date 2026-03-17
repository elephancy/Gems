package com.feel.gems.client;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.ActivateAbilityPayload;
import com.feel.gems.net.ActivateBonusAbilityPayload;
import com.feel.gems.net.AugmentOpenRequestPayload;
import com.feel.gems.net.InscriptionOpenRequestPayload;
import com.feel.gems.net.BonusSelectionOpenRequestPayload;
import com.feel.gems.net.FluxChargePayload;
import com.feel.gems.net.LoadoutOpenRequestPayload;
import com.feel.gems.net.PrismSelectionOpenRequestPayload;
import com.feel.gems.net.SpyObservedOpenRequestPayload;
import com.feel.gems.net.SoulReleasePayload;
import com.feel.gems.net.SummonerLoadoutOpenRequestPayload;
import com.feel.gems.net.TitleSelectionOpenRequestPayload;
import com.feel.gems.net.TrophyNecklaceOpenRequestPayload;
import com.feel.gems.client.screen.GuidebookScreen;
import com.feel.gems.item.GemItem;
import com.feel.gems.legendary.LegendaryItem;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;




public final class GemsKeybinds {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("gems", "controls"));

    private static boolean registered = false;
    private static KeyMapping MODIFIER;
    private static KeyMapping[] CUSTOM_KEYS;
    private static KeyMapping[] BONUS_ABILITY_KEYS;
    private static KeyMapping BONUS_SCREEN_KEY;
    private static KeyMapping SPY_OBSERVED_SCREEN_KEY;
    private static KeyMapping TROPHY_NECKLACE_SCREEN_KEY;
    private static KeyMapping LOADOUT_PRESETS_KEY;
    private static KeyMapping AUGMENT_SCREEN_KEY;
    private static KeyMapping GUIDEBOOK_KEY;
    private static KeyMapping TITLE_SCREEN_KEY;
    private static KeyMapping TOGGLE_CONTROL_MODE_KEY;

    private GemsKeybinds() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;

        MODIFIER = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.modifier", InputConstants.KEY_LALT, CATEGORY));

        // Always register the custom keybinds so they appear in the Controls menu even if the user
        // starts in chord mode and switches later via config.
        CUSTOM_KEYS = new KeyMapping[9];
        for (int i = 0; i < CUSTOM_KEYS.length; i++) {
            CUSTOM_KEYS[i] = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.gems.ability_" + (i + 1), InputConstants.UNKNOWN.getValue(), CATEGORY));
        }
        
        // Bonus ability keybinds (C and V by default, customizable via Controls)
        BONUS_ABILITY_KEYS = new KeyMapping[2];
        BONUS_ABILITY_KEYS[0] = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.bonus_ability_1", InputConstants.KEY_C, CATEGORY));
        BONUS_ABILITY_KEYS[1] = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.bonus_ability_2", InputConstants.KEY_V, CATEGORY));
        
        // Bonus selection screen keybind (B by default)
        BONUS_SCREEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.bonus_screen", InputConstants.KEY_B, CATEGORY));

        // Spy observed abilities screen keybind (O by default)
        SPY_OBSERVED_SCREEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.spy_observed_screen", InputConstants.KEY_O, CATEGORY));

        // Trophy Necklace screen keybind (P by default)
        TROPHY_NECKLACE_SCREEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.trophy_necklace_screen", InputConstants.KEY_P, CATEGORY));

        // Loadout presets screen keybind (L by default)
        LOADOUT_PRESETS_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.loadout_presets", InputConstants.KEY_L, CATEGORY));

        // Augment management screen keybind (U by default)
        AUGMENT_SCREEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.augment_screen", InputConstants.KEY_U, CATEGORY));

        // Guidebook keybind (tilde by default)
        GUIDEBOOK_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.guidebook", InputConstants.KEY_GRAVE, CATEGORY));

        // Title selection screen keybind (M by default)
        TITLE_SCREEN_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.gems.title_screen", InputConstants.KEY_M, CATEGORY));

        // Toggle control mode keybind (unbound by default - user can assign)
        TOGGLE_CONTROL_MODE_KEY = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.gems.toggle_control_mode", InputConstants.UNKNOWN.getValue(), CATEGORY));
    }

    public static boolean isModifierDown() {
        return useChordControls() && MODIFIER != null && MODIFIER.isDown();
    }

    public static String modifierLabel() {
        if (!useChordControls() || MODIFIER == null) {
            return "";
        }
        return MODIFIER.getTranslatedKeyMessage().getString();
    }

    public static String bonusScreenLabel() {
        if (BONUS_SCREEN_KEY == null) {
            return "";
        }
        return BONUS_SCREEN_KEY.getTranslatedKeyMessage().getString();
    }

    public static String bonusAbilityLabel(int bonusSlotIndex) {
        if (BONUS_ABILITY_KEYS == null || bonusSlotIndex < 0 || bonusSlotIndex >= BONUS_ABILITY_KEYS.length) {
            return "";
        }
        KeyMapping key = BONUS_ABILITY_KEYS[bonusSlotIndex];
        if (key == null) {
            return "";
        }
        return key.getTranslatedKeyMessage().getString();
    }

    public static String chordSlotLabel(int slotNumber) {
        if (!useChordControls()) {
            return customSlotLabel(slotNumber);
        }
        String key = chordKeyLabel(slotNumber);
        if (key.isEmpty()) {
            return "";
        }
        String modifier = modifierLabel();
        if (modifier.isEmpty()) {
            return key;
        }
        return modifier + " + " + key;
    }

    public static void tick(Minecraft client) {
        // Check bonus screen key (works regardless of control mode)
        if (BONUS_SCREEN_KEY != null && client.gui.screen() == null) {
            while (BONUS_SCREEN_KEY.consumeClick()) {
                openBonusScreen(client);
            }
        }

        // Spy observed abilities screen key
        if (SPY_OBSERVED_SCREEN_KEY != null && client.gui.screen() == null) {
            while (SPY_OBSERVED_SCREEN_KEY.consumeClick()) {
                openSpyObservedScreen(client);
            }
        }

        // Trophy Necklace screen key
        if (TROPHY_NECKLACE_SCREEN_KEY != null && client.gui.screen() == null) {
            while (TROPHY_NECKLACE_SCREEN_KEY.consumeClick()) {
                openTrophyNecklaceScreen(client);
            }
        }

        // Loadout presets screen key
        if (LOADOUT_PRESETS_KEY != null && client.gui.screen() == null) {
            while (LOADOUT_PRESETS_KEY.consumeClick()) {
                openLoadoutPresetsScreen(client);
            }
        }

        // Augment screen key
        if (AUGMENT_SCREEN_KEY != null && client.gui.screen() == null) {
            while (AUGMENT_SCREEN_KEY.consumeClick()) {
                openAugmentScreen(client);
            }
        }

        // Guidebook key
        if (GUIDEBOOK_KEY != null && client.gui.screen() == null) {
            while (GUIDEBOOK_KEY.consumeClick()) {
                GuidebookScreen.open(client);
            }
        }

        // Title selection screen key
        if (TITLE_SCREEN_KEY != null && client.gui.screen() == null) {
            while (TITLE_SCREEN_KEY.consumeClick()) {
                openTitleSelectionScreen(client);
            }
        }

        // Toggle control mode key
        if (TOGGLE_CONTROL_MODE_KEY != null) {
            while (TOGGLE_CONTROL_MODE_KEY.consumeClick()) {
                toggleControlMode(client);
            }
        }
        
        // Check bonus ability keybinds (work regardless of control mode)
        if (BONUS_ABILITY_KEYS != null && client.gui.screen() == null) {
            for (int i = 0; i < BONUS_ABILITY_KEYS.length; i++) {
                while (BONUS_ABILITY_KEYS[i].consumeClick()) {
                    activateBonusAbility(client, i);
                }
            }
        }
        
        if (!useCustomControls() || CUSTOM_KEYS == null || client.gui.screen() != null) {
            return;
        }
        for (int i = 0; i < CUSTOM_KEYS.length; i++) {
            while (CUSTOM_KEYS[i].consumeClick()) {
                activateSlotChord(client, i + 1);
            }
        }
    }
    
    private static void activateBonusAbility(Minecraft client, int slotIndex) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Component.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        if (ClientGemState.energy() < 10) {
            sendActionBar(client, Component.translatable("gems.bonus.need_energy_use"));
            return;
        }
        ClientPlayNetworking.send(new ActivateBonusAbilityPayload(slotIndex));
    }
    
    private static void openBonusScreen(Minecraft client) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Component.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        
        // Prism gem uses B key to open Prism selection screen (requires energy 10)
        if (ClientGemState.activeGem() == GemId.PRISM) {
            if (ClientGemState.energy() < 10) {
                sendActionBar(client, Component.translatable("gems.prism.need_energy_access"));
                return;
            }
            ClientPlayNetworking.send(PrismSelectionOpenRequestPayload.INSTANCE);
            return;
        }
        
        // All other gems use B key for bonus selection (requires energy 10+)
        if (ClientGemState.energy() < 10) {
            sendActionBar(client, Component.translatable("gems.bonus.need_energy_access"));
            return;
        }
        ClientPlayNetworking.send(BonusSelectionOpenRequestPayload.INSTANCE);
    }

    private static void openTrophyNecklaceScreen(Minecraft client) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        ClientPlayNetworking.send(TrophyNecklaceOpenRequestPayload.INSTANCE);
    }

    private static void openSpyObservedScreen(Minecraft client) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Component.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        GemId gem = ClientGemState.activeGem();
        if (gem != GemId.SPY && gem != GemId.PRISM) {
            sendActionBar(client, Component.translatable("gems.spy.observed.not_spy"));
            return;
        }
        ClientPlayNetworking.send(SpyObservedOpenRequestPayload.INSTANCE);
    }

    private static void openTitleSelectionScreen(Minecraft client) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        ClientPlayNetworking.send(TitleSelectionOpenRequestPayload.INSTANCE);
    }

    private static void openLoadoutPresetsScreen(Minecraft client) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Component.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        ClientPlayNetworking.send(new LoadoutOpenRequestPayload(ClientGemState.activeGem()));
    }

    private static void openAugmentScreen(Minecraft client) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        if (client.player == null) {
            return;
        }
        boolean mainHandGem = client.player.getMainHandItem().getItem() instanceof GemItem;
        boolean offHandGem = client.player.getOffhandItem().getItem() instanceof GemItem;
        if (mainHandGem || offHandGem) {
            ClientPlayNetworking.send(new AugmentOpenRequestPayload(mainHandGem));
            return;
        }
        boolean mainHandLegendary = client.player.getMainHandItem().getItem() instanceof LegendaryItem;
        boolean offHandLegendary = client.player.getOffhandItem().getItem() instanceof LegendaryItem;
        if (mainHandLegendary || offHandLegendary) {
            ClientPlayNetworking.send(new InscriptionOpenRequestPayload(mainHandLegendary));
            return;
        }
        sendActionBar(client, Component.translatable("gems.augment.need_gem_or_legendary"));
    }

    private static void toggleControlMode(Minecraft client) {
        GemsClientConfig cfg = GemsClientConfigManager.config();
        if (cfg.controlMode == GemsClientConfig.ControlMode.CHORD) {
            cfg.controlMode = GemsClientConfig.ControlMode.CUSTOM;
            sendActionBar(client, Component.translatable("gems.controls.mode.custom"));
        } else {
            cfg.controlMode = GemsClientConfig.ControlMode.CHORD;
            sendActionBar(client, Component.translatable("gems.controls.mode.chord"));
        }
        GemsClientConfigManager.save(cfg);
    }

    public static boolean useChordControls() {
        return GemsClientConfigManager.config().controlMode == GemsClientConfig.ControlMode.CHORD;
    }

    private static boolean useCustomControls() {
        return GemsClientConfigManager.config().controlMode == GemsClientConfig.ControlMode.CUSTOM;
    }

    private static String customSlotLabel(int slotNumber) {
        if (CUSTOM_KEYS == null || slotNumber < 1 || slotNumber > CUSTOM_KEYS.length) {
            return "";
        }
        KeyMapping key = CUSTOM_KEYS[slotNumber - 1];
        if (key == null) {
            return "";
        }
        return key.getTranslatedKeyMessage().getString();
    }

    public static void activateSlotChord(Minecraft client, int slotNumber) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Component.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        ClientAbilitySelection.record(ClientGemState.activeGem(), slotNumber);

        GemDefinition def = GemRegistry.definition(ClientGemState.activeGem());
        int abilityCount = def.abilities().size();
        
        // Prism: uses selected abilities from ClientPrismState instead of gem definition
        if (ClientGemState.activeGem() == GemId.PRISM) {
            int prismAbilityCount = ClientPrismState.getAbilities().size();
            int abilityIndex = slotNumber - 1;
            if (abilityIndex < 0 || abilityIndex >= prismAbilityCount) {
                return;
            }
            // Server validates unlocks/cooldowns/suppression.
            ClientPlayNetworking.send(new ActivateAbilityPayload(abilityIndex));
            return;
        }

        // Chaos: has independent slots, each can be rolled or used
        if (ClientGemState.activeGem() == GemId.CHAOS) {
            int slotIndex = slotNumber - 1;
            if (slotIndex < 0 || slotIndex >= ClientChaosState.slotCount()) {
                return;
            }
            // Server handles both rolling new abilities and using existing ones
            ClientPlayNetworking.send(new ActivateAbilityPayload(slotIndex));
            return;
        }

        // Summoner: open loadout editor on the chord after the last ability slot.
        if (ClientGemState.activeGem() == GemId.SUMMONER && slotNumber == abilityCount + 1) {
            ClientPlayNetworking.send(SummonerLoadoutOpenRequestPayload.INSTANCE);
            return;
        }

        // Flux: insert "Charge" as slot 2 (between ability 1 and ability 2).
        if (ClientGemState.activeGem() == GemId.FLUX) {
            if (slotNumber == 2) {
                ClientPlayNetworking.send(FluxChargePayload.INSTANCE);
                return;
            }
            if (slotNumber >= 3) {
                int shiftedIndex = slotNumber - 2;
                if (shiftedIndex < 0 || shiftedIndex >= abilityCount) {
                    return;
                }
                ClientPlayNetworking.send(new ActivateAbilityPayload(shiftedIndex));
                return;
            }
        }

        // Soul release is always the key AFTER the gem's last ability (not "after unlocked abilities").
        if (ClientGemState.activeGem() == GemId.ASTRA && slotNumber == abilityCount + 1) {
            activateSoulRelease(client);
            return;
        }

        int abilityIndex = slotNumber - 1;
        if (abilityIndex < 0 || abilityIndex >= abilityCount) {
            return;
        }

        // Server validates unlocks/cooldowns/suppression.
        ClientPlayNetworking.send(new ActivateAbilityPayload(abilityIndex));
    }

    private static void activateSoulRelease(Minecraft client) {
        if (client.getConnection() == null) {
            sendActionBar(client, Component.translatable("gems.client.not_connected"));
            return;
        }
        if (!ClientGemState.isInitialized()) {
            sendActionBar(client, Component.translatable("gems.client.gem_state_not_synced"));
            return;
        }
        ClientAbilitySelection.record(ClientGemState.activeGem(), GemRegistry.definition(ClientGemState.activeGem()).abilities().size() + 1);
        if (ClientGemState.activeGem() != GemId.ASTRA) {
            return;
        }
        ClientPlayNetworking.send(SoulReleasePayload.INSTANCE);
    }

    private static void sendActionBar(Minecraft client, Component text) {
        if (client.player == null) {
            return;
        }
        client.player.sendSystemMessage(text);
    }

    private static String chordKeyLabel(int slotNumber) {
        if (slotNumber < 1 || slotNumber > 9) {
            return "";
        }
        Minecraft client = Minecraft.getInstance();
        Options options = client.options;
        if (options == null || options.keyHotbarSlots == null || options.keyHotbarSlots.length < slotNumber) {
            return "";
        }
        return options.keyHotbarSlots[slotNumber - 1].getTranslatedKeyMessage().getString();
    }
}
