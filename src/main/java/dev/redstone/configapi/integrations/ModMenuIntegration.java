package dev.redstone.configapi.integrations;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return dev.redstone.configapi.screen.ConfigScreenFactory.forNamespace("configapi");
    }
}