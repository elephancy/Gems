# Recipes

All recipes unlock automatically when a player joins. In a crafting table:

## Core progression items

- Heart (`gems:heart`): four Netherite Scraps in the corners, four Iron Blocks on the sides, and a Gold Block in the center.
- Energy Upgrade (`gems:energy_upgrade`): Netherite Scraps in the corners, Emerald Blocks on the sides, and a Diamond Block in the center.
- Gem Trader (`gems:gem_trader`): Diamond Blocks in the corners, Dragon's Breath on the sides, and a Wither Skeleton Skull in the center. It consumes one Trader and replaces the active gem; the current active gem item must be in carried inventory.
- Gem Purchase Token (`gems:gem_purchase`): End Crystals in the corners, Beacons at the top and bottom, Diamond Blocks at the left and right, and a Netherite Block in the center. It consumes one token, adds the selected gem to the owned set, and makes it active.

## Legendary items

- Legendary recipes live in `src/main/resources/data/gems/recipe/*.json`.
- Discount recipes (`*_discount`) are cheaper and only craftable while the matching gem is active.
  - Gem gating is configured by `legendary.recipeGemRequirements` in `config/gems/balance.json`.

## Gem augments and inscriptions

- Gem augment and legendary inscription recipes live in `src/main/resources/data/gems/recipe/*.json`.
- All augment/inscription recipes require a player head and unique ingredients (see `docs/GEMS.md` and `docs/LEGENDARY_ITEMS.md`).

