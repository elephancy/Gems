package com.feel.gems.mixin;

import com.feel.gems.power.ability.trickster.TricksterMindGamesRuntime;
import com.feel.gems.power.ability.trickster.TricksterPuppetRuntime;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Mob.class)
public abstract class MobEntityTricksterEffectsMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void gems$tickTricksterEffects(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!(self.level() instanceof ServerLevel world)) {
            return;
        }
        // Avoid per-tick work on every mob: only do anything if the mob has relevant tags.
        Set<String> tags = self.entityTags();
        if (tags.isEmpty()) {
            return;
        }
        boolean puppeted = tags.contains("gems_puppeted");
        boolean confused = tags.contains("gems_confused");
        if (!puppeted && !confused) {
            return;
        }
        // Puppeting needs to feel responsive; mind games can be a bit lower frequency.
        if (puppeted) {
            TricksterPuppetRuntime.tickPuppetedMob(self, world);
        }
        if (confused && world.getGameTime() % 5 == 0) {
            TricksterMindGamesRuntime.tickConfusedMob(self, world);
        }
    }
}
