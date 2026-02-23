package dev.clc.mixin;

import dev.clc.ComplexLeafCulling;
import dev.clc.config.CLCConfig;
import dev.clc.cull.LeafPredicate;
import dev.clc.cull.SectionInteriorCache;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tier-2 culling for the vanilla renderer (active when Sodium is NOT installed).
 * Uses the SectionInteriorCache to skip fully-interior leaf blocks.
 * require=0 so a missing/changed method signature doesn't crash the game.
 */
@Mixin(BlockModelRenderer.class)
public class MixinBlockModelRenderer {

    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void clc$render(
            BlockRenderView world,
            BakedModel      model,
            BlockState      state,
            BlockPos        pos,
            MatrixStack     matrices,
            VertexConsumer  vertexConsumer,
            boolean         cull,
            Random          random,
            long            seed,
            int             overlay,
            CallbackInfo    ci)
    {
        try {
            CLCConfig cfg = CLCConfig.INSTANCE;
            if (!cfg.enabled || !cfg.enableStandardCulling) return;
            if (world == null || pos == null || state == null) return;
            if (!LeafPredicate.isLeafBlock(state)) return;

            if (LOGGED.compareAndSet(false, true)) {
                ComplexLeafCulling.LOGGER.info("[CLC] Tier-2 vanilla BlockModelRenderer firing.");
            }

            if (cfg.enableExtendedCulling) {
                SectionInteriorCache.analyzeSection(world, ChunkSectionPos.from(pos));
                if (SectionInteriorCache.isInterior(pos)) {
                    ci.cancel();
                }
            }
        } catch (Exception e) {
            if (LOGGED.compareAndSet(false, true)) {
                ComplexLeafCulling.LOGGER.warn("[CLC] Exception in render mixin: {}", e.toString());
            }
        }
    }
}
