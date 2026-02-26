package com.feel.gems.power.ability.wealth;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.screen.PocketsScreenHandler;
import com.feel.gems.screen.PocketsStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import com.feel.gems.item.GemOwnership;


public final class PocketsAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.POCKETS;
    }

    @Override
    public String name() {
        return "Pockets";
    }

    @Override
    public String description() {
        return "Opens an extra inventory.";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean activate(ServerPlayer player) {
        SimpleContainer pockets = PocketsStorage.load(player);
        GemOwnership.purgeInventory(player.level().getServer(), pockets);
        MenuProvider factory = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("gems.ability.wealth.pockets.title");
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inv, net.minecraft.world.entity.player.Player p) {
                return new PocketsScreenHandler(syncId, inv, player, pockets);
            }
        };
        player.openMenu(factory);
        AbilityFeedback.sound(player, SoundEvents.CHEST_OPEN, 0.8F, 1.1F);
        return true;
    }
}
