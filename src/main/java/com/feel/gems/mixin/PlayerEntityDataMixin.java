package com.feel.gems.mixin;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Player.class)
public abstract class PlayerEntityDataMixin implements GemsPersistentDataHolder {
    @Unique
    private static final String GEMS_PERSISTED_KEY = "gems";

    @Unique
    private CompoundTag gems$persistentData;

    @Override
    public CompoundTag gems$getPersistentData() {
        if (gems$persistentData == null) {
            gems$persistentData = new CompoundTag();
        }
        return gems$persistentData;
    }

    @Override
    public void gems$setPersistentData(CompoundTag nbt) {
        gems$persistentData = (nbt == null) ? new CompoundTag() : nbt;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void gems$writeCustomData(ValueOutput view, CallbackInfo ci) {
        view.store(GEMS_PERSISTED_KEY, CompoundTag.CODEC, gems$getPersistentData());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void gems$readCustomData(ValueInput view, CallbackInfo ci) {
        gems$persistentData = view.read(GEMS_PERSISTED_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }
}
