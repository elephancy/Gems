package com.feel.gems.screen;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;




public final class PocketsScreenHandler extends ChestMenu {
    private final ServerPlayer owner;
    private final SimpleContainer pockets;

    public PocketsScreenHandler(int syncId, Inventory playerInventory, ServerPlayer owner, SimpleContainer pockets) {
        super(typeForRows(rowsFor(pockets)), syncId, playerInventory, pockets, rowsFor(pockets));
        this.owner = owner;
        this.pockets = pockets;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer == owner) {
            PocketsStorage.save(owner, pockets);
        }
    }

    private static int rowsFor(SimpleContainer pockets) {
        int size = pockets == null ? 0 : pockets.getContainerSize();
        int rows = size / 9;
        return Math.max(1, Math.min(6, rows));
    }

    private static MenuType<?> typeForRows(int rows) {
        return switch (rows) {
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            case 1 -> MenuType.GENERIC_9x1;
            default -> MenuType.GENERIC_9x1;
        };
    }
}

