package dev.clc.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Path;

public final class CLCConfig {

    // -----------------------------------------------------------------------
    // Singleton
    // -----------------------------------------------------------------------
    public static CLCConfig INSTANCE = new CLCConfig();
    private static final Logger LOGGER = LoggerFactory.getLogger("CLC/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("complexleafculling.json");

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    /** Master switch — disabling reverts to completely vanilla behaviour. */
    public boolean enabled = true;

    /**
     * Standard leaf culling (Tier-1, face-level).
     * Marks shared faces between adjacent leaves invisible so they are skipped
     * during mesh building. Works for all leaf blocks, cooperates with Sodium.
     */
    public boolean enableStandardCulling = true;

    /**
     * Extended leaf culling (Tier-A, block-level, Sodium only).
     * Cancels rendering entirely for interior leaf blocks surrounded by other
     * leaves. Most effective for dense WWOO canopies and untagged modded leaves.
     */
    public boolean enableExtendedCulling = true;

    /**
     * Pass demotion (Tier-B, Sodium only).
     * Edge leaf blocks that partially qualify (one below the cancel threshold)
     * are demoted from TRANSLUCENT to CUTOUT rendering. Leaf shape is preserved
     * while dropping the expensive translucency pass. Threshold is automatic.
     */
    public boolean enablePassDemotion = true;

    /**
     * Culling mode — how many of a leaf's 6 neighbours must also be leaves
     * before it is treated as interior and cancelled.
     *   CONSERVATIVE  6/6 — safest visuals
     *   BALANCED      5/6 — good balance
     *   AGGRESSIVE    4/6 — maximum performance
     */
    public CullMode mode = CullMode.BALANCED;

    /**
     * Manual neighbour-count override. 0 = derive from mode. Range 1–6.
     */
    public int interiorThreshold = 0;

    /**
     * Treat blocks whose registry path contains "leaf"/"leaves"/"foliage"
     * as leaves even if not tagged minecraft:leaves. Needed for WWOO.
     */
    public boolean useExtendedLeafDetection = true;

    // -----------------------------------------------------------------------
    // Derived helpers
    // -----------------------------------------------------------------------

    public int effectiveThreshold() {
        if (interiorThreshold > 0) return Math.min(6, interiorThreshold);
        return switch (mode) {
            case CONSERVATIVE -> 6;
            case BALANCED     -> 5;
            case AGGRESSIVE   -> 4;
        };
    }

    // -----------------------------------------------------------------------
    // Persistence
    // -----------------------------------------------------------------------

    public static void load() {
        File f = CONFIG_PATH.toFile();
        if (!f.exists()) {
            INSTANCE = new CLCConfig();
            INSTANCE.save();
            LOGGER.info("Created default config at {}", CONFIG_PATH);
            return;
        }
        try (Reader r = new FileReader(f)) {
            INSTANCE = GSON.fromJson(r, CLCConfig.class);
            if (INSTANCE == null) INSTANCE = new CLCConfig();
            LOGGER.info("Loaded config from {}", CONFIG_PATH);
        } catch (IOException e) {
            LOGGER.error("Failed to load config, using defaults", e);
            INSTANCE = new CLCConfig();
        }
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, w);
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }

    // -----------------------------------------------------------------------
    // Enum
    // -----------------------------------------------------------------------

    public enum CullMode { CONSERVATIVE, BALANCED, AGGRESSIVE }
}
