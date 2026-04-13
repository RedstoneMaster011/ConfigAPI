package dev.redstone.configapi.screen;

import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ConfigScreenFactory {

    private ConfigScreenFactory() {}

    public static com.terraformersmc.modmenu.api.ConfigScreenFactory<?> forNamespace(String namespace) {
        return parent -> new ConfigScreen(parent, namespace);
    }
}
