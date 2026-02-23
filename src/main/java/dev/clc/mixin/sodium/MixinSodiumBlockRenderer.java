package dev.clc.mixin.sodium;

import dev.clc.ComplexLeafCulling;
import dev.clc.config.CLCConfig;
import dev.clc.cull.LeafPredicate;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.frapi.render.AbstractBlockRenderContext;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two-tier optimisation for Sodium's chunk build pipeline:
 *
 * TIER A — Full cancel:
 *   Blocks with ≥ cancelThreshold leaf neighbours are skipped entirely.
 *   Zero geometry generated.
 *
 * TIER B — Pass demotion:
 *   Blocks with ≥ (cancelThreshold - 1) but < cancelThreshold leaf neighbours
 *   have their render material swapped TRANSLUCENT → CUTOUT.
 *   Leaf shape is preserved, expensive translucency pass is dropped.
 *
 * Tiers never overlap — a block qualifies for exactly one or neither.
 */
@Mixin(value = BlockRenderer.class, remap = false)
public class MixinSodiumBlockRenderer {

    // 'slice' is declared protected on AbstractBlockRenderContext (confirmed via javap).
    // @Shadow won't find superclass fields — one-time reflection on the exact declaring class.
    private static final Field SLICE_FIELD;
    static {
        Field f = null;
        try {
            f = AbstractBlockRenderContext.class.getDeclaredField("slice");
            f.setAccessible(true);
            ComplexLeafCulling.LOGGER.info("[CLC] Sodium slice field located on AbstractBlockRenderContext.");
        } catch (NoSuchFieldException e) {
            ComplexLeafCulling.LOGGER.warn("[CLC] Sodium slice field NOT found: {}", e.getMessage());
        }
        SLICE_FIELD = f;
    }

    // Set in @Inject, read in @Redirect — same call frame, same thread.
    private static final ThreadLocal<Boolean> SHOULD_DEMOTE =
            ThreadLocal.withInitial(() -> false);

    private static final AtomicBoolean FIRED   = new AtomicBoolean(false);
    private static final AtomicBoolean CULLED  = new AtomicBoolean(false);
    private static final AtomicBoolean DEMOTED = new AtomicBoolean(false);

    private static final ThreadLocal<BlockPos.Mutable> SCRATCH =
            ThreadLocal.withInitial(BlockPos.Mutable::new);
    private static final Direction[] DIRS = Direction.values();

    @Inject(
        method      = "renderModel",
        at          = @At("HEAD"),
        cancellable = true,
        require     = 1,
        remap       = false
    )
    private void clc$renderModel(
            BakedModel model,
            BlockState state,
            BlockPos   pos,
            BlockPos   origin,
            CallbackInfo ci) {

        SHOULD_DEMOTE.set(false);

        CLCConfig cfg = CLCConfig.INSTANCE;
        if (!cfg.enabled || !cfg.enableExtendedCulling) return;
        if (!LeafPredicate.isLeafBlock(state)) return;
        if (SLICE_FIELD == null) return;

        try {
            LevelSlice world = (LevelSlice) SLICE_FIELD.get(this);
            if (world == null) return;

            int cancelThreshold   = cfg.effectiveThreshold();
            int demotionThreshold = Math.max(1, cancelThreshold - 1);
            int neighbours        = countLeafNeighbours(world, pos);

            if (FIRED.compareAndSet(false, true)) {
                ComplexLeafCulling.LOGGER.info(
                    "[CLC] Sodium renderModel ACTIVE. cancelAt={} demoteAt={}",
                    cancelThreshold, demotionThreshold);
            }

            // TIER A — full cancel
            if (neighbours >= cancelThreshold) {
                if (CULLED.compareAndSet(false, true)) {
                    ComplexLeafCulling.LOGGER.info(
                        "[CLC] Tier-A cancel ACTIVE. First: {} at {}", state.getBlock(), pos);
                }
                ci.cancel();
                return;
            }

            // TIER B — pass demotion
            if (cfg.enablePassDemotion && neighbours >= demotionThreshold) {
                SHOULD_DEMOTE.set(true);
                if (DEMOTED.compareAndSet(false, true)) {
                    ComplexLeafCulling.LOGGER.info(
                        "[CLC] Tier-B demotion ACTIVE. First: {} at {}", state.getBlock(), pos);
                }
            }

        } catch (Exception ignored) {}
    }

    @Redirect(
        method  = "renderModel",
        at      = @At(
            value  = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/DefaultMaterials;forRenderLayer(Lnet/minecraft/class_1921;)Lnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;",
            remap  = false
        ),
        require = 0,
        remap   = false
    )
    private Material clc$redirectMaterial(RenderLayer layer) {
        if (SHOULD_DEMOTE.get()) {
            return DefaultMaterials.CUTOUT;
        }
        return DefaultMaterials.forRenderLayer(layer);
    }

    private static int countLeafNeighbours(LevelSlice world, BlockPos pos) {
        BlockPos.Mutable n = SCRATCH.get();
        int count = 0;
        for (Direction dir : DIRS) {
            n.set(pos, dir);
            try {
                if (LeafPredicate.isLeafBlock(world.getBlockState(n))) count++;
            } catch (Exception ignored) {}
        }
        return count;
    }
}
