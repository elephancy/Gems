package com.feel.gems.power.api;

import net.minecraft.server.level.ServerPlayer;




/**
 * A passive that needs periodic maintenance (kept deliberately low-frequency).
 */
public interface GemMaintainedPassive extends GemPassive {
    void maintain(ServerPlayer player);
}

