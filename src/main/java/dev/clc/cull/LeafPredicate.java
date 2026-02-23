package dev.clc.cull;

import dev.clc.config.CLCConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;

/**
 * Determines whether a given {@link BlockState} should be treated as a
 * "leaf-type" block for Complex Leaf Culling culling purposes.
 *
 * <p>Extends detection beyond vanilla {@link LeavesBlock} to cover modded
 * leaf blocks (WWOO, Biomes O' Plenty, etc.) via the {@code minecraft:leaves}
 * block tag and an optional name-based heuristic.
 */
public final class LeafPredicate {

    private LeafPredicate() {}

    /**
     * Returns {@code true} if this block state should participate in
     * leaf-cluster culling.
     */
    public static boolean isLeafBlock(BlockState state) {
        // Tier 1: exact type check – fast path for vanilla and standard subclasses
        if (state.getBlock() instanceof LeavesBlock) {
            return true;
        }

        // Tier 2: block tag – covers modded leaves that are tagged correctly
        if (state.isIn(BlockTags.LEAVES)) {
            return true;
        }

        // Tier 3 (optional): registry-name heuristic for mods that skip the tag
        if (CLCConfig.INSTANCE.useExtendedLeafDetection) {
            return isLeafByName(state);
        }

        return false;
    }

    /**
     * Heuristic: if the block's registry path contains leaf-related substrings,
     * treat it as a leaf block.  This catches WWOO and similar packs that
     * register custom leaf blocks without the vanilla tag.
     */
    private static boolean isLeafByName(BlockState state) {
        String path = Registries.BLOCK.getId(state.getBlock()).getPath();
        return path.contains("leaf")
            || path.contains("leaves")
            || path.contains("foliage")
            || path.contains("frond")
            || path.contains("needle"); // pine/spruce-style foliage
    }
}
