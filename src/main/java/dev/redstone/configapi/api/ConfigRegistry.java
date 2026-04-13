package dev.redstone.configapi.api;

import java.util.*;

public final class ConfigRegistry {

    private static final Map<String, List<ConfigOption>> OPTIONS = new LinkedHashMap<>();

    private ConfigRegistry() {}

    public static void register(String namespace, ConfigOption option) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(option, "option");

        List<ConfigOption> list = OPTIONS.computeIfAbsent(namespace, k -> new ArrayList<>());
        boolean duplicate = list.stream().anyMatch(o -> o.id().equals(option.id()));
        if (duplicate) {
            throw new IllegalArgumentException(
                "ConfigOption id '" + option.id() + "' is already registered under namespace '" + namespace + "'");
        }
        list.add(option);
    }

    public static void registerAll(String namespace, ConfigOption... options) {
        for (ConfigOption option : options) {
            register(namespace, option);
        }
    }

    public static List<ConfigOption> getOptions(String namespace) {
        return Collections.unmodifiableList(OPTIONS.getOrDefault(namespace, List.of()));
    }

    public static List<ConfigOption> getAllOptions() {
        return OPTIONS.values().stream()
            .flatMap(Collection::stream)
            .toList();
    }

    public static Set<String> getNamespaces() {
        return Collections.unmodifiableSet(OPTIONS.keySet());
    }

    public static Optional<ConfigOption> find(String namespace, String id) {
        return getOptions(namespace).stream().filter(o -> o.id().equals(id)).findFirst();
    }
}
