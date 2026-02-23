package dev.clc;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.clc.config.CLCConfig;
import dev.clc.cull.SectionInteriorCache;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

/**
 * Client-side initialiser.
 *
 * Registers the /clc command for runtime configuration:
 *   /clc reload
 *   /clc status
 *   /clc set enabled true
 *   /clc set mode BALANCED
 *   /clc set threshold 5
 *   /clc set standardCulling true
 *   /clc set extendedCulling true
 *   /clc set passDemotion true
 *   /clc set extendedDetect true
 */
public class ComplexLeafCullingClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        registerCommands();
        ComplexLeafCulling.LOGGER.info("Complex Leaf Culling client ready.");
    }

    private void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                ClientCommandManager.literal("clc")

                    .then(ClientCommandManager.literal("reload")
                        .executes(ctx -> {
                            CLCConfig.load();
                            SectionInteriorCache.invalidateAll();
                            ctx.getSource().sendFeedback(
                                Text.literal("Complex Leaf Culling config reloaded."));
                            return 1;
                        }))

                    .then(ClientCommandManager.literal("status")
                        .executes(ctx -> {
                            CLCConfig c = CLCConfig.INSTANCE;
                            ctx.getSource().sendFeedback(Text.literal(String.format(
                                "CLC: enabled=%s standard=%s extended=%s demotion=%s mode=%s threshold=%d",
                                c.enabled, c.enableStandardCulling, c.enableExtendedCulling,
                                c.enablePassDemotion, c.mode, c.effectiveThreshold())));
                            return 1;
                        }))

                    .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("key", StringArgumentType.word())
                            .then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    String key   = StringArgumentType.getString(ctx, "key");
                                    String value = StringArgumentType.getString(ctx, "value");
                                    return applyConfig(ctx, key, value);
                                }))))
            )
        );
    }

    private static int applyConfig(
            com.mojang.brigadier.context.CommandContext<
                net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource> ctx,
            String key, String value)
    {
        CLCConfig cfg = CLCConfig.INSTANCE;
        try {
            switch (key.toLowerCase()) {
                case "enabled"                                    -> cfg.enabled                  = Boolean.parseBoolean(value);
                case "mode"                                       -> cfg.mode                     = CLCConfig.CullMode.valueOf(value.toUpperCase());
                case "threshold"                                  -> cfg.interiorThreshold        = Integer.parseInt(value);
                case "standardculling", "standard"                -> cfg.enableStandardCulling    = Boolean.parseBoolean(value);
                case "extendedculling", "extended"                -> cfg.enableExtendedCulling    = Boolean.parseBoolean(value);
                case "passdemotion", "demotion"                   -> cfg.enablePassDemotion       = Boolean.parseBoolean(value);
                case "extendeddetect", "useextendedleafdetection" -> cfg.useExtendedLeafDetection = Boolean.parseBoolean(value);
                default -> {
                    ctx.getSource().sendFeedback(Text.literal("Unknown key: " + key));
                    return 0;
                }
            }
            cfg.save();
            SectionInteriorCache.invalidateAll();
            ctx.getSource().sendFeedback(Text.literal("Set " + key + " = " + value));
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFeedback(Text.literal("Bad value for " + key + ": " + value));
            return 0;
        }
    }
}
