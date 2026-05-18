# Gems (Abilities and Passives)

Passives and abilities are gated by energy. For progression rules, see `docs/PROGRESSION.md`.

## Gem augments

- Craftable items that add modifiers to gem abilities/passives.
- Augments require a player head and unique ingredients.
- Gems have up to 4 augment slots (default), with 3 rarity tiers (default).
- Augments are stored on the gem item itself, so two copies of the same gem can have different augments.
- Rarity affects strength/roll ranges; conflicts prevent incompatible combos.
- Slot counts, rarity tiers, roll weights, and magnitudes are configurable.
- Apply a gem augment by right-clicking while the augment is in your hand and the gem is in your other hand.
- Use the augment screen (default U) while holding a gem to view or remove augments on that specific item.
- Each augment rolls a rarity on craft; the rolled rarity scales its strength.
- Augment effects:
  - Focus: reduces gem ability cooldowns.
  - Persistence: increases gem ability durations.
  - Resonance: increases passive amplifier bonuses.

## Gems

### Air Gem

Passives:
- Windburst Mace (Breach IV, Wind Burst III, Mending, Unbreaking III, Fire Aspect II).
- Aerial Guard: reduced damage and knockback while holding the mace.
- Wind Shear: mace strikes add extra knockback and a short slow.

Abilities:
- Wind Jump: wind-charge style high jump.
- Gale Slam: empower your next mace slam with a stronger wind burst.
- Crosswind: cutting gust forward that knocks back and slows enemies.
- Air Dash: forward dash with brief i-frames.

### Astra Gem

Passives:
- Soul Capture: store the most recently killed mob for later release.
- Soul Healing: heal on capture or release.

Abilities:
- Shadow Anchor: set an anchor and return to it within a short window.
- Dimensional Void: suppress enemy gem abilities and passives in a radius (players only).
- Astral Daggers: fire a ranged dagger volley.
- Unbounded: brief Spectator mode, then return to Survival.
- Astral Camera: scout in Spectator, then return to your start.
- Spook: disorient nearby enemies.
- Tag: mark a target through walls briefly.
- Soul Release: summon the captured mob as a soul summon (no loot/XP), preserving its name/gear/attributes and restoring it to full health; follows the summon AI priority.

### Beacon Gem

Passives:
- Beacon Core: regen pulses for trusted allies; slows untrusted players.
- Stabilize: reduces negative effects on trusted allies; hinders enemies with fatigue.
- Rally: casting a beacon aura grants trusted allies brief Absorption.

Abilities:
- Moving auras (toggle): Speed, Haste, Resistance, Jump Boost, Strength, Regeneration (only one active).
- Aura pulses buff trusted allies and apply Slowness/Weakness to untrusted players in range.

### Chaos Gem

Passives:
- Chaos Agent: grants six independent random ability slots by default (server owners can set 1-9).

Abilities:
- Each Chaos slot (Left Alt + its hotbar number): each slot can be rolled independently. Press a slot key when empty to roll a random ability and passive from any other gem. The rolled ability lasts 5 minutes and can be used with a 10-second cooldown. All n slots operate independently.
- At 1 energy, you can roll a slot for its passive. Using the rolled ability starts at 2 energy, like other gem abilities.

### Duelist Gem

Passives:
- Riposte: after a successful block, your next attack within 2s deals 50% bonus damage.
- Duelist's Focus: deal 25% more damage in 1v1 combat (no other players within 15 blocks).
- Combat Stance: while holding a sword, gain +10% movement speed.

Abilities:
- Lunge: dash forward with your sword, dealing damage to the first enemy hit.
- Parry: brief window to deflect incoming melee attacks; successful parry stuns the attacker.
- Rapid Strike: remove sword cooldowns for 5s.
- Flourish: quick 360 sword sweep that hits all nearby enemies.
- Mirror Match: force a target into a 1v1 duel for 15s (barrier prevents escape); also copies your skin and name onto them.
- Blade Dance: combo system where consecutive hits deal increasing damage (resets after 3s without hitting).

### Fire Gem

Passives:
- Fire Resistance.
- Auto-smelt eligible block drops.
- Auto-enchant Fire Aspect on melee weapons.

