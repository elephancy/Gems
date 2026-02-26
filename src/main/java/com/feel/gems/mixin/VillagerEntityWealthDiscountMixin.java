package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.mixin.accessor.TradeOfferAccessor;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public final class VillagerEntityWealthDiscountMixin {
    private static final int GEMS_WEALTH_MAX_USES = 999999;

    @Unique
    private MerchantOffers gems$wealthOffersBackup;

    @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
    private void gems$applyWealthDiscount(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!GemPowers.isPassiveActive(serverPlayer, PowerIds.WEALTH_CURED_PRICES)) {
            return;
        }
        Villager villager = (Villager) (Object) this;
        MerchantOffers offers = villager.getOffers();
        if (gems$wealthOffersBackup == null) {
            gems$wealthOffersBackup = offers.copy();
        }
        for (MerchantOffer offer : offers) {
            int current = offer.getCostA().getCount();
            if (current > 1) {
                offer.addToSpecialPriceDiff(1 - current);
            }
            Optional<ItemCost> second = offer.getItemCostB();
            if (second.isPresent() && second.get().count() > 1) {
                ItemCost original = second.get();
                ItemCost adjusted = new ItemCost(original.item(), 1, original.components());
                ((TradeOfferAccessor) offer).gems$setSecondBuyItem(Optional.of(adjusted));
            }
            TradeOfferAccessor accessor = (TradeOfferAccessor) offer;
            if (accessor.gems$getMaxUses() < GEMS_WEALTH_MAX_USES) {
                accessor.gems$setMaxUses(GEMS_WEALTH_MAX_USES);
                if (accessor.gems$getUses() < 0) {
                    accessor.gems$setUses(0);
                }
            }
        }
    }

    @Inject(method = "setTradingPlayer", at = @At("TAIL"))
    private void gems$restoreWealthOffers(Player player, CallbackInfo ci) {
        if (player != null || gems$wealthOffersBackup == null) {
            return;
        }
        Villager villager = (Villager) (Object) this;
        villager.setOffers(gems$wealthOffersBackup);
        gems$wealthOffersBackup = null;
    }
}
