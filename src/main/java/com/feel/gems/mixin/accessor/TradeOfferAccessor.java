package com.feel.gems.mixin.accessor;

import java.util.Optional;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantOffer.class)
public interface TradeOfferAccessor {
    @Accessor("uses")
    int gems$getUses();

    @Accessor("uses")
    @Mutable
    void gems$setUses(int uses);

    @Accessor("maxUses")
    int gems$getMaxUses();

    @Accessor("maxUses")
    @Mutable
    void gems$setMaxUses(int maxUses);

    @Accessor("costB")
    Optional<ItemCost> gems$getSecondBuyItem();

    @Accessor("costB")
    @Mutable
    void gems$setSecondBuyItem(Optional<ItemCost> secondBuyItem);
}
