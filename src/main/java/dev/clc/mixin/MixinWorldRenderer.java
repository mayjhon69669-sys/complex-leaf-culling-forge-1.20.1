package dev.clc.mixin;

import dev.clc.cull.SectionInteriorCache;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.ChunkSectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidates the {@link SectionInteriorCache} whenever Minecraft schedules
 * a chunk section for rebuild.  Without this, stale interior data would
 * persist after block changes (tree chopping, leaf decay, etc.).
 */
@Mixin(WorldRenderer.class)
public class MixinWorldRenderer {

    @Inject(method = "scheduleChunkRender", at = @At("HEAD"))
    private void clc$scheduleChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        SectionInteriorCache.invalidateSection(ChunkSectionPos.from(x, y, z));
    }

    @Inject(method = "reload()V", at = @At("HEAD"))
    private void clc$reload(CallbackInfo ci) {
        SectionInteriorCache.invalidateAll();
    }
}
