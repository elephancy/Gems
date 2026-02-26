package com.feel.gems.core;

import com.feel.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;




/**
 * Lightweight in-memory registry for gem definitions. Meant to be bridged to the loader's registry system.
 */
public final class GemRegistry {
    private static EnumMap<GemId, GemDefinition> DEFINITIONS = new EnumMap<>(GemId.class);
    private static final Gson GSON = new Gson();
    private static final String DEFINITIONS_CLASSPATH = "data/gems/gem_definitions.json";
    private static final Identifier DEFINITIONS_ID = Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gem_definitions.json");

    private static boolean initialized = false;

    private GemRegistry() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        // Bootstrap from the built-in jar resource so client-side callers always have something.
        try {
            DEFINITIONS = loadFromClasspath();
        } catch (RuntimeException e) {
            GemsMod.LOGGER.error("Failed to bootstrap gem definitions from classpath", e);
        }

        // Dedicated/integrated servers: reload from datapacks on /reload and startup.
        // Unit tests may run without Fabric Loader being fully bootstrapped; skip if unavailable.
        try {
            ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
                    Identifier.fromNamespaceAndPath(GemsMod.MOD_ID, "gem_registry"),
                    (ResourceManagerReloadListener) GemRegistry::reloadFromResourceManager
            );
        } catch (Throwable t) {
            GemsMod.LOGGER.debug("Skipping gem registry reload listener registration (likely unit-test environment)", t);
        }
    }

    public static GemDefinition definition(GemId id) {
        GemDefinition def = DEFINITIONS.get(id);
        if (def == null) {
            throw new IllegalArgumentException("Unknown gem id: " + id);
        }
        return def;
    }

    private static void reloadFromResourceManager(ResourceManager manager) {
        try {
            var opt = manager.getResource(DEFINITIONS_ID);
            if (opt.isEmpty()) {
                GemsMod.LOGGER.warn("No {} found in datapacks; keeping previous gem definitions", DEFINITIONS_ID);
                return;
            }
            var resource = opt.get();
            try (Reader reader = resource.openAsReader()) {
                EnumMap<GemId, GemDefinition> next = loadFromReader(reader);
                if (!next.isEmpty()) {
                    DEFINITIONS = next;
                    GemsMod.LOGGER.info("Reloaded gem definitions from datapacks ({})", DEFINITIONS_ID);
                }
            }
        } catch (Exception e) {
            GemsMod.LOGGER.error("Failed to reload gem definitions from datapacks; keeping previous definitions", e);
        }
    }

    private static EnumMap<GemId, GemDefinition> loadFromClasspath() {
        try (var stream = GemRegistry.class.getClassLoader().getResourceAsStream(DEFINITIONS_CLASSPATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing gem definitions at " + DEFINITIONS_CLASSPATH);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return loadFromReader(reader);
            }
        } catch (Exception e) {
            if (e instanceof JsonParseException || e instanceof IllegalStateException) {
                GemsMod.LOGGER.error("Failed to load gem definitions", e);
            } else {
                GemsMod.LOGGER.error("Unexpected error loading gem definitions", e);
            }
            throw new IllegalStateException("Failed to load gem definitions from " + DEFINITIONS_CLASSPATH, e);
        }
    }

    private static EnumMap<GemId, GemDefinition> loadFromReader(Reader reader) {
        GemDefinitionFile file = GSON.fromJson(reader, GemDefinitionFile.class);
        if (file == null || file.gems == null || file.gems.isEmpty()) {
            throw new IllegalStateException("No gem definitions found in " + DEFINITIONS_ID);
        }
        EnumMap<GemId, GemDefinition> out = new EnumMap<>(GemId.class);
        for (GemDefinitionEntry entry : file.gems) {
            GemDefinition def = entry.toDefinition();
            out.put(def.id(), def);
        }
        // Ensure we don't partially wipe the registry on malformed packs.
        for (GemId id : GemId.values()) {
            if (!out.containsKey(id)) {
                throw new IllegalStateException("Missing gem definition for " + id.name().toLowerCase());
            }
        }
        return out;
    }

    private static final class GemDefinitionFile {
        private List<GemDefinitionEntry> gems = List.of();
    }

    private static final class GemDefinitionEntry {
        private String id;
        private List<String> passives = List.of();
        private List<String> abilities = List.of();

        private GemDefinition toDefinition() {
            if (id == null || id.isBlank()) {
                throw new IllegalStateException("Gem definition missing id");
            }
            GemId gemId = GemId.valueOf(id.trim().toUpperCase());
            List<Identifier> passiveIds = toIdentifiers(passives, "passives", gemId);
            List<Identifier> abilityIds = toIdentifiers(abilities, "abilities", gemId);
            return new GemDefinition(gemId, passiveIds, abilityIds);
        }
    }

    private static List<Identifier> toIdentifiers(List<String> raw, String label, GemId gemId) {
        if (raw == null) {
            throw new IllegalStateException("Gem " + gemId + " missing " + label + " list");
        }
        List<Identifier> ids = new java.util.ArrayList<>(raw.size());
        for (String value : raw) {
            Identifier id = Identifier.tryParse(value);
            if (id == null) {
                throw new IllegalStateException("Invalid " + label + " id '" + value + "' for gem " + gemId);
            }
            ids.add(id);
        }
        return List.copyOf(ids);
    }
}
