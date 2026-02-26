package com.feel.gems.state;

import net.minecraft.nbt.CompoundTag;




public interface GemsPersistentDataHolder {
    CompoundTag gems$getPersistentData();

    void gems$setPersistentData(CompoundTag nbt);
}
