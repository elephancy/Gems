# Balance config

Balancing is controlled by `config/gems/balance.json` (generated on first run).
The file stores only values changed from the defaults, so a fresh install starts
with `{}` rather than hundreds of unused controls. The game applies its built-in
defaults to every missing value in memory.

Mod Menu shows the main settings for each gem and system: usually two or three
controls per section. Detailed tuning remains available in `balance.json` for
server owners who need it. Use **Copy JSON paths** in Mod Menu to find the file.
An existing detailed override still loads and works, even if Mod Menu does not
show that field. **Reset section** resets every value in the selected section,
including detailed overrides; **Reset server** resets all balance and disable
settings. Review these changes before saving.

- Apply changes without restart: `/gems reloadBalance` (requires op permission level 2+).
- If the JSON is invalid, the reload fails and the current balance stays active (the file is not overwritten).
- Missing keys use built-in defaults without being written into the file.
- Saving from Mod Menu changes only the fields edited in that session. Existing detailed overrides and unrecognized keys are kept. Values set back to their defaults are removed from the file.
- The in-game disable list covers gems, abilities, and passives. The existing `disabledBonusAbilities` and `disabledBonusPassives` lists remain available in `config/gems/disables.json`.
- Export the currently-applied (sanitized) values: `/gems dumpBalance` (writes `config/gems/balance.effective.json`).

Player-facing behavior docs live under `docs/` (start at `docs/README.md`). This file focuses on configuration keys, units, and safety constraints.

## Units

- `*CooldownSeconds`, `*DurationSeconds`, `*WindowSeconds`: seconds (converted to ticks internally)
- `*RadiusBlocks`, `*RangeBlocks`, `*SpreadBlocks`, `*HeightBlocks`: blocks
- `*Damage`, `*Heal`: Minecraft health points (2.0 = 1 heart)
- `*Amplifier`: potion amplifier (0 = level I, 1 = level II, etc)
- `*ScalePercent`: percentage scaling (100 = no change)

## Sanity clamps

Values are clamped on load to prevent extreme configs from tanking performance or breaking gameplay. The file is not rewritten; the runtime values are sanitized.

Typical clamps:
- Cooldowns: `0..3600s` (most); some long systems allow longer windows.
- Durations: usually `0..120s` (some allow longer).
- Most AOE radii: `0..32 blocks`

## Visual budget knobs (server)

Global caps/switches for particles/sounds emitted by abilities (useful for large servers).

- `visual.enableParticles`: master switch for all server-sent particles.
- `visual.enableSounds`: master switch for ability sounds.
- `visual.particleScalePercent`: scales all particle counts (0..200).
- `visual.maxParticlesPerCall`: hard cap for a single particle spawn call (default 128).
- `visual.maxBeamSteps`: hard cap for beam particle steps (default 256).
- `visual.maxRingPoints`: hard cap for ring/aura particle points (default 128).

## Config layout (top-level keys)

The balance file mirrors the in-code schema in `src/main/java/com/feel/gems/config/GemsBalanceConfig.java`.

- `visual`: global particles/sounds caps.
- `systems`: hearts + assassin tuning, controlled mob follow behavior.
- Per-gem sections: `astra`, `fire`, `flux`, `life`, `puff`, `speed`, `strength`, `wealth`, `terror`, `summoner`, `space`, `reaper`, `pillager`, `spy`, `beacon`, `air`, `duelist`, `hunter`, `sentinel`, `trickster`.
- `legendary`: legendary crafting limits, tracker/recall/hypno/earthsplitter tuning, supreme set effects, and `recipeGemRequirements` for discount recipes.
- `bonusPool`: per-bonus ability/passive tuning (cooldowns, damage, ranges, effect multipliers, etc).
- `mobBlacklist`: universal mob blacklist used by Hypno Staff, Summoner summons, and Astra Soul Capture.

## Recipe gem requirements (discount recipes)

`legendary.recipeGemRequirements` is a map of `recipe_id -> required_active_gem_id` for discount recipes (e.g. `gems:tracker_compass_discount` -> `spy`).

## Client config note

Client-side settings are stored in `config/gems/client.json` (per-client, not synced to server). If this file cannot be read, the settings screen leaves it intact and shows an error; use **Reload from disk** after repairing it. Saves preserve unfamiliar keys. For keybind modes and HUD controls, see `docs/CONTROLS_AND_HUD.md`.

