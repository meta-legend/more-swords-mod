package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class WindStaffItem extends Item {
    private static final int COMBAT_COOLDOWN_TICKS = 240;
    private static final int LEAP_COOLDOWN_TICKS = 40;
    private static final int DURABILITY = 192;
    private static final float GLIDE_FALL_DISTANCE_THRESHOLD = 2.5f;
    private static final double GLIDE_VELOCITY_MULTIPLIER = 0.7;
    private static final double GLIDE_MIN_FALL_SPEED = -0.5;
    private static final double LEAP_FORWARD_STRENGTH = 1.1;
    private static final double LEAP_UPWARD_BONUS = 0.45;
    private static final double ENTITY_LAUNCH_HORIZONTAL = 0.45;
    private static final double ENTITY_LAUNCH_VERTICAL = 0.6;
    private static final double PLAYER_LAUNCH_HORIZONTAL = 0.2;
    private static final double PLAYER_LAUNCH_VERTICAL = 0.3;
    private static final int TARGET_LEVITATION_DURATION_TICKS = 20;

    public WindStaffItem(Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod.wind_staff.flavor", ChatFormatting.GRAY);
        TooltipHelper.addAbilitySection(
                builder,
                "tooltip.moreswordsmod.wind_staff.ability_name",
                "tooltip.moreswordsmod.wind_staff.ability_desc_1",
                "tooltip.moreswordsmod.wind_staff.ability_desc_2",
                "tooltip.moreswordsmod.wind_staff.ability_desc_3"
        );
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide()) {
            if (!user.getCooldowns().isOnCooldown(stack)) {
                Vec3 launchDirection = target.position().subtract(user.position());
                if (launchDirection.lengthSqr() > 1.0E-6) {
                    Vec3 normalizedDirection = launchDirection.normalize();
                    boolean isPlayerTarget = target instanceof Player;
                    double horizontalStrength = isPlayerTarget ? PLAYER_LAUNCH_HORIZONTAL : ENTITY_LAUNCH_HORIZONTAL;
                    double verticalStrength = isPlayerTarget ? PLAYER_LAUNCH_VERTICAL : ENTITY_LAUNCH_VERTICAL;

                    Vec3 launchVelocity = normalizedDirection.scale(horizontalStrength).add(0.0, verticalStrength, 0.0);
                    target.setDeltaMovement(target.getDeltaMovement().add(launchVelocity));
                    target.hurtMarked = true;
                }

                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.LEVITATION,
                        TARGET_LEVITATION_DURATION_TICKS,
                        0
                ));

                stack.hurtAndBreak(1, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                user.getCooldowns().addCooldown(stack, COMBAT_COOLDOWN_TICKS);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (!user.getCooldowns().isOnCooldown(stack)) {
            Vec3 movement = user.getDeltaMovement();
            Vec3 lookDirection = user.getLookAngle();
            Vec3 leapBoost = lookDirection.scale(LEAP_FORWARD_STRENGTH).add(0.0, LEAP_UPWARD_BONUS, 0.0);

            user.setDeltaMovement(movement.add(leapBoost));
            user.fallDistance = 0.0f;
            user.hurtMarked = true;

            if (!level.isClientSide()) {
                stack.hurtAndBreak(1, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                user.getCooldowns().addCooldown(stack, LEAP_COOLDOWN_TICKS);
            }

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (!(entity instanceof Player player)) {
            return;
        }

        if (slot != EquipmentSlot.OFFHAND && slot != EquipmentSlot.MAINHAND) {
            return;
        }

        boolean shouldGlide = !player.onGround()
                && !player.getAbilities().flying
                && player.getDeltaMovement().y < 0.0
                && player.fallDistance > GLIDE_FALL_DISTANCE_THRESHOLD;

        if (shouldGlide) {
            Vec3 velocity = player.getDeltaMovement();
            double adjustedFallSpeed = Math.max(velocity.y * GLIDE_VELOCITY_MULTIPLIER, GLIDE_MIN_FALL_SPEED);
            player.setDeltaMovement(velocity.x, adjustedFallSpeed, velocity.z);
            player.fallDistance = 0.0f;
            player.hurtMarked = true;
        }
    }
}
