# ConfigAPI

A simple, flexible config screen API for Fabric mods. Drop in the JAR, register your options, and get a fully-featured in-game config screen!

**Supports:** Minecraft 1.21.6 – 1.21.11 · Fabric

mod can be downloaded from modrinth: https://modrinth.com/mod/configuration_api

---

## Installation

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
    modImplementation files('libs/configapi-x.x.x.jar') //change name to correct name of jar
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

        // Toggle (on/off switch)
        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_feature", "My Feature")
                        .description("Does something cool.")
                        .badges("Client", "blue", "Visual", "cyan")
                        .defaultEnabled(true)
                        .build()
        );

        // Text input
        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_text", "My Text")
                        .description("A custom string value.")
                        .textInput("Hello World", 64)
                        .build()
        );

        // Number input (integer)
        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_number", "My Number")
                        .description("Pick a number between 0 and 100.")
                        .numberInput(10, 0, 100)
                        .build()
        );

        // Slider (float)
        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_slider", "My Slider")
                        .description("Drag to set a value between 0.0 and 1.0.")
                        .slider(0.5f, 0.0f, 1.0f, 0.05f)
                        .build()
        );
        
        // Color input (Hex string)
        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_color", "My Color")
                        .description("Colors")
                        .colorInput("#FF5500")
                        .badges("Change the color!", "green")
                        .build()
        );

        // Item input (Registry ID)
        ConfigRegistry.register(MOD_ID,
                new ConfigOption.Builder("my_item", "My Item")
                        .description("Items")
                        .itemInput("minecraft:diamond")
                        .badges("Select a item!", "green")
                        .build()
        );

        // Load saved state from disk (or seed defaults if first run)
        ConfigState.load(MOD_ID);
    }
}
```

### 2. Read option values at runtime

```java
boolean toggle = ConfigState.getBoolean("my_mod", "my_feature");
String  text   = ConfigState.getString ("my_mod", "my_text");
int     number = ConfigState.getInt    ("my_mod", "my_number");
float   slider = ConfigState.getFloat  ("my_mod", "my_slider");
int     argb   = ConfigState.getColor  (MOD_ID, "my_color");
String  itemId = ConfigState.getItem   (MOD_ID, "my_item");
```

### 3. Write values programmatically (optional)

```java
ConfigState.setValue("my_mod", "my_feature", "true");
ConfigState.setValue("my_mod", "my_slider",  "0.75");
ConfigState.save("my_mod"); // persist to disk
```

Config is saved to `.minecraft/config/configapi/<namespace>.json` automatically when the player clicks Done in the screen.

---

## Input Types

Every option has an input type that controls what control is shown in the config screen. The type is set by which builder method you call — if you don't call any, it defaults to a toggle.

### Toggle

An on/off switch. This is the default input type.

```java
new ConfigOption.Builder("my_toggle", "My Toggle")
    .defaultEnabled(true)   // default: false
    .build()
```

Read at runtime:
```java
boolean on = ConfigState.getBoolean("my_mod", "my_toggle");
```

---

### Text Input

A free-form text field.

```java
new ConfigOption.Builder("my_text", "My Text")
    .textInput("default value", 128)  // (defaultValue, maxLength)
    .build()
```

Read at runtime:
```java
String value = ConfigState.getString("my_mod", "my_text");
```

---

### Number Input

An integer input field. The value is clamped to `[min, max]` as the player types.

```java
new ConfigOption.Builder("my_number", "My Number")
    .numberInput(10, 0, 100)  // (defaultValue, min, max)
    .build()
```

Read at runtime:
```java
int value = ConfigState.getInt("my_mod", "my_number");
```

---

### Slider

A draggable float slider.

```java
new ConfigOption.Builder("my_slider", "My Slider")
    .slider(0.5f, 0.0f, 1.0f, 0.05f)  // (defaultValue, min, max, step)
    .build()