Abilities:
- Cosy Campfire: regen aura for allies.
- Heat Haze Zone: allies gain Fire Resistance; enemies get Mining Fatigue + Weakness.
- Fireball: charge and launch an explosive fireball (charge decays unless on obsidian).
- Meteor Shower: multiple meteors strike along a line in front of you.

### Flux Gem

Passives:
- Charge Storage: consume valuables to charge up to 200%.
- Ally Inversion: Flux Beam repairs trusted allies' armor instead of damaging them.
- Overcharge Ramp: above 100% charge, ramps to 200% while dealing self-damage.
- Flux Capacitor: at high charge, gain Absorption.
- Flux Conductivity: taking damage converts some of it into charge.
- Flux Insulation: at high charge, incoming damage is reduced.

Abilities:
- Flux Beam: long-range beam; damage and armor shred scale with charge.
- Static Burst: AOE burst from recent damage taken.
- Flux Surge: spend charge for a speed/resistance burst and a close-range shockwave.
- Flux Discharge: dump charge into a damaging knockback shockwave.

### Hunter Gem

Passives:
- Prey Mark: hitting an enemy marks them; marked targets take 15% more damage from you.
- Tracker's Eye: marked enemies are visible through walls within 30 blocks.
- Trophy Hunter: on player kill, gain one of their random passives temporarily (60s); persists through death.

Abilities:
- Hunting Trap: place an invisible trap that roots and damages the first enemy who walks over it.
- Pounce: leap toward a marked target from up to 20 blocks away.
- Net Shot: fire a net that slows and grounds enemies (disables flight/elytra).
- Crippling Shot: ranged attack that reduces target's movement speed by 50% for 8s.
- Pack Tactics: nearby trusted allies deal 20% more damage to your marked target for 10s.
- Origin Tracking: track the first owner of an item (who originally crafted or found it); works on offline targets if the server has last-known data for them.
- Six-Pack Pain: summon 6 player clones that share a health pool (120 HP total) for 10s. They attack untrusted players and hostile mobs. When hit, you get regeneration + a random buff, and the attacker gets debuffed.

### Life Gem

Passives:
- Auto-enchant Unbreaking on eligible gear.
- Double Saturation from food.

Abilities:
- Vitality Vortex: adaptive pulse based on surroundings; affects nearby living entities (not you) and buffs trusted allies while debuffing enemies.
  - Aquatic (water nearby or submerged): allies heal + Regen I + Water Breathing + Dolphin's Grace; enemies Slowness II + Mining Fatigue I.
  - Infernal (Nether or lava nearby): allies heal + Regen I + Fire Resistance + Strength; enemies Wither I + Weakness I.
  - Sculk (sculk blocks nearby): allies heal + Regen I + Night Vision + Resistance; enemies Darkness + Mining Fatigue II.
  - Verdant (leaves/flowers/grass/vines nearby): allies heal + Regen II + Absorption + brief Saturation; enemies Poison II + Slowness I.
  - End (in the End): allies heal + Regen I + Slow Falling + Resistance; enemies Slowness II + Weakness I.
  - Default: allies heal + Regen II + Absorption; enemies Poison I + Weakness I.
- Health Drain: siphon health from a target to heal yourself; damage respects armor and enchantments.
- Life Swap: swap health with a target; requires at least 3 hearts to cast (configurable). If both players stay alive, the original health values are restored after 15 seconds (configurable).
- Life Circle: boost allies while reducing enemy max health (heals to the new max).
- Heart Lock: lock an enemy's max health to their current health briefly (default 4 seconds).

### Pillager Gem

Passives:
- Raider's Training: faster projectiles.
- Shieldbreaker: melee hits can disable shields without an axe (untrusted players).
- Illager Discipline: resistance burst at low health (cooldown).
- Crossbow Mastery: auto-applies Quick Charge II to crossbows.
- Raider's Stride: minor Speed while active.

Abilities:
- Fangs: evoker-fangs line at a target zone.
- Ravage: heavy knockback hit.
- Vindicator Break: melee buff that also disables shields.
- Volley: short arrow burst without ammo.
- Warhorn: buff nearby allies and slow nearby enemies.
- Snare Shot: mark a target with glowing and slowness.

### Prism Gem

Passives: None (selected at max energy).

Abilities: None (selected at max energy).

