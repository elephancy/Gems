package com.feel.gems.power.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;




public interface GemAbility {
    Identifier id();

    String name();

    String description();

    int cooldownTicks();

    boolean activate(ServerPlayer player);
}

