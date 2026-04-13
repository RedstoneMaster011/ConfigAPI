package dev.redstone.configapi.screen;

import dev.redstone.configapi.api.ConfigOption;
import dev.redstone.configapi.api.ConfigRegistry;
import dev.redstone.configapi.config.ConfigState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The main config screen for a single mod namespace.
 * Features: search, sort, sync/experimental/enabled-only filters, conflict resolution.
 *
 * <p>Open it from ModMenu via {@link ConfigScreenFactory}.
 */
@Environment(EnvType.CLIENT)
public final class ConfigScreen extends Screen {

    // ── Colours ────────────────────────────────────────────────────────────
    private static final int BG_TOP         = 0xFF0F1923;
    private static final int BG_BOTTOM      = 0xFF141E2B;
    private static final int PANEL_BG       = 0xCC1A2535;
    private static final int PANEL_BORDER   = 0xFF2A3A50;
    private static final int ROW_BG         = 0xCC1E2E42;
    private static final int ROW_BG_HOVERED = 0xCC263446;
    private static final int TEXT_PRIMARY   = 0xFFE8EEF4;
    private static final int TEXT_MUTED     = 0xFF7A9AB8;
    private static final int TEXT_WARNING   = 0xFFF4C842;
    private static final int SWITCH_ON      = 0xFF3A9E5F;
    private static final int SWITCH_OFF     = 0xFF3A4E64;
    private static final int SWITCH_KNOB    = 0xFFF0F4F8;
    private static final int FILTER_BG      = 0xAA111D2A;

    private static final int ROW_HEIGHT = 52;
    private static final int TOGGLE_W   = 46;
    private static final int TOGGLE_H   = 18;

    // ── State ──────────────────────────────────────────────────────────────
    private final Screen parent;
    private final String namespace;
    private final List<ConfigOption> allOptions;
    private final Map<String, Boolean> pendingState = new LinkedHashMap<>();

    private TextFieldWidget searchBox;
    private ButtonWidget resetButton;
    private ButtonWidget filterDrawerButton;
    private ButtonWidget sortButton;
    private ButtonWidget experimentalButton;
    private ButtonWidget enabledOnlyButton;

    private List<ConfigOption> filtered = List.of();
    private SortMode sortMode           = SortMode.ALPHABETICAL;
    private ExperimentalFilter expFilter = ExperimentalFilter.ALL;
    private boolean enabledOnly;
    private boolean filtersOpen;
    private int scrollOffset;

    // Prompts
    private ConflictPrompt conflictPrompt;
    private ResetPrompt resetPrompt;

    // ── Constructor ────────────────────────────────────────────────────────

    public ConfigScreen(Screen parent, String namespace) {
        super(Text.literal(namespace));
        this.parent     = parent;
        this.namespace  = namespace;
        this.allOptions = ConfigRegistry.getOptions(namespace);
        for (ConfigOption opt : allOptions) {
            pendingState.put(opt.id(), ConfigState.isEnabled(namespace, opt.id()));
        }
    }

    // ── Screen lifecycle ───────────────────────────────────────────────────

    @Override
    protected void init() {
        int gap = 8;
        int cx  = 20;
        int cw  = width - 40;
        int cr  = cx + cw;

        int doneW  = 84, resetW = 126;
        int doneX  = cr - doneW;
        int resetX = doneX - gap - resetW;
        int actionY = height - 28;

        resetButton = ButtonWidget.builder(Text.literal("Reset to Defaults"), b -> openResetPrompt())
                .dimensions(resetX, actionY, resetW, 20).build();
        addDrawableChild(resetButton);

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(doneX, actionY, doneW, 20).build());

        int searchX = cx;
        int searchW = cw;
        searchBox = new TextFieldWidget(textRenderer, searchX, 28, searchW, 20,
                Text.literal("Search options"));
        searchBox.setPlaceholder(Text.literal("Search name, tags, or description…"));
        searchBox.setChangedListener(v -> { scrollOffset = 0; refreshFiltered(); });
        addDrawableChild(searchBox);

        int half = (cw - gap) / 2;
        sortButton = ButtonWidget.builder(Text.literal(sortMode.label()), b -> {
            sortMode = sortMode.next();
            b.setMessage(Text.literal(sortMode.label()));
            refreshFiltered();
        }).dimensions(cx, 54, half, 20).build();
        addDrawableChild(sortButton);

