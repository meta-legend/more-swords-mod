package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.advancement.ModCriteriaTriggers;
import net.metalegend.moreswordsmod.config.ModConfig;
import net.metalegend.moreswordsmod.item.ModItemTags;
import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.metalegend.moreswordsmod.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// mobility staff with three linked behaviors
// right click leaps the player melee use launches a target and sneaking midair enables glide control
public class WindStaffItem extends Item {
    private static final Map<UUID, MountBoostState> WIND_BOOST_MOUNT_STATES = new ConcurrentHashMap<>();
    private static final int BOOST_PROTECTION_TICKS = 14;
    private static final int BOOST_VERTICAL_PROTECTION_TICKS = 8;

    private static final float LEAP_SOUND_VOLUME = 0.75f;
    private static final float LEAP_SOUND_PITCH = 1.05f;

    public WindStaffItem(Properties properties) {
        super(properties
                .durability(config().durability)
                .repairable(ModItemTags.WIND_STAFF_REPAIR_MATERIALS)
                .enchantable(config().enchantability));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod.wind_staff.flavor", ChatFormatting.GRAY);
        builder.accept(Component.empty());
        addWindStaffAbilitySection(builder, "tooltip.moreswordsmod.wind_staff.leap_name", "tooltip.moreswordsmod.wind_staff.leap_desc_1", "tooltip.moreswordsmod.wind_staff.leap_desc_2", "tooltip.moreswordsmod.wind_staff.leap_desc_3");
        addWindStaffAbilitySection(builder, "tooltip.moreswordsmod.wind_staff.glide_name", "tooltip.moreswordsmod.wind_staff.glide_desc");
        TooltipHelper.addEnchantmentSeparatorIfNeeded(stack, builder);
    }