At energy 10/10, Prism players can select:
- Up to 3 abilities from normal gems.
- Up to 3 passives from normal gems.
- Bonus abilities/passives are claimed from the global bonus pool (same as other gems) and are activated via the bonus keybinds (default C/V).
- Blacklisted powers (Void Immunity, Chaos Random Rotation, Nullify) cannot be selected.

How to select:
- Press B to open the Prism selection screen (it includes both normal gem powers and bonus pool powers).

### Puff Gem

Passives:
- Fall damage immunity.
- Auto-enchant Power and Punch on bows.
- Sculk silence.
- Crop-trample immunity.
- Windborne: while airborne, gain Slow Falling.

Abilities:
- Double Jump: midair jump reset.
- Dash: fast dash that damages enemies you pass through.
- Breezy Bash: uppercut; impact damage if they land soon.
- Group Breezy Bash: knock away nearby untrusted players.
- Gust: shockwave that launches and slows nearby enemies.

### Reaper Gem

Passives:
- Rot Eater: no negative effects from rotten flesh/spider eyes.
- Undead Ward: reduced damage from undead mobs.
- Harvest: brief regen on mob kills.

Abilities:
- Grave Steed: summon a saddled skeleton horse that decays over time.
- Withering Strikes: melee hits apply Wither temporarily.
- Death Oath: choose a target; you take damage over time but deal bonus damage to them.
- Retribution: incoming damage reflects to the attacker while the effect lasts.
- Scythe Sweep: wide melee cleave in front of you.
- Blood Charge: sacrifice health to buff your next hit or ability.
- Shadow Clone: summon multiple invulnerable decoys that vanish after 10s.

### Sentinel Gem

Passives:
- Guardian Aura: nearby trusted allies take 15% less damage.
- Fortress: while standing still for 2s, gain Resistance II for 10s (persists briefly after you move).
- Retribution Thorns: attackers take 20% of damage dealt back as true damage.

Abilities:
- Shield Wall: deploy an energy barrier that blocks projectiles and slows enemies passing through.
- Taunt: force nearby enemies to target you for 5s; gain damage reduction during taunt.
- Intervention: instantly teleport to a mutually trusted ally and absorb the next hit they would take.
- Rally Cry: heal all nearby trusted allies and grant Resistance I for 8s.
- Lockdown: create a zone where enemies cannot use abilities for 10s.

### Space Gem

Passives:
- Lunar Scaling: damage and self-healing scale with moon phase.
- Low Gravity: minor slow falling and no fall damage while active.
- Starshield: reduced projectile damage outdoors at night.

Abilities:
- Orbital Laser (Damage): strike the block you're looking at with a damage beam.
- Orbital Laser (Mining): strike the block you're looking at with a mining beam.
- Gravity Field: lighten trusted allies and weigh down enemies.
- Black Hole: pull and damage entities in an area.
- White Hole: push and damage entities in an area.

### Speed Gem

Passives:
- Speed I and Haste I.
- Momentum: abilities scale with your speed at cast time.
- Frictionless Steps: reduced slowdown from cobweb, honey, and powder snow.

Abilities:
- Arc Shot: lightning arc that can chain multiple targets.
- Speed Storm: field that buffs allies and slows enemies.
- Terminal Velocity: short Speed/Haste burst.
- Slipstream: wind lane that speeds allies and disrupts enemies.
- Afterimage: brief invisibility + speed; breaks on hit.
- Tempo Shift: speeds ally cooldowns and slows enemy cooldowns nearby.

### Spy Gem

Passives:
- Stillness Cloak: stand still to become invisible with no particles.
- Silent Step: no sculk triggers.
- False Signature: harder to track with mark/track abilities.
- Backstab: attacking from behind deals bonus damage.
- Quick Hands: minor Haste.

Abilities:
- Mimic Form: after a recent mob kill, gain invisibility + bonus health/attack + speed scaled to that mob’s base stats (tough mobs grant larger buffs).
- Echo: replay an observed ability; consumes 1 observation.
- Steal: after observing 4 times, steal an ability; consumes 4 observations. The victim only recovers it if you swap gems or they kill you (recovery does not require them to re-select it).
- Smoke Bomb: blind/slow nearby enemies and briefly cloak you.
- Stolen Cast: cast a stolen ability; sneak to cycle.
- Skinshift: steal a targeted player's appearance and name (including chat display) for a short time; the target cannot chat while the effect is active.

