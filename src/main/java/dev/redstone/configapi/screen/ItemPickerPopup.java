package dev.redstone.configapi.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class ItemPickerPopup extends Screen {

    private static final int BG           = 0xFF1A2535;
    private static final int BORDER       = 0xFF2A3A50;
    private static final int TEXT_PRIMARY = 0xFFE8EEF4;
    private static final int TEXT_MUTED   = 0xFF7A9AB8;
    private static final int INPUT_BG     = 0xFF0F1E2E;
    private static final int INPUT_BORDER = 0xFF2A4A6A;
    private static final int CELL_HOVER   = 0x44FFFFFF;
    private static final int CELL_SEL     = 0x66AACCFF;
    private static final int GRID_BG      = 0xCC0F1923;

    private static final int POP_W     = 300;
    private static final int POP_H     = 260;
    private static final int CELL_SIZE = 20;
    private static final int GRID_COLS = 12;
    private static final int GRID_X_OFF = 10;
    private static final int GRID_Y_OFF = 58;
    private static final int GRID_W    = CELL_SIZE * GRID_COLS;
    private static final int GRID_H    = POP_H - GRID_Y_OFF - 44;

    private final Screen parent;
    private final Consumer<String> onConfirm;

    private String selectedId;

    private final List<String> allItemIds = new ArrayList<>();
    private List<String> filteredIds = new ArrayList<>();

    private TextFieldWidget searchBox;
    private int scrollOffset = 0;

    private int px, py;

    private String hoveredId = null;

    public ItemPickerPopup(Screen parent, String initialItemId, Consumer<String> onConfirm) {
        super(Text.literal("Item Picker"));
        this.parent     = parent;
        this.onConfirm  = onConfirm;
        this.selectedId = (initialItemId == null) ? "" : initialItemId.trim();
    }

    @Override
    protected void init() {
        px = (width  - POP_W) / 2;
        py = (height - POP_H) / 2;

        allItemIds.clear();
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            allItemIds.add(id.toString());
        }
        allItemIds.sort(String::compareTo);
        filteredIds = new ArrayList<>(allItemIds);

        searchBox = new TextFieldWidget(textRenderer,
                px + GRID_X_OFF, py + 30, GRID_W, 14, Text.literal("Search"));
        searchBox.setPlaceholder(Text.literal("Search items…"));
        searchBox.setDrawsBackground(false);
        searchBox.setChangedListener(q -> {
            scrollOffset = 0;
            applyFilter(q);
        });
        addDrawableChild(searchBox);

        int btnY = py + POP_H - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), btn -> confirm())
                .dimensions(px + POP_W / 2 - 84, btnY, 76, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), btn -> cancel())
                .dimensions(px + POP_W / 2 + 4, btnY, 76, 20).build());
    }

    private void applyFilter(String query) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            filteredIds = new ArrayList<>(allItemIds);
        } else {
            filteredIds = allItemIds.stream()
                    .filter(id -> id.contains(q))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
        clampScroll();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client != null && parent != null) {
            parent.render(ctx, -1, -1, delta);
        }
        ctx.fillGradient(0, 0, width, height, 0xCC000000, 0xCC000000);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        drawModal(ctx);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Select Item"), px + 10, py + 10, TEXT_PRIMARY);

        drawFieldBox(ctx, searchBox.getX() - 2, searchBox.getY() - 4, GRID_W + 2, 16);

        int gridX = px + GRID_X_OFF;
        int gridY = py + GRID_Y_OFF;

        ctx.fillGradient(gridX, gridY, gridX + GRID_W, gridY + GRID_H, GRID_BG, GRID_BG);
        ctx.fillGradient(gridX - 1, gridY - 1, gridX + GRID_W + 1, gridY, BORDER, BORDER);
        ctx.fillGradient(gridX - 1, gridY + GRID_H, gridX + GRID_W + 1, gridY + GRID_H + 1, BORDER, BORDER);
        ctx.fillGradient(gridX - 1, gridY, gridX, gridY + GRID_H, BORDER, BORDER);
        ctx.fillGradient(gridX + GRID_W, gridY, gridX + GRID_W + 1, gridY + GRID_H, BORDER, BORDER);

        int visibleRows = GRID_H / CELL_SIZE;
        int startRow = scrollOffset;
        int startIdx = startRow * GRID_COLS;
        int endIdx   = Math.min(filteredIds.size(), startIdx + visibleRows * GRID_COLS);

        hoveredId = null;

        ctx.enableScissor(gridX, gridY, gridX + GRID_W, gridY + GRID_H);
        for (int idx = startIdx; idx < endIdx; idx++) {
            String id = filteredIds.get(idx);
            int col = (idx - startIdx) % GRID_COLS;
            int row = (idx - startIdx) / GRID_COLS;
            int cx  = gridX + col * CELL_SIZE;
            int cy  = gridY + row * CELL_SIZE;

            boolean hovered = mouseX >= cx && mouseX < cx + CELL_SIZE
                    && mouseY >= cy && mouseY < cy + CELL_SIZE;
            boolean selected = id.equals(selectedId);

            if (selected)      ctx.fillGradient(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, CELL_SEL,   CELL_SEL);
            else if (hovered)  ctx.fillGradient(cx, cy, cx + CELL_SIZE, cy + CELL_SIZE, CELL_HOVER, CELL_HOVER);

            if (hovered) hoveredId = id;

            try {
                Identifier itemId = Identifier.of(id);
                Item item = Registries.ITEM.get(itemId);
                ItemStack stack = new ItemStack(item);
                ctx.drawItem(stack, cx + 2, cy + 2);
            } catch (Exception ignored) {}
        }
        ctx.disableScissor();

        int totalRows = (int) Math.ceil((double) filteredIds.size() / GRID_COLS);
        if (totalRows > visibleRows) {
            int trackH   = GRID_H;
            int trackX   = gridX + GRID_W + 3;
            int thumbH   = Math.max(10, trackH * visibleRows / totalRows);
            int thumbY   = gridY + (trackH - thumbH) * scrollOffset / Math.max(1, totalRows - visibleRows);
            ctx.fillGradient(trackX, gridY, trackX + 3, gridY + GRID_H, 0x33FFFFFF, 0x33FFFFFF);
            ctx.fillGradient(trackX, thumbY, trackX + 3, thumbY + thumbH, 0xAAB0C8E0, 0xAAB0C8E0);
        }

        int labelY = py + GRID_Y_OFF + GRID_H + 6;
        String selText = selectedId.isBlank() ? "None selected" : selectedId;
        ctx.drawTextWithShadow(textRenderer, Text.literal(selText), px + 10, labelY, TEXT_MUTED);

        super.render(ctx, mouseX, mouseY, delta);

        if (hoveredId != null) {
            ctx.drawTooltip(textRenderer, Text.literal(hoveredId), mouseX, mouseY);
        }
    }

    private void drawModal(DrawContext ctx) {
        ctx.fillGradient(px, py, px + POP_W, py + POP_H, BG, BG);
        ctx.fillGradient(px, py, px + POP_W, py + 1, BORDER, BORDER);
        ctx.fillGradient(px, py + POP_H - 1, px + POP_W, py + POP_H, BORDER, BORDER);
        ctx.fillGradient(px, py, px + 1, py + POP_H, BORDER, BORDER);
        ctx.fillGradient(px + POP_W - 1, py, px + POP_W, py + POP_H, BORDER, BORDER);
    }

    private void drawFieldBox(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fillGradient(x, y, x + w, y + h, INPUT_BORDER, INPUT_BORDER);
        ctx.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, INPUT_BG, INPUT_BG);
    }

    @Override
    public boolean mouseClicked(Click click, boolean primary) {
        double mx = click.x();
        double my = click.y();

        int gridX = px + GRID_X_OFF;
        int gridY = py + GRID_Y_OFF;

        if (mx >= gridX && mx < gridX + GRID_W && my >= gridY && my < gridY + GRID_H) {
            int col      = (int)((mx - gridX) / CELL_SIZE);
            int row      = (int)((my - gridY) / CELL_SIZE);
            int startIdx = scrollOffset * GRID_COLS;
            int idx      = startIdx + row * GRID_COLS + col;
            if (idx >= 0 && idx < filteredIds.size()) {
                String clicked = filteredIds.get(idx);
                if (clicked.equals(selectedId)) {
                    confirm();
                } else {
                    selectedId = clicked;
                }
            }
            return true;
        }

        int btnY     = py + POP_H - 28;
        int confirmX = px + POP_W / 2 - 84;
        int cancelX  = px + POP_W / 2 + 4;
        if (mx >= confirmX && mx < confirmX + 76 && my >= btnY && my < btnY + 20) { confirm(); return true; }
        if (mx >= cancelX  && mx < cancelX  + 76 && my >= btnY && my < btnY + 20) { cancel();  return true; }

        return super.mouseClicked(click, primary);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hScroll, double vScroll) {
        int gridX = px + GRID_X_OFF;
        int gridY = py + GRID_Y_OFF;
        if (mx >= gridX && mx <= gridX + GRID_W && my >= gridY && my <= gridY + GRID_H) {
            scrollOffset = Math.max(0, scrollOffset - (int) Math.signum(vScroll));
            clampScroll();
            return true;
        }
        return super.mouseScrolled(mx, my, hScroll, vScroll);
    }

    private void clampScroll() {
        int visibleRows = GRID_H / CELL_SIZE;
        int totalRows   = (int) Math.ceil((double) filteredIds.size() / GRID_COLS);
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalRows - visibleRows)));
    }

    private void confirm() {
        onConfirm.accept(selectedId);
        if (client != null) client.setScreen(parent);
    }

    private void cancel() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldCloseOnEsc() { return true; }

    @Override
    public void close() { cancel(); }
}