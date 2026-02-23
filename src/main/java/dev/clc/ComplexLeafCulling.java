package dev.clc;

import dev.clc.config.CLCConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server-side (and common) initialiser for Complex Leaf Culling.
 * All actual logic is client-side; this class just loads the config.
 */
public class ComplexLeafCulling implements ModInitializer {

    public static final String MOD_ID = "complexleafculling";
    public static final Logger LOGGER = LoggerFactory.getLogger("ComplexLeafCulling");

    @Override
    public void onInitialize() {
        CLCConfig.load();
        LOGGER.info("Complex Leaf Culling initialised (mode={}, threshold={}).",
                CLCConfig.INSTANCE.mode,
                CLCConfig.INSTANCE.effectiveThreshold());
    }
}
