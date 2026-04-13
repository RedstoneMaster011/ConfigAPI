package dev.redstone.configapi.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ConfigOption {

    public enum InputType { TOGGLE, TEXT, NUMBER, SLIDER }

    private final String id;
    private final String name;
    private final String description;
    private final String category;
    private final boolean compatible;
    private final boolean requiresNewWorld;
    private final boolean experimental;
    private final String supportNote;
    private final List<BadgeInfo> badges;
    private final Set<String> conflicts;

    private final InputType inputType;

    private final boolean defaultEnabled;
    private final String defaultText;
    private final int maxTextLength;
    private final int defaultNumber;
    private final int minNumber;
    private final int maxNumber;
    private final float defaultSlider;
    private final float minSlider;
    private final float maxSlider;
    private final float stepSlider;

    private ConfigOption(Builder b) {
        this.id               = b.id;
        this.name             = b.name;
        this.description      = b.description;
        this.category         = b.category;
        this.compatible       = b.compatible;
        this.requiresNewWorld = b.requiresNewWorld;
        this.experimental     = b.experimental;
        this.supportNote      = b.supportNote;
        this.badges           = List.copyOf(b.badges);
        this.conflicts        = Set.copyOf(b.conflicts);
        this.inputType        = b.inputType;
        this.defaultEnabled   = b.defaultEnabled;
        this.defaultText      = b.defaultText;
        this.maxTextLength    = b.maxTextLength;
        this.defaultNumber    = b.defaultNumber;
        this.minNumber        = b.minNumber;
        this.maxNumber        = b.maxNumber;
        this.defaultSlider    = b.defaultSlider;
        this.minSlider        = b.minSlider;
        this.maxSlider        = b.maxSlider;
        this.stepSlider       = b.stepSlider;
    }

    public String id()               { return id; }
    public String name()             { return name; }
    public String description()      { return description; }
    public String category()         { return category; }
    public boolean compatible()      { return compatible; }
    public boolean requiresNewWorld(){ return requiresNewWorld; }
    public boolean experimental()    { return experimental; }
    public String supportNote()      { return supportNote; }
    public List<BadgeInfo> badges()  { return badges; }
    public Set<String> conflicts()   { return conflicts; }
    public InputType inputType()     { return inputType; }
    public boolean defaultEnabled()  { return defaultEnabled; }
    public String defaultText()      { return defaultText; }
    public int maxTextLength()       { return maxTextLength; }
    public int defaultNumber()       { return defaultNumber; }
    public int minNumber()           { return minNumber; }
    public int maxNumber()           { return maxNumber; }
    public float defaultSlider()     { return defaultSlider; }
    public float minSlider()         { return minSlider; }
    public float maxSlider()         { return maxSlider; }
    public float stepSlider()        { return stepSlider; }

    public String defaultValueString() {
        return switch (inputType) {
            case TOGGLE -> String.valueOf(defaultEnabled);
            case TEXT   -> defaultText;
            case NUMBER -> String.valueOf(defaultNumber);
            case SLIDER -> String.valueOf(defaultSlider);
        };
    }

    public record BadgeInfo(String label, int color) {}

    public static int resolveColor(String color) {
        if (color == null) return 0xFF8899AA;
        return switch (color.trim().toLowerCase()) {
            case "red"          -> 0xFFBC4040;
            case "crimson"      -> 0xFFAA2244;
            case "rose"         -> 0xFFCC5577;
            case "green"        -> 0xFF3A9E5F;
            case "lime"         -> 0xFF6DBF45;
            case "emerald"      -> 0xFF2ECC71;
            case "teal"         -> 0xFF2A9D8F;
            case "blue"         -> 0xFF4A88CC;
            case "navy"         -> 0xFF2A4A80;
            case "sky"          -> 0xFF5BC0DE;
            case "cyan"         -> 0xFF30AAAA;
            case "aqua"         -> 0xFF00BCD4;
            case "purple"       -> 0xFF8866CC;
            case "violet"       -> 0xFF6A4CAA;
            case "pink"         -> 0xFFCC6688;
            case "magenta"      -> 0xFFBB44AA;
            case "lavender"     -> 0xFF9988CC;
            case "yellow"       -> 0xFFCCAA30;
            case "gold"         -> 0xFFFFAA00;
            case "amber"        -> 0xFFFFB347;
            case "orange"       -> 0xFFCC7733;
            case "peach"        -> 0xFFDD8866;
            case "white"        -> 0xFFDDEEFF;
            case "silver"       -> 0xFFAABBCC;
            case "gray", "grey" -> 0xFF778899;
            case "slate"        -> 0xFF556677;
            case "dark"         -> 0xFF334455;
            case "black"        -> 0xFF222233;
            case "dirt"         -> 0xFF8B6340;
            case "grass"        -> 0xFF5D9E3A;
            case "diamond"      -> 0xFF4FD9D9;
            case "netherite"    -> 0xFF4A4040;
            case "redstone"     -> 0xFFCC2222;
            case "lapis"        -> 0xFF2255AA;
            case "quartz"       -> 0xFFCCBBAA;
            default             -> parseHexColor(color.trim());
        };
    }

    private static int parseHexColor(String s) {
        try {
            String hex = s.startsWith("#")                         ? s.substring(1)
                    : s.startsWith("0x") || s.startsWith("0X") ? s.substring(2)
                      : s;
            if (hex.length() == 3) {
                hex = "" + hex.charAt(0) + hex.charAt(0)
                        + hex.charAt(1) + hex.charAt(1)
                        + hex.charAt(2) + hex.charAt(2);
            }
            if (hex.length() == 6) hex = "FF" + hex;
            return (int) Long.parseLong(hex, 16);
        } catch (NumberFormatException e) {
            return 0xFF8899AA;
        }
    }

    public static final class Builder {
        private final String id;
        private final String name;
        private String description       = "";
        private String category          = "General";
        private boolean compatible       = true;
        private boolean requiresNewWorld = false;
        private boolean experimental     = false;
        private String supportNote       = "";
        private List<BadgeInfo> badges   = new ArrayList<>();
        private Set<String> conflicts    = Set.of();

        private InputType inputType    = InputType.TOGGLE;
        private boolean defaultEnabled = false;
        private String defaultText     = "";
        private int maxTextLength      = 128;
        private int defaultNumber      = 0;
        private int minNumber          = Integer.MIN_VALUE;
        private int maxNumber          = Integer.MAX_VALUE;
        private float defaultSlider    = 0f;
        private float minSlider        = 0f;
        private float maxSlider        = 1f;
        private float stepSlider       = 0.01f;

        public Builder(String id, String name) {
            if (id == null || id.isBlank())     throw new IllegalArgumentException("id must not be blank");
            if (name == null || name.isBlank()) throw new IllegalArgumentException("name must not be blank");
            this.id   = id;
            this.name = name;
        }

        public Builder description(String v)     { this.description = v; return this; }
        public Builder category(String v)         { this.category = v; return this; }
        public Builder compatible(boolean v)      { this.compatible = v; return this; }
        public Builder requiresNewWorld(boolean v){ this.requiresNewWorld = v; return this; }
        public Builder experimental(boolean v)    { this.experimental = v; return this; }
        public Builder supportNote(String v)      { this.supportNote = v; return this; }
        public Builder conflicts(Set<String> v)   { this.conflicts = v; return this; }

        public Builder defaultEnabled(boolean v) {
            this.inputType      = InputType.TOGGLE;
            this.defaultEnabled = v;
            return this;
        }

        public Builder textInput(String defaultValue, int maxLength) {
            this.inputType     = InputType.TEXT;
            this.defaultText   = defaultValue == null ? "" : defaultValue;
            this.maxTextLength = maxLength;
            return this;
        }

        public Builder numberInput(int defaultValue, int min, int max) {
            this.inputType     = InputType.NUMBER;
            this.defaultNumber = defaultValue;
            this.minNumber     = min;
            this.maxNumber     = max;
            return this;
        }

        public Builder slider(float defaultValue, float min, float max, float step) {
            this.inputType     = InputType.SLIDER;
            this.defaultSlider = defaultValue;
            this.minSlider     = min;
            this.maxSlider     = max;
            this.stepSlider    = step;
            return this;
        }

        public Builder badges(String... labelColorPairs) {
            if (labelColorPairs.length % 2 != 0)
                throw new IllegalArgumentException("badges() requires alternating label/color pairs");
            this.badges = new ArrayList<>();
            for (int i = 0; i < labelColorPairs.length; i += 2)
                this.badges.add(new BadgeInfo(labelColorPairs[i], resolveColor(labelColorPairs[i + 1])));
            return this;
        }

        public Builder badge(String label, int argbColor) {
            this.badges.add(new BadgeInfo(label, argbColor));
            return this;
        }

        public ConfigOption build() { return new ConfigOption(this); }
    }
}