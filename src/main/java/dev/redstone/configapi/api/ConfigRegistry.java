package dev.redstone.configapi.api;

import java.util.*;

/**
 * Central registry for all {@link ConfigOption}s across all mods.
 *
 * <p>Register your options during mod initialisation (or client init):
 * <pre>{@code
 * ConfigRegistry.register("my_mod", new ConfigOption.Builder("my_feature", "My Feature")
 *     .description("Does something cool.")
 *     .defaultEnabled(true)
 *     .build());
 * }</pre>
 *
 * The config screen will pick these up automatically via ModMenu.
 */
public final class ConfigRegistry {

    // namespace -> ordered list of options
    private static final Map<String, List<ConfigOption>> OPTIONS = new LinkedHashMap<>();

    private ConfigRegistry() {}

    // ── Registration ───────────────────────────────────────────────────────

    /**
     * Register a single {@link ConfigOption} under the given mod namespace.
     *
     * @param namespace  Your mod id (e.g. "my_mod").
     * @param option     The option to register.
     * @throws IllegalArgumentException if an option with the same id is already registered in this namespace.
     */
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

    /**
     * Register multiple options at once.
     *
     * @param namespace  Your mod id.
     * @param options    The options to register.
     */
    public static void registerAll(String namespace, ConfigOption... options) {
        for (ConfigOption option : options) {
            register(namespace, option);
        }
    }

    // ── Queries ────────────────────────────────────────────────────────────

    /**
     * @return All options registered under the given namespace, or an empty list.
     */
    public static List<ConfigOption> getOptions(String namespace) {
        return Collections.unmodifiableList(OPTIONS.getOrDefault(namespace, List.of()));
    }

    /**
     * @return Every option across all namespaces (flat list).
     */
    public static List<ConfigOption> getAllOptions() {
        return OPTIONS.values().stream()
            .flatMap(Collection::stream)
            .toList();
    }

    /**
     * @return All registered namespaces (mod ids).
     */
    public static Set<String> getNamespaces() {
        return Collections.unmodifiableSet(OPTIONS.keySet());
    }

    /**
     * Look up a single option by namespace + id.
     */
    public static Optional<ConfigOption> find(String namespace, String id) {
        return getOptions(namespace).stream().filter(o -> o.id().equals(id)).findFirst();
    }
}
