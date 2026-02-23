package dev.clc.compat;

import dev.clc.config.CLCConfig;
import dev.clc.cull.SectionInteriorCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;

public class CLCConfigScreen extends Screen {

    private final Screen parent;

    private boolean enabled;
    private boolean standardCulling;
    private boolean extendedCulling;
    private boolean passDemotion;
    private CLCConfig.CullMode mode;
    private int     interiorThreshold;
    private boolean extendedDetection;

    public CLCConfigScreen(Screen parent) {
        super(Text.literal("Complex Leaf Culling Settings"));
        this.parent = parent;
        CLCConfig c = CLCConfig.INSTANCE;
        this.enabled           = c.enabled;
        this.standardCulling   = c.enableStandardCulling;
        this.extendedCulling   = c.enableExtendedCulling;
        this.passDemotion      = c.enablePassDemotion;
        this.mode              = c.mode;
        this.interiorThreshold = c.interiorThreshold;
        this.extendedDetection = c.useExtendedLeafDetection;
    }

    private static final int W   = 200;
    private static final int H   = 20;
    private static final int GAP = 6;

    @Override
    protected void init() {
        int cx   = this.width / 2;
        int y    = 36;
        int step = H + GAP;

        // ── Master toggle ─────────────────────────────────────────────
        addDrawableChild(CyclingButtonWidget.onOffBuilder(enabled)
            .tooltip(v -> Tooltip.of(Text.literal(
                "Master switch. Off = completely vanilla leaf rendering.")))
            .build(cx - W / 2, y, W, H, Text.literal("Complex Leaf Culling Enabled"),
                (b, v) -> enabled = v));
        y += step + 4;

        // ── Standard culling ──────────────────────────────────────────
        addDrawableChild(CyclingButtonWidget.onOffBuilder(standardCulling)
            .tooltip(v -> Tooltip.of(Text.literal(
                "Standard leaf culling — face level.\n" +
                "Marks shared faces between adjacent leaves as invisible,\n" +
                "skipping them during mesh building. Works for all leaves\n" +
                "and cooperates with Sodium's occlusion system.\n" +
                "This is what most culling mods do.")))
            .build(cx - W / 2, y, W, H, Text.literal("Standard Leaf Culling"),
                (b, v) -> standardCulling = v));
        y += step;

        // ── Extended culling ──────────────────────────────────────────
        addDrawableChild(CyclingButtonWidget.onOffBuilder(extendedCulling)
            .tooltip(v -> Tooltip.of(Text.literal(
                "Extended leaf culling — block level (requires Sodium).\n" +
                "Cancels rendering entirely for interior leaf blocks that\n" +
                "are fully surrounded by other leaves. Most effective for\n" +
                "dense WWOO canopies and modded leaves standard tools miss.")))
            .build(cx - W / 2, y, W, H, Text.literal("Extended Leaf Culling"),
                (b, v) -> extendedCulling = v));
        y += step;

        // ── Pass demotion ─────────────────────────────────────────────
        addDrawableChild(CyclingButtonWidget.onOffBuilder(passDemotion)
            .tooltip(v -> Tooltip.of(Text.literal(
                "Edge pass demotion (requires Sodium).\n" +
                "Leaf blocks on the edge of a canopy that partially qualify\n" +
                "are demoted from expensive translucent rendering to cheaper\n" +
                "cutout mode. Leaf shapes are fully preserved.\n" +
                "Threshold is automatic: one below the cancel threshold.\n" +
                "Most noticeable with shader packs.")))
            .build(cx - W / 2, y, W, H, Text.literal("Edge Pass Demotion"),
                (b, v) -> passDemotion = v));
        y += step + 4;

        // ── Mode ──────────────────────────────────────────────────────
        addDrawableChild(CyclingButtonWidget
            .<CLCConfig.CullMode>builder(m -> switch (m) {
                case CONSERVATIVE -> Text.literal("Culling Mode: Conservative (6/6)");
                case BALANCED     -> Text.literal("Culling Mode: Balanced (5/6)");
                case AGGRESSIVE   -> Text.literal("Culling Mode: Aggressive (4/6)");
            })
            .values(CLCConfig.CullMode.values())
            .initially(mode)
            .tooltip(v -> Tooltip.of(Text.literal(
                "How many of a leaf block's 6 neighbours must also be leaves\n" +
                "before it is treated as interior and cancelled.\n" +
                "Conservative (6/6) = only perfectly buried blocks\n" +
                "Balanced (5/6)     = good balance of visuals and performance\n" +
                "Aggressive (4/6)   = maximum performance, slight edge thinning\n" +
                "Pass demotion fires at one below this threshold automatically.")))
            .build(cx - W / 2, y, W, H, Text.literal("Culling Mode"),
                (b, v) -> mode = v));
        y += step;

        // ── Threshold override ────────────────────────────────────────
        addDrawableChild(new CallbackSlider(cx - W / 2, y, W, H,
            interiorThreshold, 0, 6,
            v -> interiorThreshold = v,
            v -> v == 0 ? "Threshold Override: Auto" : "Threshold Override: " + v + "/6"));
        y += step + 4;

        // ── Extended detection ────────────────────────────────────────
        addDrawableChild(CyclingButtonWidget.onOffBuilder(extendedDetection)
            .tooltip(v -> Tooltip.of(Text.literal(
                "Treat blocks with 'leaf'/'leaves'/'foliage' in their registry\n" +
                "name as leaves, even if not tagged minecraft:leaves.\n" +
                "Required for WWOO and many other modded leaf types.")))
            .build(cx - W / 2, y, W, H, Text.literal("Extended Leaf Detection"),
                (b, v) -> extendedDetection = v));
        y += step + 4;

        // ── Save / Cancel ─────────────────────────────────────────────
        int half = W / 2 - 2;
        addDrawableChild(ButtonWidget
            .builder(Text.literal("Save and Close"), b -> { applyAndSave(); close(); })
            .dimensions(cx - W / 2, y, half, H).build());
        addDrawableChild(ButtonWidget
            .builder(Text.literal("Cancel"), b -> close())
            .dimensions(cx + 2, y, half, H).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        renderBackground(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 14, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Changes apply immediately after Save"),
            width / 2, 24, 0xAAAAAA);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    private void applyAndSave() {
        CLCConfig c      = CLCConfig.INSTANCE;
        c.enabled                  = enabled;
        c.enableStandardCulling    = standardCulling;
        c.enableExtendedCulling    = extendedCulling;
        c.enablePassDemotion       = passDemotion;
        c.mode                     = mode;
        c.interiorThreshold        = interiorThreshold;
        c.useExtendedLeafDetection = extendedDetection;
        c.save();
        SectionInteriorCache.invalidateAll();
        if (client != null && client.worldRenderer != null) {
            client.worldRenderer.reload();
        }
    }

    private static final class CallbackSlider extends SliderWidget {

        private final int         min;
        private final int         max;
        private final IntConsumer onChange;
        private final IntFormatter label;

        @FunctionalInterface interface IntFormatter { String format(int v); }

        CallbackSlider(int x, int y, int w, int h,
                       int initial, int min, int max,
                       IntConsumer onChange, IntFormatter label) {
            super(x, y, w, h,
                  Text.literal(label.format(initial)),
                  (double)(initial - min) / Math.max(1, max - min));
            this.min      = min;
            this.max      = max;
            this.onChange = onChange;
            this.label    = label;
        }

        @Override protected void updateMessage() { setMessage(Text.literal(label.format(currentInt()))); }
        @Override protected void applyValue()    { onChange.accept(currentInt()); }
        private int currentInt() { return min + (int) Math.round(value * (max - min)); }
    }
}