    private static void addWindStaffAbilitySection(Consumer<Component> builder, String titleKey, String... descriptionKeys) {
        TooltipHelper.addTooltipLine(builder, titleKey, ChatFormatting.AQUA);
        for (String descriptionKey : descriptionKeys) {
            TooltipHelper.addTooltipLine(builder, descriptionKey, ChatFormatting.DARK_AQUA);
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide()) {
            if (!user.getCooldowns().isOnCooldown(stack)) {
                // player targets get lighter horizontal and vertical push so the staff is less oppressive in pvp
                Vec3 launchDirection = target.position().subtract(user.position());
                if (launchDirection.lengthSqr() > 1.0E-6) {
                    Vec3 normalizedDirection = launchDirection.normalize();
                    boolean isPlayerTarget = target instanceof Player;
                    double horizontalStrength = isPlayerTarget ? config().playerLaunchHorizontal : config().entityLaunchHorizontal;
                    double verticalStrength = isPlayerTarget ? config().playerLaunchVertical : config().entityLaunchVertical;

                    Vec3 launchVelocity = normalizedDirection.scale(horizontalStrength).add(0.0, verticalStrength, 0.0);
                    target.setDeltaMovement(target.getDeltaMovement().add(launchVelocity));
                    target.hurtMarked = true;
                }

                target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.LEVITATION,
                        config().targetLevitationDurationTicks,
                        0
                ));

                stack.hurtAndBreak(config().combatDurabilityCost, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                user.getCooldowns().addCooldown(stack, config().combatCooldownTicks);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        Entity vehicle = user.getVehicle();
        if (vehicle != null) {
            Vec3 look = user.getLookAngle();
            Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
            Vec3 boostDirection = horizontalLook.lengthSqr() > 1.0E-6
                    ? horizontalLook.normalize()
                    : Vec3.directionFromRotation(0.0f, user.getYRot());

            beginMountBoost(vehicle, boostDirection, level.getGameTime());
            user.fallDistance = 0.0f;

            if (!level.isClientSide()) {
                stack.hurtAndBreak(config().leapDurabilityCost, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                user.getCooldowns().addCooldown(stack, config().leapCooldownTicks);
                level.playSound(null, vehicle.blockPosition(), ModSounds.WIND_STAFF_WIND_LEAP, user.getSoundSource(), LEAP_SOUND_VOLUME, LEAP_SOUND_PITCH);
            }

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        // Standard Wind Leap — uses look direction so the staff converts aim into movement.
        Vec3 leapBoost = user.getLookAngle().scale(config().leapForwardStrength).add(0.0, config().leapUpwardBonus, 0.0);
        user.setDeltaMovement(user.getDeltaMovement().add(leapBoost));
        user.fallDistance = 0.0f;
        user.hurtMarked = true;

        if (!level.isClientSide()) {
            stack.hurtAndBreak(config().leapDurabilityCost, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            user.getCooldowns().addCooldown(stack, config().leapCooldownTicks);
            level.playSound(null, user.blockPosition(), ModSounds.WIND_STAFF_WIND_LEAP, user.getSoundSource(), LEAP_SOUND_VOLUME, LEAP_SOUND_PITCH);
        }

        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private static void beginMountBoost(Entity vehicle, Vec3 boostDirection, long gameTime) {
        if (vehicle.onGround()) {
            vehicle.absSnapTo(vehicle.getX(), vehicle.getY() + config().mountBoostAirLift, vehicle.getZ(), vehicle.getYRot(), vehicle.getXRot());
        }

        Vec3 horizontalImpulse = boostDirection.scale(config().mountBoostHorizontal);
        vehicle.setDeltaMovement(vehicle.getDeltaMovement().add(horizontalImpulse).add(0.0, config().mountBoostVertical, 0.0));
        vehicle.setOnGround(false);
        vehicle.hurtMarked = true;
        vehicle.fallDistance = 0.0f;

        if (vehicle instanceof LivingEntity) {
            WIND_BOOST_MOUNT_STATES.put(
                    vehicle.getUUID(),
                    new MountBoostState(
                            gameTime,
                            gameTime + config().mountBoostProtectionTicks,
                            boostDirection.x,
                            boostDirection.z,
                            config().mountBoostHorizontal,
                            config().mountBoostVertical * 0.85
                    )
            );
        }
    }

    public static boolean forceMountBoostAirborne(LivingEntity entity) {
        MountBoostState state = getActiveMountBoostState(entity);
        if (state == null) {
            return false;
        }

        entity.setOnGround(false);
        return true;
    }

    public static void stabilizeMountBoostVelocity(LivingEntity entity) {
        MountBoostState state = getActiveMountBoostState(entity);
        if (state == null) {
            return;
        }

        long gameTime = entity.level().getGameTime();
        long elapsedTicks = Math.max(0L, gameTime - state.startTick());
        Vec3 velocity = entity.getDeltaMovement();
        double projection = velocity.x * state.directionX() + velocity.z * state.directionZ();
        double protectedSpeed = state.protectedSpeed(gameTime);
        double x = velocity.x;
        double y = velocity.y;
        double z = velocity.z;

        if (projection < protectedSpeed) {
            double missingSpeed = protectedSpeed - projection;
            x += state.directionX() * missingSpeed;
            z += state.directionZ() * missingSpeed;
        }

        if (elapsedTicks < config().mountBoostVerticalProtectionTicks) {
            double protectedVerticalSpeed = state.protectedVerticalSpeed(gameTime);
            if (y < protectedVerticalSpeed) {
                y = protectedVerticalSpeed;
            }
        }

        if (x != velocity.x || y != velocity.y || z != velocity.z) {
            entity.setDeltaMovement(x, y, z);
            entity.hurtMarked = true;
        }
    }

    private static MountBoostState getActiveMountBoostState(LivingEntity entity) {
        MountBoostState state = WIND_BOOST_MOUNT_STATES.get(entity.getUUID());
        if (state == null) {
            return null;
        }

        if (entity.level().getGameTime() >= state.expiryTick()) {
            WIND_BOOST_MOUNT_STATES.remove(entity.getUUID());
            return null;
        }

        return state;
    }

    private static ModConfig.WindStaff config() {
        return ModConfig.get().windStaff;
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

        if (slot == EquipmentSlot.OFFHAND && player.getMainHandItem().getItem() instanceof WindStaffItem) {
            return;
        }

        boolean shouldGlide = !player.onGround()
                && player.isShiftKeyDown()
                && !player.getAbilities().flying
                && player.getDeltaMovement().y < 0.0
                && !player.isFallFlying()
                && !player.isInWater();

        if (shouldGlide) {
            // gale glide only clamps downward speed so horizontal momentum stays under normal player control
            if (player instanceof ServerPlayer serverPlayer && player.fallDistance >= config().advancementGlideFallDistance) {
                ModCriteriaTriggers.GALE_GLIDE.trigger(serverPlayer);
            }

            Vec3 velocity = player.getDeltaMovement();
            double adjustedFallSpeed = Math.max(velocity.y, config().glideMaxFallSpeed);
            player.setDeltaMovement(velocity.x, adjustedFallSpeed, velocity.z);
            player.fallDistance = 0.0f;
            player.hurtMarked = true;

        }
    }

    private record MountBoostState(
            long startTick,
            long expiryTick,
            double directionX,
            double directionZ,
            double startSpeed,
            double startVerticalSpeed
    ) {
        private double protectedSpeed(long gameTime) {
            long elapsedTicks = Math.max(0L, gameTime - startTick);
            return Math.max(
                    config().mountBoostMinProtectedSpeed,
                    startSpeed * Math.pow(config().mountBoostSpeedDecay, elapsedTicks)
            );
        }

        private double protectedVerticalSpeed(long gameTime) {
            long elapsedTicks = Math.max(0L, gameTime - startTick);
            return Math.max(
                    config().mountBoostMinProtectedVerticalSpeed,
                    startVerticalSpeed * Math.pow(config().mountBoostVerticalSpeedDecay, elapsedTicks)
            );
        }
    }
}
