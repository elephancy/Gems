package com.feel.gems.power.passive;

import com.feel.gems.power.api.GemPassive;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;




/**
 * A passive implemented via event hooks / mixins rather than direct apply/remove.
 */
public final class MarkerPassive implements GemPassive {
    private final Identifier id;
    private final String name;
    private final String description;

    public MarkerPassive(Identifier id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void apply(ServerPlayer player) {
        // no-op
    }

    @Override
    public void remove(ServerPlayer player) {
        // no-op
    }
}

