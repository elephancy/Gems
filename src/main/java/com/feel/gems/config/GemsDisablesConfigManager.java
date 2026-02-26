package com.feel.gems.config;

import com.feel.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GemsDisablesConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String FILE = "disables.json";

    private GemsDisablesConfigManager() {
    }

    public enum LoadStatus {
        LOADED,
        CREATED,
        ERROR
    }

    public record LoadResult(LoadStatus status, Path path, GemsDisablesConfig config, String error) {
    }

    static LoadResult loadOrCreateWithFallback() {
        return loadInternal(true);
    }

    public static LoadResult loadOrCreateStrict() {
        return loadInternal(false);
    }

    public static LoadResult loadOrCreateForUi() {
        return loadInternal(true);
    }

    private static LoadResult loadInternal(boolean fallbackToDefaults) {
        Path path = disablesPath();

        if (!Files.exists(path)) {
            GemsDisablesConfig cfg = new GemsDisablesConfig();
            // An empty object means every content category uses its default
            // empty list. Keep newly generated files focused on overrides.
            if (!GemsConfigManager.write(path, new JsonObject())) {
                return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? cfg : null,
                        "Could not create config " + path + ".");
            }
            return new LoadResult(LoadStatus.CREATED, path, cfg, null);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement existing = JsonParser.parseReader(reader);
            if (!existing.isJsonObject()) {
                throw new JsonSyntaxException("Disables config root must be an object");
            }
            GemsDisablesConfig cfg = GSON.fromJson(existing, GemsDisablesConfig.class);
            return new LoadResult(LoadStatus.LOADED, path, cfg, null);
        } catch (JsonSyntaxException e) {
            String error = "Failed to parse config " + path + " (keeping file, not overwriting).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsDisablesConfig() : null, error);
        } catch (IOException e) {
            String error = "Failed to read config " + path + " (keeping file).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsDisablesConfig() : null, error);
        }
    }

    static Path disablesPath() {
        return GemsConfigManager.configDir().resolve(FILE);
    }

    public static Path disablesPathForUi() {
        return disablesPath();
    }

    public static GemsDisablesConfig copyForUi(GemsDisablesConfig cfg) {
        return GSON.fromJson(GSON.toJsonTree(cfg), GemsDisablesConfig.class);
    }

    /** Apply this screen's edits while retaining newer keys and external changes. */
    public static boolean writeDisablesForUi(GemsDisablesConfig current, GemsDisablesConfig loaded) {
        return writeDisables(disablesPath(), current, loaded);
    }

    static boolean writeDisables(Path path, GemsDisablesConfig current, GemsDisablesConfig loaded) {
        try {
            JsonObject disk;
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (!parsed.isJsonObject()) {
                        GemsMod.LOGGER.error("Disables config root is not an object: {}", path);
                        return false;
                    }
                    disk = parsed.getAsJsonObject();
                }
            } else {
                disk = new JsonObject();
            }
            JsonObject defaults = GSON.toJsonTree(new GemsDisablesConfig()).getAsJsonObject();
            GemsConfigManager.applyEditedFields(disk, GSON.toJsonTree(loaded).getAsJsonObject(),
                    GSON.toJsonTree(current).getAsJsonObject(), defaults);
            GemsConfigManager.stripDefaultFields(disk, defaults);
            return GemsConfigManager.writeJson(path, disk);
        } catch (IOException | RuntimeException e) {
            GemsMod.LOGGER.error("Failed to save disables config {} (keeping file)", path, e);
            return false;
        }
    }
}
