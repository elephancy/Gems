package com.feel.gems.client;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsConfigManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;


public final class GemsClientConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIR = "gems";
    private static final String FILE = "client.json";
    private static GemsClientConfig cached;
    private static String loadError;

    private GemsClientConfigManager() {
    }

    public static GemsClientConfig loadOrCreate() {
        if (cached != null) {
            return cached;
        }
        Path path = configPath();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) {
                    throw new IllegalArgumentException("Client config root must be an object");
                }
                GemsClientConfig cfg = GSON.fromJson(parsed, GemsClientConfig.class);
                if (cfg.controlMode == null) cfg.controlMode = GemsClientConfig.ControlMode.CHORD;
                cached = cfg;
                loadError = null;
                return cfg;
            } catch (Exception e) {
                GemsMod.LOGGER.warn("Failed to read client config: {}", path, e);
                loadError = "Could not read client.json; the file was kept.";
                cached = new GemsClientConfig();
                return cached;
            }
        }
        GemsClientConfig cfg = new GemsClientConfig();
        // Both personal controls have safe in-memory defaults. A new file
        // needs only overrides, just like balance.json.
        if (!GemsConfigManager.write(path, new JsonObject())) {
            loadError = "Could not create client.json; check the log.";
        } else {
            loadError = null;
        }
        cached = cfg;
        return cfg;
    }

    public static GemsClientConfig reloadFromDisk() {
        cached = null;
        loadError = null;
        return loadOrCreate();
    }

    public static String loadError() {
        return loadError;
    }

    public static GemsClientConfig config() {
        return loadOrCreate();
    }

    public static GemsClientConfig copyForUi(GemsClientConfig cfg) {
        return GSON.fromJson(GSON.toJsonTree(cfg), GemsClientConfig.class);
    }

    public static boolean save(GemsClientConfig cfg) {
        if (loadError != null) return false;
        Path path = configPath();
        try {
            JsonObject disk = new JsonObject();
            if (Files.exists(path)) {
                try (var reader = Files.newBufferedReader(path)) {
                    JsonElement parsed = JsonParser.parseReader(reader);
                    if (!parsed.isJsonObject()) {
                        GemsMod.LOGGER.warn("Client config root is not an object: {}", path);
                        return false;
                    }
                    disk = parsed.getAsJsonObject();
                }
            }
            for (var entry : GSON.toJsonTree(cfg).getAsJsonObject().entrySet()) {
                disk.add(entry.getKey(), entry.getValue());
            }
            GemsConfigManager.stripDefaultFields(disk,
                    GSON.toJsonTree(new GemsClientConfig()).getAsJsonObject());
            if (!GemsConfigManager.write(path, disk)) return false;
            cached = cfg;
            return true;
        } catch (IOException | RuntimeException e) {
            GemsMod.LOGGER.warn("Failed to save client config: {}", path, e);
            return false;
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(DIR).resolve(FILE);
    }

}