        filterDrawerButton = ButtonWidget.builder(filterDrawerLabel(), b -> {
            filtersOpen = !filtersOpen;
            updateFilterVisibility();
            b.setMessage(filterDrawerLabel());
        }).dimensions(cx + half + gap, 54, half, 20).build();
        addDrawableChild(filterDrawerButton);

        experimentalButton = ButtonWidget.builder(Text.literal(expFilter.label()), b -> {
            expFilter = expFilter.next();
            b.setMessage(Text.literal(expFilter.label()));
            refreshFiltered();
        }).dimensions(cx, 80, half, 20).build();
        addDrawableChild(experimentalButton);

        enabledOnlyButton = ButtonWidget.builder(enabledOnlyLabel(), b -> {
            enabledOnly = !enabledOnly;
            b.setMessage(enabledOnlyLabel());
            refreshFiltered();
        }).dimensions(cx + half + gap, 80, half, 20).build();
        addDrawableChild(enabledOnlyButton);

        updateFilterVisibility();
        refreshFiltered();
        updateResetButtonState();
    }

    // ── Rendering ──────────────────────────────────────────────────────────

    /**
     * Suppress Minecraft's default background/blur pass so our custom
     * gradient and fills are not rendered behind a blur layer.
     */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionally empty — we draw our own background in render().
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 1. Our background — drawn first, before super.render() widgets
        ctx.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);

        // 2. Header
        ctx.drawTextWithShadow(textRenderer, Text.literal(namespace), 20, 10, TEXT_PRIMARY);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(filtered.size() + " options shown"),
                20 + textRenderer.getWidth(Text.literal(namespace)) + 8, 10, TEXT_MUTED);

        // 3. Filter drawer background
        if (filtersOpen) {
            int fy = 74, fh = 38;
            ctx.fill(20, fy, width - 20, fy + fh, FILTER_BG);
            ctx.fill(20, fy, width - 20, fy + 1, PANEL_BORDER);
            ctx.fill(20, fy + fh - 1, width - 20, fy + fh, PANEL_BORDER);
            ctx.drawTextWithShadow(textRenderer, Text.literal("Filters"), 28, fy - 10, TEXT_MUTED);
        }

        // 4. Feature list panel
        int listTop    = listTopY();
        int listBottom = height - 36;
        int listH      = listBottom - listTop;
        int listX = 20, listW = width - 40;

        ctx.fill(listX, listTop, listX + listW, listBottom, PANEL_BG);
        ctx.fill(listX, listTop, listX + listW, listTop + 1, PANEL_BORDER);
        ctx.fill(listX, listBottom - 1, listX + listW, listBottom, PANEL_BORDER);

        // 5. Rows
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        int start = scrollOffset;
        int end   = Math.min(filtered.size(), start + visibleRows);

        if (filtered.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Text.literal("No options match."), listX + 12, listTop + 12, TEXT_MUTED);
        } else {
            for (int i = start; i < end; i++) {
                ConfigOption opt = filtered.get(i);
                int rowY = listTop + 8 + (i - start) * ROW_HEIGHT;
                boolean hovered = mouseX >= listX + 8 && mouseX <= listX + listW - 8
                        && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT - 4;
                renderRow(ctx, opt, listX + 8, rowY, listW - 16, ROW_HEIGHT - 4, hovered);
            }
            String pag = (start + 1) + "–" + end + " of " + filtered.size();
            ctx.drawTextWithShadow(textRenderer, Text.literal(pag), listX + 12, listBottom - 14, TEXT_MUTED);
        }

        // 6. Scrollbar
        if (filtered.size() > visibleRows) {
            int trackH = listH - 8;
            int trackX = listX + listW - 6;
            int trackY = listTop + 4;
            ctx.fill(trackX, trackY, trackX + 4, trackY + trackH, 0x33FFFFFF);
            int thumbH = Math.max(16, trackH * visibleRows / filtered.size());
            int thumbY = trackY + (trackH - thumbH) * scrollOffset / Math.max(1, filtered.size() - visibleRows);
            ctx.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xAAB0C8E0);
        }

        // 7. Minecraft widgets (buttons, text fields) — drawn on top of our custom UI
        super.render(ctx, mouseX, mouseY, delta);

        // 8. Modal prompts — always on top of everything
        if (resetPrompt != null)    renderResetPrompt(ctx, mouseX, mouseY);
        if (conflictPrompt != null) renderConflictPrompt(ctx, mouseX, mouseY);
    }

    private void renderRow(DrawContext ctx, ConfigOption opt, int x, int y, int w, int h, boolean hovered) {
        ctx.fill(x, y, x + w, y + h, hovered ? ROW_BG_HOVERED : ROW_BG);
        ctx.fill(x, y, x + w, y + 1, PANEL_BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, PANEL_BORDER);

        boolean on  = isPending(opt.id());
        int toggleX = x + w - TOGGLE_W - 10;
        int toggleY = y + (h - TOGGLE_H) / 2;
        int textW   = Math.max(50, toggleX - x - 18);

        ctx.drawTextWithShadow(textRenderer, Text.literal(fitText(opt.name(), textW)), x + 10, y + 6, TEXT_PRIMARY);
        ctx.drawTextWithShadow(textRenderer, Text.literal(fitText(opt.description(), textW)), x + 10, y + 18, TEXT_MUTED);

        // Custom badges from .badges("Label", "color", ...)
        int bx = x + 10;
        int by = y + h - 18;
        for (ConfigOption.BadgeInfo badge : opt.badges()) {
            bx = drawBadge(ctx, bx, by, badge.label(), badge.color(), toggleX - 6);
        }
        // Built-in status badges
        if (opt.requiresNewWorld()) bx = drawBadge(ctx, bx, by, "New World",    0xFF8C6DAA, toggleX - 6);
        if (opt.experimental())     bx = drawBadge(ctx, bx, by, "Experimental", 0xFF9E7A3A, toggleX - 6);
        if (!opt.compatible())           drawBadge(ctx, bx, by, "Unsupported",  0xFF9E4040, toggleX - 6);

        // Toggle switch
        ctx.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, on ? SWITCH_ON : SWITCH_OFF);
        int knobX = toggleX + (on ? TOGGLE_W - 16 : 2);
        ctx.fill(knobX, toggleY + 2, knobX + 14, toggleY + TOGGLE_H - 2, SWITCH_KNOB);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(on ? "ON" : "OFF"),
                toggleX + TOGGLE_W / 2, toggleY + 5, on ? 0xFFDDFFDD : 0xFFCCCCCC);
    }

    private void renderResetPrompt(DrawContext ctx, int mx, int my) {
        int bw = Math.min(340, width - 40);
        int bh = 126;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        drawModal(ctx, bx, by, bw, bh);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Reset to Defaults?"), bx + 12, by + 10, TEXT_PRIMARY);
        ctx.drawTextWrapped(textRenderer,
                Text.literal("This will restore all options to their default state."),
                bx + 12, by + 26, bw - 24, TEXT_MUTED);
        boolean hoverConfirm = insideBounds(mx, my, bx + 12, by + 88, 112, 20);
        ctx.fill(bx + 12, by + 88, bx + 124, by + 108, hoverConfirm ? 0xFF3A5E2A : 0xFF2A4E1A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Confirm"), bx + 68, by + 93, 0xFFCCFFCC);
        boolean hoverCancel = insideBounds(mx, my, bx + 132, by + 88, 80, 20);
        ctx.fill(bx + 132, by + 88, bx + 212, by + 108, hoverCancel ? 0xFF4A3030 : 0xFF3A2020);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Cancel"), bx + 172, by + 93, 0xFFFFCCCC);
    }

    private void renderConflictPrompt(DrawContext ctx, int mx, int my) {
        int bw = Math.min(320, width - 40);
        int bh = 118;
        int bx = (width - bw) / 2;
        int by = (height - bh) / 2;
        drawModal(ctx, bx, by, bw, bh);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Conflict"), bx + 12, by + 10, TEXT_WARNING);
        ctx.drawTextWrapped(textRenderer,
                Text.literal(conflictPrompt.message()), bx + 12, by + 26, bw - 24, TEXT_MUTED);
        boolean hoverReplace = insideBounds(mx, my, bx + 12, by + 82, 104, 20);
        ctx.fill(bx + 12, by + 82, bx + 116, by + 102, hoverReplace ? 0xFF3A5E2A : 0xFF2A4E1A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Replace"), bx + 64, by + 87, 0xFFCCFFCC);
        boolean hoverCancel = insideBounds(mx, my, bx + 124, by + 82, 80, 20);
        ctx.fill(bx + 124, by + 82, bx + 204, by + 102, hoverCancel ? 0xFF4A3030 : 0xFF3A2020);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Cancel"), bx + 164, by + 87, 0xFFFFCCCC);
    }

    private void drawModal(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(0, 0, width, height, 0x88000000);
        ctx.fill(x, y, x + w, y + h, 0xFF1A2535);
        ctx.fill(x, y, x + w, y + 1, PANEL_BORDER);
        ctx.fill(x, y + h - 1, x + w, y + h, PANEL_BORDER);
        ctx.fill(x, y, x + 1, y + h, PANEL_BORDER);
        ctx.fill(x + w - 1, y, x + w, y + h, PANEL_BORDER);
    }

    private int drawBadge(DrawContext ctx, int x, int y, String label, int color, int maxX) {
        int w = textRenderer.getWidth(Text.literal(label)) + 8;
        if (x + w > maxX) return x;
        ctx.fill(x, y, x + w, y + 12, color & 0x80FFFFFF | 0x40000000);
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + 11, x + w, y + 12, color);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + 4, y + 2, 0xFFFFFFFF);
        return x + w + 4;
    }

    // ── Input ──────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (resetPrompt != null) {
            int bw = Math.min(340, width - 40), bh = 126;
            int bx = (width - bw) / 2, by = (height - bh) / 2;
            if (insideBounds(mx, my, bx + 12, by + 88, 112, 20)) { doReset(); return true; }
            if (insideBounds(mx, my, bx + 132, by + 88, 80, 20)) { resetPrompt = null; return true; }
            return true;
        }
        if (conflictPrompt != null) {
            int bw = Math.min(320, width - 40), bh = 118;
            int bx = (width - bw) / 2, by = (height - bh) / 2;
            if (insideBounds(mx, my, bx + 12, by + 82, 104, 20))  { applyConflictReplacement(); return true; }
            if (insideBounds(mx, my, bx + 124, by + 82, 80, 20))  { conflictPrompt = null; return true; }
            return true;
        }
        if (handleToggleClick(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int listTop     = listTopY();
        int listBottom  = height - 36;
        int listH       = listBottom - listTop;
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        int maxScroll   = Math.max(0, filtered.size() - visibleRows);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll));
        return true;
    }

    @Override
    public void close() {
        for (Map.Entry<String, Boolean> e : pendingState.entrySet()) {
            ConfigState.setEnabled(namespace, e.getKey(), e.getValue());
        }
        ConfigState.save(namespace);
        if (client != null) client.setScreen(parent);
    }

    // ── Toggle logic ───────────────────────────────────────────────────────

    private boolean handleToggleClick(double mx, double my) {
        int listTop     = listTopY();
        int listH       = height - 36 - listTop;
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        int start = scrollOffset;
        int end   = Math.min(filtered.size(), start + visibleRows);
        int listX = 20, listW = width - 40;

        for (int i = start; i < end; i++) {
            int rowY    = listTop + 8 + (i - start) * ROW_HEIGHT;
            int toggleX = listX + 8 + (listW - 16) - TOGGLE_W - 10;
            int toggleY = rowY + (ROW_HEIGHT - 4 - TOGGLE_H) / 2;
            if (insideBounds(mx, my, toggleX, toggleY, TOGGLE_W, TOGGLE_H)) {
                requestToggle(filtered.get(i));
                return true;
            }
        }
        return false;
    }

    private void requestToggle(ConfigOption opt) {
        if (!opt.compatible()) return;

        if (isPending(opt.id())) {
            pendingState.put(opt.id(), false);
            refreshFiltered();
            return;
        }

        Set<String> activeConflicts = opt.conflicts().stream()
                .filter(this::isPending)
                .collect(Collectors.toSet());

        if (activeConflicts.isEmpty()) {
            pendingState.put(opt.id(), true);
            refreshFiltered();
        } else {
            String conflictNames = allOptions.stream()
                    .filter(o -> activeConflicts.contains(o.id()))
                    .map(ConfigOption::name)
                    .sorted()
                    .collect(Collectors.joining(", "));
            conflictPrompt = new ConflictPrompt(
                    opt.id(), activeConflicts,
                    opt.name() + " conflicts with " + conflictNames + ". Replace the active selection?");
        }
    }

    private void applyConflictReplacement() {
        if (conflictPrompt == null) return;
        for (String id : conflictPrompt.conflictingIds()) {
            pendingState.put(id, false);
        }
        pendingState.put(conflictPrompt.requestedId(), true);
        conflictPrompt = null;
        refreshFiltered();
    }

    // ── Filtering / sorting ────────────────────────────────────────────────

    private void refreshFiltered() {
        String query = searchBox == null ? "" : searchBox.getText().trim().toLowerCase(Locale.ROOT);
        filtered = allOptions.stream()
                .filter(o -> query.isBlank() || matchesQuery(o, query))
                .filter(o -> !enabledOnly || isPending(o.id()))
                .filter(o -> expFilter.matches(o))
                .sorted(sortMode.comparator(pendingState))
                .toList();

        int listH       = height - 36 - listTopY();
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        scrollOffset = Math.min(scrollOffset, Math.max(0, filtered.size() - visibleRows));

        if (filterDrawerButton != null) filterDrawerButton.setMessage(filterDrawerLabel());
        updateResetButtonState();
    }

    private boolean matchesQuery(ConfigOption o, String q) {
        if (o.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (o.description().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (o.category().toLowerCase(Locale.ROOT).contains(q)) return true;
        // Also search badge labels
        for (ConfigOption.BadgeInfo badge : o.badges()) {
            if (badge.label().toLowerCase(Locale.ROOT).contains(q)) return true;
        }
        return false;
    }

    private void updateFilterVisibility() {
        experimentalButton.visible = filtersOpen;
        enabledOnlyButton.visible  = filtersOpen;
    }

    // ── Reset ──────────────────────────────────────────────────────────────

    private void openResetPrompt() {
        if (!hasPendingChanges()) return;
        conflictPrompt = null;
        resetPrompt = new ResetPrompt();
    }

    private void doReset() {
        for (ConfigOption opt : allOptions) {
            pendingState.put(opt.id(), opt.defaultEnabled());
        }
        resetPrompt = null;
        conflictPrompt = null;
        refreshFiltered();
    }

    private boolean hasPendingChanges() {
        for (ConfigOption opt : allOptions) {
            if (isPending(opt.id()) != opt.defaultEnabled()) return true;
        }
        return false;
    }

    private void updateResetButtonState() {
        if (resetButton != null) resetButton.active = hasPendingChanges();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private boolean isPending(String id) {
        return pendingState.getOrDefault(id, false);
    }

    private int listTopY() {
        return filtersOpen ? 106 : 80;
    }

    private String fitText(String text, int maxWidth) {
        if (textRenderer == null || textRenderer.getWidth(Text.literal(text)) <= maxWidth) return text;
        while (text.length() > 0 && textRenderer.getWidth(Text.literal(text + "…")) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
    }

    private boolean insideBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private Text filterDrawerLabel() {
        int active = 0;
        if (expFilter != ExperimentalFilter.ALL) active++;
        if (enabledOnly) active++;
        return active == 0 ? Text.literal("Filters") : Text.literal("Filters (" + active + ")");
    }

    private Text enabledOnlyLabel() {
        return Text.literal("Enabled only: " + (enabledOnly ? "On" : "Off"));
    }

    // ── Inner types ────────────────────────────────────────────────────────

    private enum SortMode {
        ALPHABETICAL("Sort: A-Z"),
        ENABLED_FIRST("Sort: Enabled First");

        private final String label;
        SortMode(String l) { label = l; }
        String label() { return label; }
        SortMode next() { return values()[(ordinal() + 1) % values().length]; }
        Comparator<ConfigOption> comparator(Map<String, Boolean> state) {
            return switch (this) {
                case ALPHABETICAL  -> Comparator.comparing(ConfigOption::name);
                case ENABLED_FIRST -> Comparator.<ConfigOption, Boolean>comparing(
                                o -> !state.getOrDefault(o.id(), false))
                        .thenComparing(ConfigOption::name);
            };
        }
    }

    private enum ExperimentalFilter {
        ALL("Experimental: All"), HIDE("Experimental: Hide"), ONLY("Experimental: Only");
        private final String label;
        ExperimentalFilter(String l) { label = l; }
        String label() { return label; }
        ExperimentalFilter next() { return values()[(ordinal() + 1) % values().length]; }
        boolean matches(ConfigOption o) {
            return switch (this) {
                case ALL  -> true;
                case HIDE -> !o.experimental();
                case ONLY -> o.experimental();
            };
        }
    }

    private record ConflictPrompt(String requestedId, Set<String> conflictingIds, String message) {}
    private record ResetPrompt() {}
}