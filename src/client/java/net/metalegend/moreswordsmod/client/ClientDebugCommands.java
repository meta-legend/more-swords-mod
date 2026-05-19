package net.metalegend.moreswordsmod.client;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public final class ClientDebugCommands {
    private ClientDebugCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(ClientDebugCommands::registerCommands);
    }

    private static void registerCommands(
            CommandDispatcher<FabricClientCommandSource> dispatcher,
            CommandBuildContext context
    ) {
        dispatcher.register(
                ClientCommands.literal("moreswords_client")
                        .then(
                                ClientCommands.literal("katanaPose")
                                        .then(ClientCommands.literal("show").executes(commandContext -> showPose(commandContext.getSource())))
                                        .then(ClientCommands.literal("reset").executes(commandContext -> resetPose(commandContext.getSource())))
                                        .then(ClientCommands.literal("trigger").executes(commandContext -> triggerPose(commandContext.getSource())))
                                        .then(ClientCommands.literal("toggleEndValues").executes(commandContext -> toggleEndValues(commandContext.getSource())))
                                        .then(
                                                ClientCommands.literal("useEndValues")
                                                        .then(
                                                                ClientCommands.argument("enabled", BoolArgumentType.bool())
                                                                        .executes(commandContext -> setUseEndValues(
                                                                                commandContext.getSource(),
                                                                                BoolArgumentType.getBool(commandContext, "enabled")
                                                                        ))
                                                        )
                                        )
                                        .then(
                                                ClientCommands.literal("set")
                                                        .then(
                                                                ClientCommands.argument("field", StringArgumentType.word())
                                                                        .suggests((commandContext, suggestionsBuilder) ->
                                                                                SharedSuggestionProvider.suggest(KatanaSheathStrikeAnimation.getScreenPoseFieldNames(), suggestionsBuilder))
                                                                        .then(
                                                                                ClientCommands.argument("value", FloatArgumentType.floatArg())
                                                                                        .executes(commandContext -> setPoseValue(
                                                                                                commandContext.getSource(),
                                                                                                StringArgumentType.getString(commandContext, "field"),
                                                                                                FloatArgumentType.getFloat(commandContext, "value")
                                                                                        ))
                                                                        )
                                                        )
                                        )
                        )
        );
    }

    private static int showPose(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(KatanaSheathStrikeAnimation.getScreenPoseTuningSummary()));
        return 1;
    }

    private static int resetPose(FabricClientCommandSource source) {
        KatanaSheathStrikeAnimation.resetScreenPoseTuning();
        source.sendFeedback(Component.literal("Reset katana pose tuning: " + KatanaSheathStrikeAnimation.getScreenPoseTuningSummary()));
        return 1;
    }

    private static int triggerPose(FabricClientCommandSource source) {
        KatanaSheathStrikeAnimation.start(source.getPlayer().getId());
        source.sendFeedback(Component.literal("Triggered local katana dash animation"));
        return 1;
    }

    private static int toggleEndValues(FabricClientCommandSource source) {
        boolean enabled = KatanaSheathStrikeAnimation.toggleUseEndPoseValues();
        source.sendFeedback(Component.literal("Katana end pose values enabled: " + enabled));
        return 1;
    }

    private static int setUseEndValues(FabricClientCommandSource source, boolean enabled) {
        KatanaSheathStrikeAnimation.setUseEndPoseValues(enabled);
        source.sendFeedback(Component.literal("Katana end pose values enabled: " + enabled));
        return 1;
    }

    private static int setPoseValue(FabricClientCommandSource source, String field, float value) {
        if (!KatanaSheathStrikeAnimation.setScreenPoseValue(field, value)) {
            source.sendError(Component.literal("Unknown katana pose field: " + field));
            return 0;
        }

        source.sendFeedback(Component.literal("Set " + field + " to " + value));
        return 1;
    }
}
