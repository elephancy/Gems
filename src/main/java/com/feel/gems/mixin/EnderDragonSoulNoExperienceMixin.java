package com.feel.gems.mixin;

import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;




@Mixin(EnderDragon.class)
public abstract class EnderDragonSoulNoExperienceMixin {
    @Redirect(
            method = "tickDeath",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"
            ),
            require = 0
    )
    private void gems$soulNoDragonXp(ServerLevel world, Vec3 pos, int amount) {
        EnderDragon self = (EnderDragon) (Object) this;
        if (SoulSummons.isSoul(self) || SummonerSummons.isSummon(self)) {
            return;
        }
        ExperienceOrb.award(world, pos, amount);
    }
}

