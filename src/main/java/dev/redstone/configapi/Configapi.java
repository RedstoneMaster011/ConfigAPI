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
                new ConfigOption.Builder("my_toggle", "My Toggle")
                        .description("A simple on/off option.")
                        .badges("Client", "blue")
                        .defaultEnabled(true)
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_text", "My Text")
                        .description("Type anything you want here.")
                        .badges("Text", "purple")
                        .textInput("Hello World", 64)
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_number", "My Number")
                        .description("Pick a number between 0 and 100.")
                        .badges("Number", "orange")
                        .numberInput(10, 0, 100)
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_slider", "My Slider")
                        .description("Drag to set a value between 0.0 and 1.0.")
                        .badges("Slider", "teal")
                        .slider(0.5f, 0.0f, 1.0f, 0.05f)
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("experimental_feature", "Experimental Feature")
                        .description("Generates massive crystalline caverns. Might be laggy!")
                        .badges("Server", "green")
                        .defaultEnabled(false)
                        .experimental(true)
                        .requiresNewWorld(true)
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("classic_hud", "Classic HUD")
                        .description("Uses the legacy 1.8 style interface.")
                        .badges("Client", "blue", "Cosmetic", "gray")
                        .conflicts(Set.of("modern_hud"))
                        .build()
        );

        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("modern_hud", "Modern HUD")
                        .description("A sleek, minimalist interface.")
                        .badges("Client", "blue", "Cosmetic", "gray")
                        .conflicts(Set.of("classic_hud"))
                        .build()
        );

        // TOGGLE — incompatible/unsupported option
        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("unsupported_tweak", "Legacy Physics")
                        .description("Currently disabled due to bugs.")
                        .compatible(false)
                        .build()
        );

        ConfigState.load(MOD_ID);

        // ── Reading values at runtime ───────────────────────────────────────
        // Use these anywhere in your mod after ConfigState.load() has been called:
        // boolean toggle = ConfigState.getBoolean(MOD_ID, "my_toggle");
        // String  text   = ConfigState.getString (MOD_ID, "my_text");
        // int     number = ConfigState.getInt    (MOD_ID, "my_number");
        // float   slider = ConfigState.getFloat  (MOD_ID, "my_slider");
    }
}