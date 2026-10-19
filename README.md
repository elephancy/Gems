# Gems of Fate

A standalone Minecraft 26.3 Fabric mod with collectible gems, powers, progression, and legendary items. It also powers the separate [Second Chance](https://github.com/elephancy/Second-Chance) adventure.

Install the 2.0 Gems jar with Fabric Loader 0.19.5+, Fabric API 0.162.0+26.3, and Java 25. Start a fresh world. Mod Menu is optional.

Build with `./gradlew build` (`gradlew.bat build` on Windows). The jar is in `build-win/libs/` on Windows or `build-wsl/libs/` on Linux. Player rules are in [gameplay.md](gameplay.md); asset sources and licenses are in [credits.md](credits.md).

CI builds the mod, compiles the GameTest source set, and reports the resolved
runtime dependency graph on Java 25. Dedicated server and client GameTest smoke
runs remain explicit release checks. Gradle uses patched LZ4 and a consistent
Netty 4.2 patch family and Log4j, while retaining newer upstream dependencies.
A mod jar does not replace libraries supplied by a player's
Minecraft launcher; keep the game installation updated separately.
