package com.feel.gems.config;

import com.feel.gems.GemsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import net.fabricmc.loader.api.FabricLoader;




public final class GemsConfigManager {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static final String DIR = "gems";
    private static final String FILE = "balance.json";

    private GemsConfigManager() {
    }

    public enum LoadStatus {
        LOADED,
        CREATED,
        ERROR
    }

    public record LoadResult(LoadStatus status, Path path, GemsBalanceConfig config, String error) {
    }

    static LoadResult loadOrCreateWithFallback() {
        return loadInternal(true);
    }

    static LoadResult loadOrCreateStrict() {
        return loadInternal(false);
    }

    public static LoadResult loadOrCreateForUi() {
        return loadInternal(true);
    }

    private static LoadResult loadInternal(boolean fallbackToDefaults) {
        Path path = balancePath();

        if (!Files.exists(path)) {
            GemsBalanceConfig cfg = new GemsBalanceConfig();
            if (!writeJson(path, new JsonObject())) {
                return new LoadResult(LoadStatus.ERROR, path,
                        fallbackToDefaults ? cfg : null,
                        "Could not create config " + path + ".");
            }
            return new LoadResult(LoadStatus.CREATED, path, cfg, null);
        }

        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement existing = JsonParser.parseReader(reader);
            if (!existing.isJsonObject()) {
                throw new JsonSyntaxException("Balance config root must be an object");
            }
            JsonElement merged = mergeWithDefaults(existing, GSON.toJsonTree(new GemsBalanceConfig()));
            GemsBalanceConfig cfg = GSON.fromJson(merged, GemsBalanceConfig.class);
            return new LoadResult(LoadStatus.LOADED, path, cfg, null);
        } catch (JsonSyntaxException e) {
            String error = "Failed to parse config " + path + " (keeping file, not overwriting).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsBalanceConfig() : null, error);
        } catch (IOException e) {
            String error = "Failed to read config " + path + " (keeping file).";
            GemsMod.LOGGER.error(error, e);
            return new LoadResult(LoadStatus.ERROR, path, fallbackToDefaults ? new GemsBalanceConfig() : null, error);
        }
    }

    private static JsonElement mergeWithDefaults(JsonElement existing, JsonElement defaults) {
        if (existing == null || existing.isJsonNull()) {
            return defaults;
        }
        if (defaults == null || defaults.isJsonNull()) {
            return existing;
        }
        if (existing.isJsonObject() && defaults.isJsonObject()) {
            JsonObject merged = new JsonObject();
            JsonObject existingObj = existing.getAsJsonObject();
            JsonObject defaultObj = defaults.getAsJsonObject();
            for (var entry : existingObj.entrySet()) {
                merged.add(entry.getKey(), entry.getValue());
            }
            for (var entry : defaultObj.entrySet()) {
                String key = entry.getKey();
                JsonElement existingValue = merged.get(key);
                if (existingValue == null || existingValue instanceof JsonNull) {
                    merged.add(key, entry.getValue());
                } else {
                    merged.add(key, mergeWithDefaults(existingValue, entry.getValue()));
                }
            }
            return merged;
        }
        return existing;
    }

    static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR);
    }

    static Path balancePath() {
        return configDir().resolve(FILE);
    }

    public static Path balancePathForUi() {
        return balancePath();
    }

    static Path resolveInConfigDir(String fileName) {
        return configDir().resolve(fileName);
    }

    public static boolean write(Path path, Object cfg) {
        return writeJson(path, GSON.toJsonTree(cfg));
    }

    static boolean writeJson(Path path, JsonElement element) {
        Path temporary = null;
        try {
            Files.createDirectories(path.getParent());
            temporary = Files.createTempFile(path.getParent(), "config-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(element, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException | RuntimeException e) {
            GemsMod.LOGGER.error("Failed to write config {}", path, e);
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException e) {
                    GemsMod.LOGGER.warn("Could not remove temporary config {}", temporary, e);
                }
            }
        }
    }

    public static GemsBalanceConfig copyForUi(GemsBalanceConfig cfg) {
        return GSON.fromJson(GSON.toJsonTree(cfg), GemsBalanceConfig.class);
    }

    /** Apply only edits made in this screen, then omit default values from disk. */
    public static boolean writeBalanceForUi(GemsBalanceConfig current, GemsBalanceConfig loaded) {
        Path path = balancePath();
        try {
            JsonObject disk;
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (!parsed.isJsonObject()) {
                        GemsMod.LOGGER.error("Balance config root is not an object: {}", path);
                        return false;
                    }
                    disk = parsed.getAsJsonObject();
                }
            } else {
                disk = new JsonObject();
            }
            JsonObject defaults = GSON.toJsonTree(new GemsBalanceConfig()).getAsJsonObject();
            applyEditedFields(disk, GSON.toJsonTree(loaded).getAsJsonObject(),
                    GSON.toJsonTree(current).getAsJsonObject(), defaults);
            stripDefaultFields(disk, defaults);
            return writeJson(path, disk);
        } catch (IOException | RuntimeException e) {
            GemsMod.LOGGER.error("Failed to save balance config {} (keeping file)", path, e);
            return false;
        }
    }

    static void applyEditedFields(JsonObject disk, JsonObject before, JsonObject after,
                                  JsonObject defaults) {
        for (var entry : after.entrySet()) {
            String key = entry.getKey();
            JsonElement next = entry.getValue();
            JsonElement previous = before.get(key);
            if (next.equals(previous)) continue;
            JsonElement defaultValue = defaults.get(key);
            if (next.isJsonObject() && previous != null && previous.isJsonObject()
                    && defaultValue != null && defaultValue.isJsonObject()) {
                JsonElement onDisk = disk.get(key);
                JsonObject child = onDisk != null && onDisk.isJsonObject()
                        ? onDisk.getAsJsonObject() : new JsonObject();
                applyEditedFields(child, previous.getAsJsonObject(), next.getAsJsonObject(),
                        defaultValue.getAsJsonObject());
                disk.add(key, child);
            } else if (defaultValue != null && next.equals(defaultValue)) {
                disk.remove(key);
            } else {
                disk.add(key, next.deepCopy());
            }
        }
    }

    /** Remove known default values while retaining unknown keys from newer versions. */
    public static void stripDefaultFields(JsonObject disk, JsonObject defaults) {
        for (var entry : new ArrayList<>(disk.entrySet())) {
            String key = entry.getKey();
            JsonElement defaultValue = defaults.get(key);
            if (defaultValue == null) continue; // Preserve keys from newer versions or other tooling.
            JsonElement current = entry.getValue();
            if (current.isJsonObject() && defaultValue.isJsonObject()) {
                stripDefaultFields(current.getAsJsonObject(), defaultValue.getAsJsonObject());
                if (current.getAsJsonObject().isEmpty()) disk.remove(key);
            } else if (current.isJsonNull() || current.equals(defaultValue)) {
                disk.remove(key);
            }
        }
    }
}
