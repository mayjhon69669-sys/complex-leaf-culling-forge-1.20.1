package dev.clc.cull;

import dev.clc.config.CLCConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-chunk-section cache of "interior" leaf block positions.
 *
 * Thread safety: the cache map is a {@link ConcurrentHashMap}.  The
 * per-section Set is computed atomically via {@code computeIfAbsent} and
 * never mutated after insertion, so reads are lock-free.
 */
public final class SectionInteriorCache {

    private static final ConcurrentHashMap<Long, Set<Long>> CACHE =
            new ConcurrentHashMap<>(256);

    private SectionInteriorCache() {}

    public static void invalidateSection(ChunkSectionPos pos) {
        CACHE.remove(pos.asLong());
    }

    public static void invalidateAll() {
        CACHE.clear();
    }

    public static boolean isInterior(BlockPos pos) {
        CLCConfig cfg = CLCConfig.INSTANCE;
        if (!cfg.enableExtendedCulling) return false;

        long sectionKey = ChunkSectionPos.from(pos).asLong();
        Set<Long> interior = CACHE.get(sectionKey);
        if (interior == null) return false;

        return interior.contains(pos.asLong());
    }

    public static void analyzeSection(BlockRenderView world, ChunkSectionPos sectionPos) {
        long key = sectionPos.asLong();
        CACHE.computeIfAbsent(key, k -> computeInterior(world, sectionPos));
    }

    private static final Direction[] DIRECTIONS = Direction.values();

    private static Set<Long> computeInterior(BlockRenderView world, ChunkSectionPos section) {
        CLCConfig cfg = CLCConfig.INSTANCE;
        int threshold = cfg.effectiveThreshold();

        int minX = section.getMinX();
        int minY = section.getMinY();
        int minZ = section.getMinZ();

        Set<Long> result = new HashSet<>();
        BlockPos.Mutable pos      = new BlockPos.Mutable();
        BlockPos.Mutable neighPos = new BlockPos.Mutable();

        for (int lx = 0; lx < 16; lx++) {
            for (int ly = 0; ly < 16; ly++) {
                for (int lz = 0; lz < 16; lz++) {
                    pos.set(minX + lx, minY + ly, minZ + lz);

                    if (!LeafPredicate.isLeafBlock(safeGetState(world, pos))) continue;

                    int leafNeighbours = 0;
                    for (Direction dir : DIRECTIONS) {
                        neighPos.set(pos).move(dir);
                        if (LeafPredicate.isLeafBlock(safeGetState(world, neighPos))) {
                            leafNeighbours++;
                        }
                    }

                    if (leafNeighbours >= threshold) {
                        result.add(pos.asLong());
                    }
                }
            }
        }

        return result.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(result);
    }

    private static net.minecraft.block.BlockState safeGetState(BlockRenderView world, BlockPos pos) {
        try {
            return world.getBlockState(pos);
        } catch (Exception e) {
            return net.minecraft.block.Blocks.AIR.getDefaultState();
        }
    }
}
