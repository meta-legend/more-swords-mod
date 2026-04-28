package net.metalegend.moreswordsmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.metalegend.moreswordsmod.entity.ModEntities;
import net.metalegend.moreswordsmod.entity.custom.ShieldTestDummyEntity;
import net.metalegend.moreswordsmod.item.custom.BoneScytheItem;
import net.metalegend.moreswordsmod.item.custom.KatanaItem;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class DebugCommands {
    private DebugCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register(DebugCommands::registerCommands);
    }

    private static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            Commands.CommandSelection environment
    ) {
        dispatcher.register(
                Commands.literal("moreswords_debug")
                        .then(
                                Commands.literal("katana")
                                        .then(
                                                Commands.literal("armShieldPierce")
                                                        .executes(commandContext -> {
                                                            ServerPlayer player = commandContext.getSource().getPlayerOrException();
                                                            ItemStack stack = player.getMainHandItem();
                                                            if (!(stack.getItem() instanceof KatanaItem)) {
                                                                commandContext.getSource().sendFailure(Component.literal("Hold a katana in your main hand first."));
                                                                return 0;
                                                            }

                                                            KatanaItem.armDebugShieldPierce(stack, player.level().getGameTime());
                                                            commandContext.getSource().sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Armed the next Sheath Strike to simulate the shield-pierce branch. "
                                                                                    + "Use it on any target in singleplayer to test the special sound and behavior."
                                                                    ),
                                                                    false
                                                            );
                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                Commands.literal("dummy")
                                        .then(
                                                Commands.literal("spawnShieldDummy")
                                                        .executes(commandContext -> {
                                                            ServerPlayer player = commandContext.getSource().getPlayerOrException();
                                                            ServerLevel level = (ServerLevel) player.level();
                                                            ShieldTestDummyEntity dummy = ModEntities.SHIELD_TEST_DUMMY.create(level, EntitySpawnReason.COMMAND);
                                                            if (dummy == null) {
                                                                commandContext.getSource().sendFailure(Component.literal("Failed to create shield test dummy."));
                                                                return 0;
                                                            }

                                                            Vec3 look = player.getLookAngle();
                                                            Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
                                                            if (horizontalLook.lengthSqr() < 1.0E-6) {
                                                                horizontalLook = Vec3.directionFromRotation(0.0f, player.getYRot());
                                                            } else {
                                                                horizontalLook = horizontalLook.normalize();
                                                            }

                                                            Vec3 spawnPos = player.position().add(horizontalLook.scale(2.5));
                                                            dummy.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
                                                            dummy.setYRot(player.getYRot() + 180.0f);
                                                            dummy.setYHeadRot(player.getYRot() + 180.0f);
                                                            dummy.setYBodyRot(player.getYRot() + 180.0f);
                                                            dummy.setCustomName(Component.literal("Shield Test Dummy"));
                                                            dummy.setCustomNameVisible(true);
                                                            level.addFreshEntity(dummy);
                                                            commandContext.getSource().sendSuccess(
                                                                    () -> Component.literal(
                                                                            "Spawned a shield test dummy. It auto-blocks with a shield and re-raises it after disable."
                                                                    ),
                                                                    false
                                                            );
                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                Commands.literal("scythe")
                                        .then(
                                                Commands.literal("giveSoulCharges")
                                                        .executes(commandContext -> grantSoulCharges(commandContext.getSource(), 10))
                                                        .then(
                                                                Commands.argument("amount", IntegerArgumentType.integer(0, 10))
                                                                        .executes(commandContext -> grantSoulCharges(
                                                                                commandContext.getSource(),
                                                                                IntegerArgumentType.getInteger(commandContext, "amount")
                                                                        ))
                                                        )
                                        )
                        )
        );
    }

    private static int grantSoulCharges(CommandSourceStack source, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BoneScytheItem)) {
            source.sendFailure(Component.literal("Hold the Bone Scythe in your main hand first."));
            return 0;
        }

        int newTotal = BoneScytheItem.debugGrantSoulCharges(stack, amount);
        source.sendSuccess(
                () -> Component.literal("Granted " + amount + " Soul Charges. Current scythe charges: " + newTotal + "/10"),
                false
        );
        return 1;
    }
}
