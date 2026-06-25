# Assassin endgame

- If your maximum health is down to 5 hearts (by default), dying again turns you into an Assassin.
- Assassins are highlighted red in the player list.
- Assassins have a configurable max hearts cap (`systems.assassinMaxHearts`), cannot consume heart items, and never drop heart items.
- If killed by another Assassin: configurable heart loss/gain (`systems.assassinVsAssassinVictimHeartsLoss`/`systems.assassinVsAssassinKillerHeartsGain`).
- If killed by another Assassin: the killer also takes all of your accumulated Assassin points.
- Reaching the elimination threshold (`systems.assassinEliminationHeartsThreshold`) permanently eliminates the Assassin.
- Scoring (after becoming an Assassin): normal kill = +1, final kill = +3.
- Reaching 10 Assassin points unlocks a permanent choice: you can return to normal at any time. Use `/gems assassin leave` after unlocking (leaving resets assassin points).
- The highest-score Assassin is matched against the last non-Assassin survivor (admin-run duel).

## UI

- Assassin points are shown in the player list (TAB).
