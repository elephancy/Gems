package com.feel.gems.power.api;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;




public interface GemPassive {
    Identifier id();

    String name();

    String description();

    void apply(ServerPlayer player);

    void remove(ServerPlayer player);
}