```

- **step** controls the snap increment — `0.05f` snaps to `0.0, 0.05, 0.10, ...`
- The displayed value trims trailing zeros (`0.5` not `0.50`)

Read at runtime:
```java
float value = ConfigState.getFloat("my_mod", "my_slider");
```

---

Color Input
A dedicated field for hex colors. It automatically handles parsing hex strings into ARGB integers for use in rendering.

```java
new ConfigOption.Builder("my_color", "My Color")
.colorInput("#FF5500") // (defaultHex)
.build()
```

Read at runtime:

```java
int argb = ConfigState.getColor("my_mod", "my_color");
```

Item Input
A field for selecting Minecraft items or blocks via their registry identifier.

```java
new ConfigOption.Builder("my_item", "My Item")
.itemInput("minecraft:diamond") // (defaultIdentifier)
.build()
```

Read at runtime:

```java
String id = ConfigState.getItem("my_mod", "my_item");
```

## ConfigOption Builder Reference

```java
new ConfigOption.Builder("option_id", "Display Name")
    // metadata
    .description("Shown under the name in the config screen.")
    .category("General")              // for your own organisation, not shown in UI yet
    .badges("Label", "color", ...)    // see Badge Colors section below
    .experimental(true)               // shows an "Experimental" badge automatically
    .requiresNewWorld(true)           // shows a "New World" badge automatically
    .compatible(false)                // marks the option as unsupported/greyed out
    .conflicts(Set.of("other_id"))    // prompts the player when enabling conflicting options
    .supportNote("Some note")         // currently stored, reserved for future UI use

    // input type — pick one (default is toggle)
    .defaultEnabled(true)             // TOGGLE: default on/off state
    .textInput("default", 128)        // TEXT:   default value, max character length
    .numberInput(0, 0, 100)           // NUMBER: default, min, max
    .slider(0.5f, 0f, 1f, 0.05f)     // SLIDER: default, min, max, step
    .colorInput("#FFFFFF")            // COLOR: hex
    .itemInput("minecraft:air")       // ITEM  item id

    .build()
```

---

## Badge Colors

Badges are defined as alternating label/color pairs passed to `.badges()`:

```java
.badges("Client", "blue", "Visual", "cyan", "Special", "#FF4444")
```

You can also add a single badge with a raw ARGB int using `.badge(label, argbColor)`.

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

> Conflict resolution only applies to **toggle** options.

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
- **Conflict prompts** — automatic UI when enabling a conflicting toggle option
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
  "my_feature": "true",
  "my_text": "Hello World",
  "my_number": "10",
  "my_slider": "0.5",
  "classic_hud": "true"
}
```

All values are stored as strings regardless of type. On next load, `ConfigState.load()` will pick them up and each typed getter (`getBoolean`, `getInt`, `getFloat`, `getString`) will parse them back correctly. Players can edit this file directly.

---

## API Summary

| Class | Purpose |
|---|---|
| `ConfigOption` + `ConfigOption.Builder` | Define a single config option |
| `ConfigRegistry` | Register and look up options by namespace |
| `ConfigState` | Read/write values, load/save to disk |
| `ConfigScreen` | The in-game GUI screen |
| `ConfigScreenFactory` | Creates a ModMenu-compatible screen factory |

### `ConfigState` method reference

| Method | Returns                       | Use for                          |
|---|-------------------------------|----------------------------------|
| `getBoolean(ns, id)` | `boolean`                     | Toggle options                   |
| `getString(ns, id)` | `String`                      | Text input options               |
| `getInt(ns, id)` | `int`                         | Number input options             |
| `getFloat(ns, id)` | `float`                       | Slider options                   |
| `getRaw(ns, id)` | `String`                      | Any option, unparsed             |
| `setValue(ns, id, value)` | `void`                        | Write any value as string        |
| `load(ns)` | `void`                        | Call once in `onInitialize()`    |
| `save(ns)` | `void`                        | Persist to disk manually         |
| `resetToDefaults(ns)` | `void`                        | Reset all options and save       |
| `enabledIds(ns)` | `Set<String>`                 | All currently-enabled toggle ids |
| `getColor(ns, id)` | 	`int	` | Color options (ARGB)             | 
| `getItem(ns, id)` | `String`	| Item registry IDs |

### `ConfigRegistry` method reference

| Method | Use for |
|---|---|
| `register(ns, option)` | Register a single option |
| `registerAll(ns, options...)` | Register multiple options at once |
| `getOptions(ns)` | Get all options for a namespace |
| `find(ns, id)` | Look up a specific option by id |
| `getNamespaces()` | All registered namespaces |
