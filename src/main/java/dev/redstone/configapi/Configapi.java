package dev.redstone.configapi;

import dev.redstone.configapi.api.ConfigOption;
import dev.redstone.configapi.api.ConfigRegistry;
import dev.redstone.configapi.config.ConfigState;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class Configapi implements ModInitializer {

    public static final String MOD_ID = "configapi";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[ConfigAPI] Initialized.");

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("id1", "Enhanced uhh nothing")
                        .description("Improves smooth lighting transitions.")
                        .badges("hmm", "blue", "cool", "cyan")
                        .defaultEnabled(true)
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("uhh_smh", "Experimental something")
                        .description("Generates massive crystalline caverns. Might be laggy!")
                        .badges("Client? Server? Both? idk", "green")
                        .experimental(true)
                        .requiresNewWorld(true)
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("test1", "test1")
                        .description("this is incompadable with test2")
                        .badges("TestFlag", "blue", "test", "gray")
                        .conflicts(Set.of("test2"))
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("test2", "this is incompadable with test1")
                        .description("A sleek, minimalist interface.")
                        .badges("uhh", "blue", "sure", "gray")
                        .conflicts(Set.of("test1"))
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("unsupported_tweak", "test_config")
                        .description("This feature is currently disabled")
                        .compatible(false)
                        .build()
        );

        ConfigState.load(MOD_ID);

        //Example on how to use it or smh
        /*
        int[] ticker = {0};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) return;
            if (++ticker[0] % 20 != 0) return;
            for (ConfigOption opt : ConfigRegistry.getOptions(MOD_ID)) {
                boolean enabled = ConfigState.isEnabled(MOD_ID, opt.id());
                LOGGER.info("[ConfigAPI] {} is: {}", opt.name(), enabled ? "turned on" : "turned off");
            }
        });
        */
    }
}