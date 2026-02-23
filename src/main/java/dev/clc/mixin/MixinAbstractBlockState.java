package dev.clc.mixin;

import dev.clc.ComplexLeafCulling;
import dev.clc.config.CLCConfig;
import dev.clc.cull.LeafPredicate;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tier-1 culling: mark faces between adjacent leaf-type blocks as invisible.
 *
 * Sodium respects this via BlockOcclusionCache for solid+cutout render layers.
 * When a block has all 6 faces invisible, Sodium emits zero quads for it —
 * effectively the same result as the block-skip in Tier 2/3.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class MixinAbstractBlockState {

    private static final AtomicBoolean LOGGED = new AtomicBoolean(false);

    @Inject(
        method = "isSideInvisible",
        at     = @At("HEAD"),
        cancellable = true
    )
    private void clc$isSideInvisible(
            BlockState adjacentState,
            Direction  direction,
            CallbackInfoReturnable<Boolean> cir)
    {
        CLCConfig cfg = CLCConfig.INSTANCE;
        if (!cfg.enabled || !cfg.enableStandardCulling) return;

        BlockState thisState = (BlockState)(Object) this;

        if (LeafPredicate.isLeafBlock(thisState) && LeafPredicate.isLeafBlock(adjacentState)) {
            if (LOGGED.compareAndSet(false, true)) {
                ComplexLeafCulling.LOGGER.info(
                    "[CLC] Tier-1 isSideInvisible firing. " +
                    "this={} adjacent={}",
                    thisState.getBlock(), adjacentState.getBlock());
            }
            cir.setReturnValue(true);
        }
    }
}
