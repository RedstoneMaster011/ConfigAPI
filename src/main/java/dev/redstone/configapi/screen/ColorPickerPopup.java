package dev.redstone.configapi.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public final class ColorPickerPopup extends Screen {

    private static final int BG            = 0xFF1A2535;
    private static final int BORDER        = 0xFF2A3A50;
    private static final int TEXT_PRIMARY  = 0xFFE8EEF4;
    private static final int TEXT_MUTED    = 0xFF7A9AB8;
    private static final int INPUT_BG      = 0xFF0F1E2E;
    private static final int INPUT_BORDER  = 0xFF2A4A6A;
    private static final int CHECKERBOARD1 = 0xFF888888;
    private static final int CHECKERBOARD2 = 0xFF555555;

    private static final int POP_W      = 290;
    private static final int POP_H      = 250;
    private static final int SV_W       = 180;
    private static final int SV_H       = 130;
    private static final int SV_X_OFF   = 10;
    private static final int SV_Y_OFF   = 28;
    private static final int HUE_W      = 16;
    private static final int HUE_X_OFF  = SV_X_OFF + SV_W + 8;
    private static final int SWATCH     = 20;
    private static final int FIELD_H    = 14;
    private static final int FIELD_W_HEX = 72;
    private static final int FIELD_W_RGB = 36;

    private final Screen parent;
    private final Consumer<String> onConfirm;

    private int r, g, b;
    private float hue = 0f;
    private float sat = 1f;
    private float val = 1f;

    private TextFieldWidget hexField;
    private TextFieldWidget rField, gField, bField;

    private boolean draggingSV  = false;
    private boolean draggingHue = false;
    private boolean syncing     = false;

    private int px, py;

    public ColorPickerPopup(Screen parent, String initialHex, Consumer<String> onConfirm) {
        super(Text.literal("Color Picker"));
        this.parent    = parent;
        this.onConfirm = onConfirm;
        int argb = parseHex(initialHex);
        this.r = (argb >> 16) & 0xFF;
        this.g = (argb >>  8) & 0xFF;
        this.b =  argb        & 0xFF;
        float[] hsv = rgbToHsv(r, g, b);
        this.hue = hsv[0];
        this.sat = hsv[1];
        this.val = hsv[2];
    }

    @Override
    protected void init() {
        px = (width  - POP_W) / 2;
        py = (height - POP_H) / 2;

        int fieldsY = py + SV_Y_OFF + SV_H + 14;

        int hexX = px + SV_X_OFF + SWATCH + 20;
        hexField = new TextFieldWidget(textRenderer, hexX, fieldsY, FIELD_W_HEX, FIELD_H, Text.literal("Hex"));
        hexField.setMaxLength(7);
        hexField.setDrawsBackground(false);
        hexField.setText(toHexNoHash(r, g, b));
        hexField.setChangedListener(v -> {
            if (syncing) return;
            String s = v.startsWith("#") ? v.substring(1) : v;
            if (s.length() == 6) {
                try {
                    int rgb = (int) Long.parseLong(s, 16);
                    r = (rgb >> 16) & 0xFF;
                    g = (rgb >>  8) & 0xFF;
                    b =  rgb        & 0xFF;
                    float[] hsv = rgbToHsv(r, g, b);
                    hue = hsv[0]; sat = hsv[1]; val = hsv[2];
                    syncFromRgb(false);
                } catch (NumberFormatException ignored) {}
            }
        });
        addDrawableChild(hexField);

        int rgbY = fieldsY + FIELD_H + 8;
        int rX = px + SV_X_OFF + 16;
        int gX = rX + FIELD_W_RGB + 26;
        int bX = gX + FIELD_W_RGB + 26;

        rField = makeRgbField(rX, rgbY, r, v -> { r = v; float[] hsv = rgbToHsv(r,g,b); hue=hsv[0]; sat=hsv[1]; val=hsv[2]; syncFromRgb(true); });
        gField = makeRgbField(gX, rgbY, g, v -> { g = v; float[] hsv = rgbToHsv(r,g,b); hue=hsv[0]; sat=hsv[1]; val=hsv[2]; syncFromRgb(true); });
        bField = makeRgbField(bX, rgbY, b, v -> { b = v; float[] hsv = rgbToHsv(r,g,b); hue=hsv[0]; sat=hsv[1]; val=hsv[2]; syncFromRgb(true); });
        addDrawableChild(rField);
        addDrawableChild(gField);
        addDrawableChild(bField);

        int btnY = py + POP_H - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), b -> confirm())
                .dimensions(px + POP_W / 2 - 82, btnY, 76, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> cancel())
                .dimensions(px + POP_W / 2 + 6, btnY, 76, 20).build());
    }

    private TextFieldWidget makeRgbField(int x, int y, int initial, java.util.function.IntConsumer onChange) {
        TextFieldWidget tw = new TextFieldWidget(textRenderer, x, y, FIELD_W_RGB, FIELD_H, Text.literal(""));
        tw.setMaxLength(3);
        tw.setDrawsBackground(false);
        tw.setText(String.valueOf(initial));
        tw.setChangedListener(v -> {
            if (syncing) return;
            try { onChange.accept(Math.max(0, Math.min(255, Integer.parseInt(v)))); }
            catch (NumberFormatException ignored) {}
        });
        return tw;
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

        ctx.drawTextWithShadow(textRenderer, Text.literal("Color Picker"), px + 10, py + 10, TEXT_PRIMARY);

        int svX = px + SV_X_OFF;
        int svY = py + SV_Y_OFF;

        for (int col = 0; col < SV_W; col++) {
            float s = (float) col / (SV_W - 1);
            int topColor    = hsvToRgbArgb(hue, s, 1.0f);
            int bottomColor = hsvToRgbArgb(hue, s, 0.0f);
            ctx.fillGradient(svX + col, svY, svX + col + 1, svY + SV_H, topColor, bottomColor);
        }
        drawBorder(ctx, svX - 1, svY - 1, SV_W + 2, SV_H + 2);

        int cursorX = svX + Math.round(sat * (SV_W - 1));
        int cursorY = svY + Math.round((1f - val) * (SV_H - 1));
        ctx.fillGradient(cursorX - 4, cursorY - 1, cursorX + 4, cursorY + 1, 0xFFFFFFFF, 0xFFFFFFFF);
        ctx.fillGradient(cursorX - 1, cursorY - 4, cursorX + 1, cursorY + 4, 0xFFFFFFFF, 0xFFFFFFFF);

        int hueX = px + HUE_X_OFF;
        int hueY = py + SV_Y_OFF;
        for (int row = 0; row < SV_H; row++) {
            float h = (float) row / (SV_H - 1) * 360f;
            int c = hsvToRgbArgb(h, 1f, 1f);
            ctx.fillGradient(hueX, hueY + row, hueX + HUE_W, hueY + row + 1, c, c);
        }
        drawBorder(ctx, hueX - 1, hueY - 1, HUE_W + 2, SV_H + 2);

        int hueCursorY = hueY + Math.round(hue / 360f * (SV_H - 1));
        ctx.fillGradient(hueX - 2, hueCursorY - 1, hueX + HUE_W + 2, hueCursorY + 1, 0xFFFFFFFF, 0xFFFFFFFF);

        int fieldsY   = py + SV_Y_OFF + SV_H + 14;
        int swatchX   = px + SV_X_OFF;
        for (int ty = 0; ty < SWATCH; ty += 4)
            for (int tx = 0; tx < SWATCH; tx += 4) {
                boolean odd = ((tx / 4 + ty / 4) % 2 == 1);
                ctx.fillGradient(swatchX + tx, fieldsY + ty, swatchX + tx + 4, fieldsY + ty + 4,
                        odd ? CHECKERBOARD1 : CHECKERBOARD2, odd ? CHECKERBOARD1 : CHECKERBOARD2);
            }
        int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
        ctx.fillGradient(swatchX, fieldsY, swatchX + SWATCH, fieldsY + SWATCH, argb, argb);
        drawBorder(ctx, swatchX - 1, fieldsY - 1, SWATCH + 2, SWATCH + 2);

        ctx.drawTextWithShadow(textRenderer, Text.literal("#"), swatchX + SWATCH + 8, fieldsY, TEXT_MUTED);
        drawFieldBox(ctx, hexField.getX() - 1, hexField.getY() - 4, FIELD_W_HEX + 2, FIELD_H + 2);

        int rgbY = fieldsY + FIELD_H + 8;
        ctx.drawTextWithShadow(textRenderer, Text.literal("R"), px + SV_X_OFF, rgbY + 4, 0xFFFF8888);
        ctx.drawTextWithShadow(textRenderer, Text.literal("G"), rField.getX() + FIELD_W_RGB + 8, rgbY + 4, 0xFF88FF88);
        ctx.drawTextWithShadow(textRenderer, Text.literal("B"), gField.getX() + FIELD_W_RGB + 8, rgbY + 4, 0xFF88AAFF);

        rField.setPosition(rField.getX(), rgbY + 3);
        gField.setPosition(gField.getX(), rgbY + 3);
        bField.setPosition(bField.getX(), rgbY + 3);

        drawFieldBox(ctx, rField.getX() - 1, rField.getY() - 4, FIELD_W_RGB + 2, FIELD_H + 2);
        drawFieldBox(ctx, gField.getX() - 1, gField.getY() - 4, FIELD_W_RGB + 2, FIELD_H + 2);
        drawFieldBox(ctx, bField.getX() - 1, bField.getY() - 4, FIELD_W_RGB + 2, FIELD_H + 2);

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawModal(DrawContext ctx) {
        ctx.fillGradient(px, py, px + POP_W, py + POP_H, BG, BG);
        drawBorder(ctx, px, py, POP_W, POP_H);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fillGradient(x, y,         x + w,     y + 1,     BORDER, BORDER);
        ctx.fillGradient(x, y + h - 1, x + w,     y + h,     BORDER, BORDER);
        ctx.fillGradient(x, y,         x + 1,     y + h,     BORDER, BORDER);
        ctx.fillGradient(x + w - 1, y, x + w,     y + h,     BORDER, BORDER);
    }

    private void drawFieldBox(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fillGradient(x, y, x + w, y + h, INPUT_BORDER, INPUT_BORDER);
        ctx.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, INPUT_BG, INPUT_BG);
    }

    @Override
    public boolean mouseClicked(Click click, boolean primary) {
        double mx = click.x();
        double my = click.y();

        int svX  = px + SV_X_OFF,  svY  = py + SV_Y_OFF;
        int hueX = px + HUE_X_OFF, hueY = py + SV_Y_OFF;

        if (mx >= svX && mx <= svX + SV_W && my >= svY && my <= svY + SV_H) {
            draggingSV = true;
            applySVPick(
                    Math.max(svX, Math.min(svX + SV_W, mx)),
                    Math.max(svY, Math.min(svY + SV_H, my))
            );
            return true;
        }
        if (mx >= hueX && mx <= hueX + HUE_W && my >= hueY && my <= hueY + SV_H) {
            draggingHue = true;
            applyHuePick(Math.max(hueY, Math.min(hueY + SV_H, my)));
            return true;
        }

        int btnY     = py + POP_H - 28;
        int confirmX = px + POP_W / 2 - 82;
        int cancelX  = px + POP_W / 2 + 6;
        if (mx >= confirmX && mx < confirmX + 76 && my >= btnY && my < btnY + 20) { confirm(); return true; }
        if (mx >= cancelX  && mx < cancelX  + 76 && my >= btnY && my < btnY + 20) { cancel();  return true; }

        return super.mouseClicked(click, primary);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (draggingSV)  {
            int svX = px + SV_X_OFF, svY = py + SV_Y_OFF;
            double clampedX = Math.max(svX, Math.min(svX + SV_W, click.x()));
            double clampedY = Math.max(svY, Math.min(svY + SV_H, click.y()));
            applySVPick(clampedX, clampedY);
            return true;
        }
        if (draggingHue) {
            int hueY = py + SV_Y_OFF;
            double clampedY = Math.max(hueY, Math.min(hueY + SV_H, click.y()));
            applyHuePick(clampedY);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingSV || draggingHue) {
            draggingSV  = false;
            draggingHue = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    private void applySVPick(double mx, double my) {
        int svX = px + SV_X_OFF, svY = py + SV_Y_OFF;
        sat = (float) Math.max(0, Math.min(1, (mx - svX) / (SV_W - 1)));
        val = (float) Math.max(0, Math.min(1, 1.0 - (my - svY) / (SV_H - 1)));
        int argb = hsvToRgbArgb(hue, sat, val);
        r = (argb >> 16) & 0xFF;
        g = (argb >>  8) & 0xFF;
        b =  argb        & 0xFF;
        syncFromRgb(true);
    }

    private void applyHuePick(double my) {
        int hueY = py + SV_Y_OFF;
        hue = (float) Math.max(0, Math.min(360, (my - hueY) / (SV_H - 1) * 360f));
        int argb = hsvToRgbArgb(hue, sat, val);
        r = (argb >> 16) & 0xFF;
        g = (argb >>  8) & 0xFF;
        b =  argb        & 0xFF;
        syncFromRgb(true);
    }

    private void syncFromRgb(boolean syncHex) {
        syncing = true;
        if (syncHex) hexField.setText(toHexNoHash(r, g, b));
        rField.setText(String.valueOf(r));
        gField.setText(String.valueOf(g));
        bField.setText(String.valueOf(b));
        syncing = false;
    }

    private void confirm() {
        onConfirm.accept("#" + toHexNoHash(r, g, b));
        if (client != null) client.setScreen(parent);
    }

    private void cancel() {
        if (client != null) client.setScreen(parent);
    }

    @Override public boolean shouldCloseOnEsc() { return true; }
    @Override public void close() { cancel(); }

    private static int parseHex(String s) {
        if (s == null) return 0xFFFFFF;
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        try { return (int)(Long.parseLong(h, 16)) & 0x00FFFFFF; }
        catch (NumberFormatException e) { return 0xFFFFFF; }
    }

    private static String toHexNoHash(int r, int g, int b) {
        return String.format("%02X%02X%02X", r, g, b);
    }

    private static float[] rgbToHsv(int r, int g, int b) {
        float rf = r / 255f, gf = g / 255f, bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        float h = 0f, s = (max == 0f) ? 0f : delta / max, v = max;
        if (delta != 0f) {
            if      (max == rf) h = ((gf - bf) / delta) % 6f;
            else if (max == gf) h = (bf - rf) / delta + 2f;
            else                h = (rf - gf) / delta + 4f;
            h *= 60f;
            if (h < 0) h += 360f;
        }
        return new float[]{h, s, v};
    }

    private static int hsvToRgbArgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1f - Math.abs((h / 60f) % 2f - 1f));
        float m = v - c;
        float rp, gp, bp;
        if      (h < 60f)  { rp = c; gp = x; bp = 0; }
        else if (h < 120f) { rp = x; gp = c; bp = 0; }
        else if (h < 180f) { rp = 0; gp = c; bp = x; }
        else if (h < 240f) { rp = 0; gp = x; bp = c; }
        else if (h < 300f) { rp = x; gp = 0; bp = c; }
        else               { rp = c; gp = 0; bp = x; }
        return 0xFF000000 | (Math.round((rp+m)*255) << 16) | (Math.round((gp+m)*255) << 8) | Math.round((bp+m)*255);
    }
}