Observed menu:
- Tracks observed abilities and counts; choose which observed ability Echo/Steal will use. Observations do not expire but are cleared on death.

### Strength Gem

Passives:
- Strength I.
- Auto-enchant Sharpness III.
- Adrenaline: gain brief Resistance when critically low.

Abilities:
- Nullify: remove active effects from enemies in a radius.
- Frailer: apply Weakness to a target.
- Bounty Hunting: consume an item to track its last owner temporarily (falls back to the previous owner if you're the last owner; works on offline targets if the server has last-known data for them).
- Chad Strength: every Nth hit deals bonus damage.

### Summoner Gem

Passives:
- Summoner's Bond: summons do not target you or trusted players.
- Commander's Mark: hits mark a target; summons prioritize and gain temporary Strength.
- Soulbound Minions: summons despawn on death or logout.
- Familiar's Blessing: summons spawn with bonus health.

Abilities:
- Summon 1-5: spawn the configured loadouts while staying under the active summon point cap. Summons drop no loot or XP.
- Recall: despawn all active summons.
- Summon slots share a global cooldown.

Summoner UI:
- Open the loadout editor with Gems Modifier + the hotbar key after the last ability slot (default Alt + 7) while using Summoner.

Summoned/controlled AI priority (Summoner, Astra Soul Release, Hypno Staff):
- Summoner only: Commander's Mark target overrides all other priorities.
- Otherwise: target the last entity you attacked (player or mob) within command range.
- If none: target whoever last attacked you or is currently hostile toward you.
- If none: follow you.
- Trust-based targeting protection only applies while passives are active (energy > 0 and not suppressed).

### Terror Gem

Passives:
- Dread Aura: nearby untrusted players get Darkness.
- Fearless: cleanse Blindness and Darkness on the holder.
- Blood Price: on player kill, gain a short Strength/Resistance burst.

Abilities:
- Rig: trap a block; any use, break, step, or block update triggers an explosion (five primed TNT, fuse configurable).
- Remote Charge: arm a block within 10s, then detonate from anywhere within 1 minute (has a cooldown).
- Panic Ring: spawn primed TNT around you.
- Breach Charge: immediate blast at the targeted block or entity with wither-skull style VFX.
- Terror Trade: sacrifice yourself to attempt to kill a target (totems can save). Costs 2 max hearts and 2 permanent energy, limited to 3 uses per player.

### Trickster Gem

Passives:
- Sleight of Hand: 20% chance to not consume items when using throwables.
- Chaos Agent: your abilities have randomized bonus effects (can be beneficial or detrimental).
- Slippery: 25% chance to ignore slowing effects.

Abilities:
- Shadow Swap: instantly swap places with the entity you are looking at (line-of-sight).
- Mirage: create a single mirage clone at your aim point; it mirrors your movements and breaks on hit or timeout, granting you a brief buff.
- Glitch Step: short-range teleport that leaves a damaging afterimage at your origin.
- Puppet Master: briefly control an enemy's movement for 3s (they walk where you aim).
- Mind Games: reverse an enemy's movement controls for 5s (left becomes right, forward becomes back).

### Void Gem

Passives:
- Void Immunity: immune to all gem abilities and passives from other players.

Abilities: None.

### Wealth Gem

Passives:
- Auto-enchant Mending, Fortune, and Looting.
- Luck and Hero of the Village V.
- Cured Prices: villager trades cost 1 item/emerald and refresh infinitely.
- Durability chip and armor mend on hit.
- Double Debris from furnaces.

Abilities:
- Pockets: extra inventory (rows configurable in balance).
- Fumble: disable offhand use and eating for enemies.
- Hotbar Lock: lock an enemy to their current hotbar slot.
- Amplification: temporarily boosts enchant effectiveness by +N levels (configurable; default +2). If Unbreaking, Protection, Mending, Looting, Sharpness, Efficiency, or Fortune are already maxed, they count as +N levels while Amplification is active.
- Rich Rush: rolls mob/ore loot multiple times (configurable) and can guarantee rare drops.
