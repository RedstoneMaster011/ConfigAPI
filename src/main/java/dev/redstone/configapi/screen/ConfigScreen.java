package dev.redstone.configapi.screen;

import dev.redstone.configapi.api.ConfigOption;
import dev.redstone.configapi.api.ConfigRegistry;
import dev.redstone.configapi.config.ConfigState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public final class ConfigScreen extends Screen {

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
    private static final int SLIDER_TRACK   = 0xFF1A3A50;
    private static final int SLIDER_FILL    = 0xFF3A7AAA;
    private static final int SLIDER_KNOB    = 0xFFB0D0F0;
    private static final int INPUT_BG       = 0xFF0F1E2E;
    private static final int INPUT_BORDER   = 0xFF2A4A6A;
    private static final int FILTER_BG      = 0xAA111D2A;

    private static final int ROW_HEIGHT  = 58;
    private static final int TOGGLE_W    = 46;
    private static final int TOGGLE_H    = 18;
    private static final int SLIDER_W    = 120;
    private static final int SLIDER_H    = 10;
    private static final int NUM_W       = 80;
    private static final int TEXT_INPUT_H = 14;

    private final Screen parent;
    private final String namespace;
    private final List<ConfigOption> allOptions;

    private final Map<String, String> pendingState = new LinkedHashMap<>();

    private final Map<String, TextFieldWidget> textWidgets = new LinkedHashMap<>();

    private TextFieldWidget searchBox;
    private ButtonWidget resetButton;
    private ButtonWidget filterDrawerButton;
    private ButtonWidget sortButton;
    private ButtonWidget experimentalButton;
    private ButtonWidget enabledOnlyButton;

    private List<ConfigOption> filtered = List.of();
    private SortMode sortMode            = SortMode.ALPHABETICAL;
    private ExperimentalFilter expFilter = ExperimentalFilter.ALL;
    private boolean enabledOnly;
    private boolean filtersOpen;
    private int scrollOffset;

    private String draggingSlider = null;


    private ConflictPrompt conflictPrompt;
    private ResetPrompt resetPrompt;

    public ConfigScreen(Screen parent, String namespace) {
        super(Text.literal(namespace));
        this.parent     = parent;
        this.namespace  = namespace;
        this.allOptions = ConfigRegistry.getOptions(namespace);
        for (ConfigOption opt : allOptions) {
            pendingState.put(opt.id(), ConfigState.getRaw(namespace, opt.id()));
        }
    }

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

        searchBox = new TextFieldWidget(textRenderer, cx, 28, cw, 20, Text.literal("Search"));
        searchBox.setPlaceholder(Text.literal("Search name, tags, or description…"));
        searchBox.setChangedListener(v -> { scrollOffset = 0; refreshFiltered(); rebuildTextWidgets(); });
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
            rebuildTextWidgets();
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
        rebuildTextWidgets();
        updateResetButtonState();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, BG_TOP, BG_BOTTOM);

        ctx.drawTextWithShadow(textRenderer, Text.literal(namespace), 20, 10, TEXT_PRIMARY);
        ctx.drawTextWithShadow(textRenderer,
                Text.literal(filtered.size() + " options shown"),
                20 + textRenderer.getWidth(Text.literal(namespace)) + 8, 10, TEXT_MUTED);

        if (filtersOpen) {
            int fy = 74, fh = 38;
            ctx.fillGradient(20, fy, width - 20, fy + fh, FILTER_BG, FILTER_BG);
            ctx.fillGradient(20, fy, width - 20, fy + 1, PANEL_BORDER, PANEL_BORDER);
            ctx.fillGradient(20, fy + fh - 1, width - 20, fy + fh, PANEL_BORDER, PANEL_BORDER);
            ctx.drawTextWithShadow(textRenderer, Text.literal("Filters"), 28, fy - 10, TEXT_MUTED);
        }

        int listTop    = listTopY();
        int listBottom = height - 36;
        int listH      = listBottom - listTop;
        int listX = 20, listW = width - 40;

        ctx.fillGradient(listX, listTop, listX + listW, listBottom, PANEL_BG, PANEL_BG);
        ctx.fillGradient(listX, listTop, listX + listW, listTop + 1, PANEL_BORDER, PANEL_BORDER);
        ctx.fillGradient(listX, listBottom - 1, listX + listW, listBottom, PANEL_BORDER, PANEL_BORDER);

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
                renderRow(ctx, opt, listX + 8, rowY, listW - 16, ROW_HEIGHT - 4, hovered, mouseX, mouseY);
            }
            String pag = (start + 1) + "–" + end + " of " + filtered.size();
            ctx.drawTextWithShadow(textRenderer, Text.literal(pag), listX + 12, listBottom - 14, TEXT_MUTED);
        }

        if (filtered.size() > visibleRows) {
            int trackH = listH - 8;
            int trackX = listX + listW - 6;
            int trackY = listTop + 4;
            ctx.fillGradient(trackX, trackY, trackX + 4, trackY + trackH, 0x33FFFFFF, 0x33FFFFFF);
            int thumbH = Math.max(16, trackH * visibleRows / filtered.size());
            int thumbY = trackY + (trackH - thumbH) * scrollOffset / Math.max(1, filtered.size() - visibleRows);
            ctx.fillGradient(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xAAB0C8E0, 0xAAB0C8E0);
        }

        super.render(ctx, mouseX, mouseY, delta);

        if (resetPrompt != null)    renderResetPrompt(ctx, mouseX, mouseY);
        if (conflictPrompt != null) renderConflictPrompt(ctx, mouseX, mouseY);
    }

    private void renderRow(DrawContext ctx, ConfigOption opt, int x, int y, int w, int h,
                           boolean hovered, int mouseX, int mouseY) {
        ctx.fillGradient(x, y, x + w, y + h, hovered ? ROW_BG_HOVERED : ROW_BG, hovered ? ROW_BG_HOVERED : ROW_BG);
        ctx.fillGradient(x, y, x + w, y + 1, PANEL_BORDER, PANEL_BORDER);
        ctx.fillGradient(x, y + h - 1, x + w, y + h, PANEL_BORDER, PANEL_BORDER);

        int controlW  = controlWidth(opt);
        int controlX  = x + w - controlW - 10;
        int textW     = Math.max(50, controlX - x - 18);

        ctx.drawTextWithShadow(textRenderer, Text.literal(fitText(opt.name(), textW)), x + 10, y + 6, TEXT_PRIMARY);
        ctx.drawTextWithShadow(textRenderer, Text.literal(fitText(opt.description(), textW)), x + 10, y + 18, TEXT_MUTED);

        int bx = x + 10;
        int by = y + h - 18;
        for (ConfigOption.BadgeInfo badge : opt.badges()) {
            bx = drawBadge(ctx, bx, by, badge.label(), badge.color(), controlX - 6);
        }
        if (opt.requiresNewWorld()) bx = drawBadge(ctx, bx, by, "New World",    0xFF8C6DAA, controlX - 6);
        if (opt.experimental())     bx = drawBadge(ctx, bx, by, "Experimental", 0xFF9E7A3A, controlX - 6);
        if (!opt.compatible())           drawBadge(ctx, bx, by, "Unsupported",  0xFF9E4040, controlX - 6);

        int controlY = y + (h - controlHeight(opt)) / 2;
        switch (opt.inputType()) {
            case TOGGLE -> renderToggle(ctx, opt, controlX, controlY);
            case SLIDER -> renderSlider(ctx, opt, controlX, controlY, mouseX, mouseY);
            case TEXT, NUMBER -> {
                TextFieldWidget tw = textWidgets.get(opt.id());
                if (tw != null) {
                    int boxX = controlX - 15;
                    int boxW = controlWidth(opt);
                    ctx.fillGradient(boxX - 1, controlY - 1, boxX + boxW + 1,
                            controlY + TEXT_INPUT_H + 1, INPUT_BORDER, INPUT_BORDER);
                    ctx.fillGradient(boxX, controlY, boxX + boxW,
                            controlY + TEXT_INPUT_H, INPUT_BG, INPUT_BG);
                }
            }
        }
    }

    private void renderToggle(DrawContext ctx, ConfigOption opt, int x, int y) {
        boolean on = "true".equals(pendingState.get(opt.id()));
        ctx.fillGradient(x, y, x + TOGGLE_W, y + TOGGLE_H, on ? SWITCH_ON : SWITCH_OFF, on ? SWITCH_ON : SWITCH_OFF);
        int knobX = x + (on ? TOGGLE_W - 16 : 2);
        ctx.fillGradient(knobX, y + 2, knobX + 14, y + TOGGLE_H - 2, SWITCH_KNOB, SWITCH_KNOB);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(on ? "ON" : "OFF"),
                x + TOGGLE_W / 2, y + 5, on ? 0xFFDDFFDD : 0xFFCCCCCC);
    }

    private void renderSlider(DrawContext ctx, ConfigOption opt, int x, int y, int mouseX, int mouseY) {
        float value   = parseFloat(pendingState.getOrDefault(opt.id(), String.valueOf(opt.defaultSlider())));
        float ratio   = (value - opt.minSlider()) / Math.max(0.0001f, opt.maxSlider() - opt.minSlider());
        int trackY    = y + (TOGGLE_H - SLIDER_H) / 2;

        ctx.fillGradient(x, trackY, x + SLIDER_W, trackY + SLIDER_H, SLIDER_TRACK, SLIDER_TRACK);
        int fillW = (int) (ratio * SLIDER_W);
        ctx.fillGradient(x, trackY, x + fillW, trackY + SLIDER_H, SLIDER_FILL, SLIDER_FILL);
        int knobX = x + fillW - 3;
        ctx.fillGradient(knobX, trackY - 2, knobX + 6, trackY + SLIDER_H + 2, SLIDER_KNOB, SLIDER_KNOB);
        String label = formatSliderValue(value);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(label),
                x + SLIDER_W / 2, trackY + SLIDER_H + 3, TEXT_MUTED);
    }

    private void renderResetPrompt(DrawContext ctx, int mx, int my) {
        int bw = Math.min(340, width - 40), bh = 126;
        int bx = (width - bw) / 2, by = (height - bh) / 2;
        drawModal(ctx, bx, by, bw, bh);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Reset to Defaults?"), bx + 12, by + 10, TEXT_PRIMARY);
        ctx.drawWrappedTextWithShadow(textRenderer,
                Text.literal("This will restore all options to their default state."),
                bx + 12, by + 26, bw - 24, TEXT_MUTED);
        boolean hoverConfirm = insideBounds(mx, my, bx + 12, by + 88, 112, 20);
        ctx.fillGradient(bx + 12, by + 88, bx + 124, by + 108, hoverConfirm ? 0xFF3A5E2A : 0xFF2A4E1A, hoverConfirm ? 0xFF3A5E2A : 0xFF2A4E1A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Confirm"), bx + 68, by + 93, 0xFFCCFFCC);
        boolean hoverCancel = insideBounds(mx, my, bx + 132, by + 88, 80, 20);
        ctx.fillGradient(bx + 132, by + 88, bx + 212, by + 108, hoverCancel ? 0xFF4A3030 : 0xFF3A2020, hoverCancel ? 0xFF4A3030 : 0xFF3A2020);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Cancel"), bx + 172, by + 93, 0xFFFFCCCC);
    }

    private void renderConflictPrompt(DrawContext ctx, int mx, int my) {
        int bw = Math.min(320, width - 40), bh = 118;
        int bx = (width - bw) / 2, by = (height - bh) / 2;
        drawModal(ctx, bx, by, bw, bh);
        ctx.drawTextWithShadow(textRenderer, Text.literal("Conflict"), bx + 12, by + 10, TEXT_WARNING);
        ctx.drawWrappedTextWithShadow(textRenderer,
                Text.literal(conflictPrompt.message()), bx + 12, by + 26, bw - 24, TEXT_MUTED);
        boolean hoverReplace = insideBounds(mx, my, bx + 12, by + 82, 104, 20);
        ctx.fillGradient(bx + 12, by + 82, bx + 116, by + 102, hoverReplace ? 0xFF3A5E2A : 0xFF2A4E1A, hoverReplace ? 0xFF3A5E2A : 0xFF2A4E1A);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Replace"), bx + 64, by + 87, 0xFFCCFFCC);
        boolean hoverCancel = insideBounds(mx, my, bx + 124, by + 82, 80, 20);
        ctx.fillGradient(bx + 124, by + 82, bx + 204, by + 102, hoverCancel ? 0xFF4A3030 : 0xFF3A2020, hoverCancel ? 0xFF4A3030 : 0xFF3A2020);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Cancel"), bx + 164, by + 87, 0xFFFFCCCC);
    }

    private void drawModal(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fillGradient(0, 0, width, height, 0x88000000, 0x88000000);
        ctx.fillGradient(x, y, x + w, y + h, 0xFF1A2535, 0xFF1A2535);
        ctx.fillGradient(x, y, x + w, y + 1, PANEL_BORDER, PANEL_BORDER);
        ctx.fillGradient(x, y + h - 1, x + w, y + h, PANEL_BORDER, PANEL_BORDER);
        ctx.fillGradient(x, y, x + 1, y + h, PANEL_BORDER, PANEL_BORDER);
        ctx.fillGradient(x + w - 1, y, x + w, y + h, PANEL_BORDER, PANEL_BORDER);
    }

    private int drawBadge(DrawContext ctx, int x, int y, String label, int color, int maxX) {
        int w = textRenderer.getWidth(Text.literal(label)) + 8;
        if (x + w > maxX) return x;
        ctx.fillGradient(x, y, x + w, y + 12, color & 0x80FFFFFF | 0x40000000, color & 0x80FFFFFF | 0x40000000);
        ctx.fillGradient(x, y, x + w, y + 1, color, color);
        ctx.fillGradient(x, y + 11, x + w, y + 12, color, color);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + 4, y + 2, 0xFFFFFFFF);
        return x + w + 4;
    }

    private void rebuildTextWidgets() {
        for (TextFieldWidget tw : textWidgets.values()) remove(tw);
        textWidgets.clear();

        int listTop     = listTopY();
        int listBottom  = height - 36;
        int listH       = listBottom - listTop;
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        int start = scrollOffset;
        int end   = Math.min(filtered.size(), start + visibleRows);
        int listX = 20, listW = width - 40;

        for (int i = start; i < end; i++) {
            ConfigOption opt = filtered.get(i);
            if (opt.inputType() != ConfigOption.InputType.TEXT
                    && opt.inputType() != ConfigOption.InputType.NUMBER) continue;
            if (!opt.compatible()) continue;

            int rowY     = listTop + 8 + (i - start) * ROW_HEIGHT;
            int h        = ROW_HEIGHT - 4;
            int controlX = listX + 8 + (listW - 16) - controlWidth(opt) - 10;
            int controlY = rowY + (h - TEXT_INPUT_H) / 2;

            TextFieldWidget tw = new TextFieldWidget(textRenderer,
                    controlX - 14, controlY + 3, controlWidth(opt), TEXT_INPUT_H, Text.literal(opt.name()));
            tw.setMaxLength(opt.inputType() == ConfigOption.InputType.NUMBER ? 10 : opt.maxTextLength());
            tw.setText(pendingState.getOrDefault(opt.id(), opt.defaultValueString()));
            tw.setDrawsBackground(false);

            final String optId = opt.id();
            final ConfigOption finalOpt = opt;
            tw.setChangedListener(val -> {
                if (finalOpt.inputType() == ConfigOption.InputType.NUMBER) {
                    String cleaned = val.replaceAll("[^\\-0-9]", "");
                    if (!cleaned.equals(val)) { tw.setText(cleaned); return; }
                    try {
                        int parsed = Integer.parseInt(cleaned);
                        parsed = Math.max(finalOpt.minNumber(), Math.min(finalOpt.maxNumber(), parsed));
                        pendingState.put(optId, String.valueOf(parsed));
                    } catch (NumberFormatException ignored) {}
                } else {
                    pendingState.put(optId, val);
                }
                updateResetButtonState();
            });

            textWidgets.put(opt.id(), tw);
            addDrawableChild(tw);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean primary) {
        double mx = click.x();
        double my = click.y();
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
        if (primary && tryStartSliderDrag(mx, my)) return true;
        if (primary && handleToggleClick(mx, my)) return true;
        return super.mouseClicked(click, primary);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingSlider != null) {
            updateSliderFromMouse(draggingSlider, click.x());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSlider != null) {
            draggingSlider = null;
            updateResetButtonState();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int listTop     = listTopY();
        int listBottom  = height - 36;
        int listH       = listBottom - listTop;
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        int maxScroll   = Math.max(0, filtered.size() - visibleRows);
        int newOffset   = (int) Math.max(0, Math.min(maxScroll, scrollOffset - vScroll));
        if (newOffset != scrollOffset) {
            scrollOffset = newOffset;
            rebuildTextWidgets();
        }
        return true;
    }

    @Override
    public void close() {
        for (Map.Entry<String, TextFieldWidget> e : textWidgets.entrySet()) {
            pendingState.put(e.getKey(), e.getValue().getText());
        }
        for (Map.Entry<String, String> e : pendingState.entrySet()) {
            ConfigState.setValue(namespace, e.getKey(), e.getValue());
        }
        ConfigState.save(namespace);
        if (client != null) client.setScreen(parent);
    }

    private boolean handleToggleClick(double mx, double my) {
        int listTop     = listTopY();
        int listH       = height - 36 - listTop;
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        int start = scrollOffset;
        int end   = Math.min(filtered.size(), start + visibleRows);
        int listX = 20, listW = width - 40;

        for (int i = start; i < end; i++) {
            ConfigOption opt = filtered.get(i);
            if (opt.inputType() != ConfigOption.InputType.TOGGLE) continue;
            int rowY    = listTop + 8 + (i - start) * ROW_HEIGHT;
            int h       = ROW_HEIGHT - 4;
            int toggleX = listX + 8 + (listW - 16) - TOGGLE_W - 10;
            int toggleY = rowY + (h - TOGGLE_H) / 2;
            if (insideBounds(mx, my, toggleX, toggleY, TOGGLE_W, TOGGLE_H)) {
                requestToggle(opt);
                return true;
            }
        }
        return false;
    }

    private void requestToggle(ConfigOption opt) {
        if (!opt.compatible()) return;
        boolean current = "true".equals(pendingState.get(opt.id()));
        if (current) {
            pendingState.put(opt.id(), "false");
            refreshFiltered();
            updateResetButtonState();
            return;
        }
        Set<String> activeConflicts = opt.conflicts().stream()
                .filter(id -> "true".equals(pendingState.get(id)))
                .collect(Collectors.toSet());
        if (activeConflicts.isEmpty()) {
            pendingState.put(opt.id(), "true");
            refreshFiltered();
            updateResetButtonState();
        } else {
            String conflictNames = allOptions.stream()
                    .filter(o -> activeConflicts.contains(o.id()))
                    .map(ConfigOption::name).sorted()
                    .collect(Collectors.joining(", "));
            conflictPrompt = new ConflictPrompt(opt.id(), activeConflicts,
                    opt.name() + " conflicts with " + conflictNames + ". Replace the active selection?");
        }
    }

    private void applyConflictReplacement() {
        if (conflictPrompt == null) return;
        for (String id : conflictPrompt.conflictingIds()) pendingState.put(id, "false");
        pendingState.put(conflictPrompt.requestedId(), "true");
        conflictPrompt = null;
        refreshFiltered();
        updateResetButtonState();
    }

    private boolean tryStartSliderDrag(double mx, double my) {
        int listTop     = listTopY();
        int listH       = height - 36 - listTop;
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        int start = scrollOffset;
        int end   = Math.min(filtered.size(), start + visibleRows);
        int listX = 20, listW = width - 40;

        for (int i = start; i < end; i++) {
            ConfigOption opt = filtered.get(i);
            if (opt.inputType() != ConfigOption.InputType.SLIDER) continue;
            int rowY    = listTop + 8 + (i - start) * ROW_HEIGHT;
            int h       = ROW_HEIGHT - 4;
            int sliderX = listX + 8 + (listW - 16) - SLIDER_W - 10;
            int sliderY = rowY + (h - TOGGLE_H) / 2;
            if (insideBounds(mx, my, sliderX - 4, sliderY - 4, SLIDER_W + 8, TOGGLE_H + 8)) {
                draggingSlider = opt.id();
                updateSliderFromMouse(opt.id(), mx);
                return true;
            }
        }
        return false;
    }

    private void updateSliderFromMouse(String optId, double mx) {
        ConfigOption opt = allOptions.stream().filter(o -> o.id().equals(optId)).findFirst().orElse(null);
        if (opt == null) return;

        int listX = 20, listW = width - 40;
        int sliderX = listX + 8 + (listW - 16) - SLIDER_W - 10;

        float ratio = (float) Math.max(0, Math.min(1, (mx - sliderX) / SLIDER_W));
        float raw   = opt.minSlider() + ratio * (opt.maxSlider() - opt.minSlider());
        float step  = opt.stepSlider();
        float value = Math.round(raw / step) * step;
        value = Math.max(opt.minSlider(), Math.min(opt.maxSlider(), value));
        pendingState.put(optId, String.valueOf(value));
    }

    private void refreshFiltered() {
        String query = searchBox == null ? "" : searchBox.getText().trim().toLowerCase(Locale.ROOT);
        filtered = allOptions.stream()
                .filter(o -> query.isBlank() || matchesQuery(o, query))
                .filter(o -> !enabledOnly || isEffectivelyEnabled(o))
                .filter(o -> expFilter.matches(o))
                .sorted(sortMode.comparator(pendingState))
                .toList();

        int listH       = height - 36 - listTopY();
        int visibleRows = Math.max(1, (listH - 16) / ROW_HEIGHT);
        scrollOffset = Math.min(scrollOffset, Math.max(0, filtered.size() - visibleRows));

        if (filterDrawerButton != null) filterDrawerButton.setMessage(filterDrawerLabel());
        updateResetButtonState();
        rebuildTextWidgets();
    }

    private boolean isEffectivelyEnabled(ConfigOption o) {
        if (o.inputType() == ConfigOption.InputType.TOGGLE)
            return "true".equals(pendingState.get(o.id()));
        return true;
    }

    private boolean matchesQuery(ConfigOption o, String q) {
        if (o.name().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (o.description().toLowerCase(Locale.ROOT).contains(q)) return true;
        if (o.category().toLowerCase(Locale.ROOT).contains(q)) return true;
        for (ConfigOption.BadgeInfo badge : o.badges())
            if (badge.label().toLowerCase(Locale.ROOT).contains(q)) return true;
        return false;
    }

    private void updateFilterVisibility() {
        experimentalButton.visible = filtersOpen;
        enabledOnlyButton.visible  = filtersOpen;
    }

    private void openResetPrompt() {
        if (!hasPendingChanges()) return;
        conflictPrompt = null;
        resetPrompt = new ResetPrompt();
    }

    private void doReset() {
        for (TextFieldWidget tw : textWidgets.values()) remove(tw);
        textWidgets.clear();
        for (ConfigOption opt : allOptions) {
            pendingState.put(opt.id(), opt.defaultValueString());
        }
        resetPrompt = null;
        conflictPrompt = null;
        refreshFiltered();
    }

    private boolean hasPendingChanges() {
        for (ConfigOption opt : allOptions) {
            String current = pendingState.getOrDefault(opt.id(), opt.defaultValueString());
            if (!current.equals(opt.defaultValueString())) return true;
        }
        return false;
    }

    private void updateResetButtonState() {
        if (resetButton != null) resetButton.active = hasPendingChanges();
    }

    private int controlWidth(ConfigOption opt) {
        return switch (opt.inputType()) {
            case TOGGLE -> TOGGLE_W;
            case SLIDER -> SLIDER_W;
            case TEXT   -> 120;
            case NUMBER -> NUM_W;
        };
    }

    private int controlHeight(ConfigOption opt) {
        return switch (opt.inputType()) {
            case TOGGLE -> TOGGLE_H;
            case SLIDER -> TOGGLE_H;
            case TEXT, NUMBER -> TEXT_INPUT_H;
        };
    }

    private int listTopY() { return filtersOpen ? 106 : 80; }

    private String fitText(String text, int maxWidth) {
        if (textRenderer == null || textRenderer.getWidth(Text.literal(text)) <= maxWidth) return text;
        while (text.length() > 0 && textRenderer.getWidth(Text.literal(text + "…")) > maxWidth)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }

    private boolean insideBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private float parseFloat(String s) {
        try { return Float.parseFloat(s); } catch (NumberFormatException e) { return 0f; }
    }

    private String formatSliderValue(float v) {
        if (v == Math.floor(v)) return String.valueOf((int) v);
        return String.format("%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
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

    private enum SortMode {
        ALPHABETICAL("Sort: A-Z"),
        ENABLED_FIRST("Sort: Enabled First");
        private final String label;
        SortMode(String l) { label = l; }
        String label() { return label; }
        SortMode next() { return values()[(ordinal() + 1) % values().length]; }
        Comparator<ConfigOption> comparator(Map<String, String> state) {
            return switch (this) {
                case ALPHABETICAL  -> Comparator.comparing(ConfigOption::name);
                case ENABLED_FIRST -> Comparator.<ConfigOption, Boolean>comparing(
                                o -> !"true".equals(state.get(o.id())))
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