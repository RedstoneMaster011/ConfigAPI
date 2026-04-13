package dev.redstone.configapi.screen;

import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Helper that other mods use to create a ModMenu-compatible config screen
 * for their namespace.
 *
 * <p>In your mod's client init, implement {@link ModMenuApi} like this:
 * <pre>{@code
 * public class MyModMenuIntegration implements ModMenuApi {
 *     \@Override
 *     public ConfigScreenFactory<?> getModConfigScreenFactory() {
 *         return ConfigScreenFactory.forNamespace("my_mod");
 *     }
 * }
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public final class ConfigScreenFactory {

    private ConfigScreenFactory() {}

    /**
     * Returns a {@link com.terraformersmc.modmenu.api.ConfigScreenFactory} that opens
     * the ConfigAPI screen for the given namespace.
     *
     * @param namespace Your mod id - must have options registered via {@link dev.redstone.configapi.api.ConfigRegistry}.
     */
    public static com.terraformersmc.modmenu.api.ConfigScreenFactory<?> forNamespace(String namespace) {
        return parent -> new ConfigScreen(parent, namespace);
    }
}
