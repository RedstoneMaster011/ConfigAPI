package dev.redstone.configapi.config;

import com.google.gson.*;
import dev.redstone.configapi.api.ConfigOption;
import dev.redstone.configapi.api.ConfigRegistry;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public final class ConfigState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Map<String, String>> STATE = new HashMap<>();

    private ConfigState() {}

    public static void load(String namespace) {
        Path file = configFile(namespace);
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                Map<String, String> map = new HashMap<>();
                if (json != null) {
                    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                        if (entry.getValue().isJsonPrimitive()) {
                            map.put(entry.getKey(), entry.getValue().getAsString());
                        }
                    }
                }
                STATE.put(namespace, map);
            } catch (IOException e) {
                System.err.println("[ConfigAPI] Failed to load config for '" + namespace + "': " + e.getMessage());
                seedDefaults(namespace);
            }
        } else {
            seedDefaults(namespace);
        }
    }

    public static void save(String namespace) {
        Path file = configFile(namespace);
        try {
            Files.createDirectories(file.getParent());
            JsonObject json = new JsonObject();
            Map<String, String> map = STATE.getOrDefault(namespace, Map.of());
            for (ConfigOption option : ConfigRegistry.getOptions(namespace)) {
                json.addProperty(option.id(), map.getOrDefault(option.id(), option.defaultValueString()));
            }
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            System.err.println("[ConfigAPI] Failed to save config for '" + namespace + "': " + e.getMessage());
        }
    }

    public static boolean getBoolean(String namespace, String optionId) {
        return Boolean.parseBoolean(getRaw(namespace, optionId));
    }

    public static String getString(String namespace, String optionId) {
        return getRaw(namespace, optionId);
    }

    public static int getInt(String namespace, String optionId) {
        try { return Integer.parseInt(getRaw(namespace, optionId)); }
        catch (NumberFormatException e) { return 0; }
    }

    public static float getFloat(String namespace, String optionId) {
        try { return Float.parseFloat(getRaw(namespace, optionId)); }
        catch (NumberFormatException e) { return 0f; }
    }

    @Deprecated
    public static boolean isEnabled(String namespace, String optionId) {
        return getBoolean(namespace, optionId);
    }

    @Deprecated
    public static void setEnabled(String namespace, String optionId, boolean enabled) {
        setValue(namespace, optionId, String.valueOf(enabled));
    }

    public static void setValue(String namespace, String optionId, String value) {
        STATE.computeIfAbsent(namespace, k -> new HashMap<>()).put(optionId, value);
    }

    public static String getRaw(String namespace, String optionId) {
        return STATE
                .getOrDefault(namespace, Map.of())
                .getOrDefault(optionId, defaultFor(namespace, optionId));
    }

    public static void resetToDefaults(String namespace) {
        seedDefaults(namespace);
        save(namespace);
    }

    public static Set<String> enabledIds(String namespace) {
        Set<String> result = new HashSet<>();
        for (ConfigOption option : ConfigRegistry.getOptions(namespace)) {
            if (option.inputType() == ConfigOption.InputType.TOGGLE && getBoolean(namespace, option.id())) {
                result.add(option.id());
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static void seedDefaults(String namespace) {
        Map<String, String> map = new HashMap<>();
        for (ConfigOption option : ConfigRegistry.getOptions(namespace)) {
            map.put(option.id(), option.defaultValueString());
        }
        STATE.put(namespace, map);
    }

    private static String defaultFor(String namespace, String optionId) {
        return ConfigRegistry.find(namespace, optionId)
                .map(ConfigOption::defaultValueString)
                .orElse("");
    }

    private static Path configFile(String namespace) {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("configapi")
                .resolve(namespace + ".json");
    }
}