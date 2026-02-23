package dev.clc.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers Complex Leaf Culling in ModMenu's mod list and hooks up the
 * config screen button.  If ModMenu is not installed, this class
 * is never loaded (it is listed as an optional entrypoint).
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new CLCConfigScreen(parent);
    }
}
