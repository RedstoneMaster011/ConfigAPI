# ConfigAPI

A simple, flexible config screen API for Fabric mods. Drop in the JAR, register your options, and get a fully-featured in-game config screen with search, filters, conflict resolution, and persistent saves — for free.

**Supports:** Minecraft 1.21 – 1.21.11 · Fabric

---

## Installation (JAR-in-JAR / Local)

Since ConfigAPI is not hosted on a mod repository, you need to bundle the JAR directly into your mod.

### 1. Add the JAR to your project

Place `configapi-x.x.x.jar` inside your project, typically in a `libs/` folder:

```
my-mod/
├── libs/
│   └── configapi-x.x.x.jar
├── src/
└── build.gradle
```

### 2. Add it as a dependency in `build.gradle`

```groovy
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    modImplementation fileTree(dir: 'libs', include: ['*.jar'])
    include fileTree(dir: 'libs', include: ['*.jar']) // bundles it into your JAR
}
```

The `include` line is important — it tells Fabric's loom to nest ConfigAPI inside your output JAR so players only need to install your mod.

### 3. Declare the dependency in `fabric.mod.json`

```json
"depends": {
    "configapi": "*"
}
```

---

## Quick Start

### 1. Register your options

Call `ConfigRegistry.register()` during your mod's `onInitialize()`. Each option needs a unique snake_case id and a display name. Everything else is optional.

```java
public class MyMod implements ModInitializer {

    public static final String MOD_ID = "my_mod";

    @Override
    public void onInitialize() {

        ConfigRegistry.register(MOD_ID,
            new ConfigOption.Builder("my_feature", "My Feature")
                .description("Does something cool.")
                .badges("Client", "blue", "Visual", "cyan")
                .defaultEnabled(true)
                .build()
        );

        ConfigRegistry.register(MOD_ID,
            new ConfigOption.Builder("experimental_thing", "Experimental Thing")
                .description("Might cause issues.")
                .badges("Server", "green")
                .experimental(true)
                .requiresNewWorld(true)
                .build()
        );

        // Load saved state from disk (or seed defaults if first run)
        ConfigState.load(MOD_ID);
    }
}
```

### 2. Check option state at runtime

```java
boolean enabled = ConfigState.isEnabled("my_mod", "my_feature");

if (enabled) {
    // do the thing
}
```

### 3. Toggle programmatically (optional)

```java
ConfigState.setEnabled("my_mod", "my_feature", true);
ConfigState.save("my_mod"); // persist to disk
```

Config is saved to `.minecraft/config/configapi/<namespace>.json` automatically when the player clicks Done in the screen.

---

## ConfigOption Builder Reference

```java
new ConfigOption.Builder("option_id", "Display Name")
    .description("Shown under the name in the config screen.")
    .category("General")             // for your own organisation, not shown in UI yet
    .badges("Label", "color", ...)   // see Badge Colors section below
    .defaultEnabled(true)            // default: false
    .experimental(true)              // shows an "Experimental" badge automatically
    .requiresNewWorld(true)          // shows a "New World" badge automatically
    .compatible(false)               // marks the option as unsupported/greyed out
    .conflicts(Set.of("other_id"))   // prompts the player when enabling conflicting options
    .supportNote("Some note")        // currently stored, reserved for future UI use
    .build()
```

---

## Badge Colors

Badges are defined as alternating label/color pairs passed to `.badges()`:

```java
.badges("Client", "blue", "Visual", "cyan", "Special", "#FF4444")
```

### Named Colors

| Name | | Name | | Name |
|------|-|------|-|------|
| `red` | | `green` | | `blue` |
| `crimson` | | `lime` | | `navy` |
| `rose` | | `emerald` | | `sky` |
| | | `teal` | | `cyan` / `aqua` |
| `yellow` | | `purple` | | `white` |
| `gold` | | `violet` | | `silver` |
| `amber` | | `pink` | | `gray` / `grey` |
| `orange` | | `magenta` | | `slate` |
| `peach` | | `lavender` | | `dark` / `black` |

**Minecraft-themed:** `dirt`, `grass`, `diamond`, `netherite`, `redstone`, `lapis`, `quartz`

### Hex Colors

All standard hex formats are accepted:

```java
.badges("Label", "#FF4444")       // #RRGGBB
.badges("Label", "#F44")          // #RGB shorthand
.badges("Label", "0xFFCC2244")    // 0xAARRGGBB  (with alpha)
.badges("Label", "CC2244")        // plain RRGGBB, no prefix
```

---

## Conflict Resolution

If two options conflict with each other, declare it on both sides. When a player tries to enable one while the other is already on, the screen will show a confirmation prompt asking if they want to replace the active one.

```java
ConfigRegistry.register(MOD_ID,
    new ConfigOption.Builder("classic_hud", "Classic HUD")
        .conflicts(Set.of("modern_hud"))
        .build()
);

ConfigRegistry.register(MOD_ID,
    new ConfigOption.Builder("modern_hud", "Modern HUD")
        .conflicts(Set.of("classic_hud"))
        .build()
);
```

---

## Opening the Config Screen

### Via ModMenu (recommended)

If you want your mod to show a config button in ModMenu's mod list, add the following to your `fabric.mod.json`:

```json
"entrypoints": {
    "modmenu": ["com.example.mymod.ModMenuIntegration"]
}
```

Then create the integration class:

```java
package com.example.mymod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.redstone.configapi.screen.ConfigScreenFactory;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return dev.redstone.configapi.screen.ConfigScreenFactory.forNamespace("my_mod");
    }
}
```

ModMenu is an optional dependency — ConfigAPI does not require it. If you don't include it, just open the screen another way (keybind, command, etc).

### Via keybind (example)

```java
// In your client init:
KeyBinding configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
    "key.my_mod.open_config",
    InputUtil.Type.KEYSYM,
    GLFW.GLFW_KEY_UNKNOWN,
    "category.my_mod"
));

ClientTickEvents.END_CLIENT_TICK.register(client -> {
    if (configKey.wasPressed()) {
        client.setScreen(new dev.redstone.configapi.screen.ConfigScreen(
            client.currentScreen, "my_mod"));
    }
});
```

### Via command (example)

```java
CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
    dispatcher.register(literal("mymodconfig").executes(ctx -> {
        MinecraftClient client = MinecraftClient.getInstance();
        client.send(() -> client.setScreen(
            new dev.redstone.configapi.screen.ConfigScreen(null, "my_mod")));
        return 1;
    }));
});
```

---

## Config Screen Features

Players get the following out of the box with no extra work from you:

- **Search** — filters by name, description, category, and badge labels
- **Sort** — alphabetical or enabled-first
- **Filters** — hide/show experimental options, show only enabled options
- **Conflict prompts** — automatic UI when enabling a conflicting option
- **Reset to defaults** — with a confirmation prompt
- **Persistent state** — saved to `config/configapi/<namespace>.json` on Done

---

## Config File Format

State is stored as a plain JSON file at:

```
.minecraft/config/configapi/<your_mod_id>.json
```

Example:

```json
{
  "my_feature": true,
  "experimental_thing": false,
  "classic_hud": true
}
```

Players can edit this file directly if they want. On next load, `ConfigState.load()` will pick it up.

---

## API Summary

| Class | Purpose |
|---|---|
| `ConfigOption` + `ConfigOption.Builder` | Define a single config option |
| `ConfigRegistry` | Register and look up options by namespace |
| `ConfigState` | Read/write enabled state, load/save to disk |
| `ConfigScreen` | The in-game GUI screen |
| `ConfigScreenFactory` | Creates a ModMenu-compatible screen factory |