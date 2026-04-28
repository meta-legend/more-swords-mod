package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

// single-target static buildup weapon
// repeated hits on the same target within the decay window increase damage until the
// staff discharges into a chain-lightning burst
public class LightningStaffItem extends Item {
    private static final int COOLDOWN_TICKS = 12;
    private static final int DURABILITY = 128;
    private static final int STACK_DECAY_TICKS = 60;
    private static final int MAX_STATIC_STACKS = 5;
    private static final float BASE_STATIC_DAMAGE = 3.0f;
    private static final float BONUS_DAMAGE_PER_STACK = 1.0f;
    private static final float CHAIN_LIGHTNING_DAMAGE = 5.0f;
    private static final double CHAIN_RANGE = 6.0;
    private static final int MAX_CHAIN_TARGETS = 3;
    private static final Map<UUID, StaticBuildupState> STATIC_BUILDUP = new HashMap<>();

    public LightningStaffItem(Item.Properties properties) {
        super(properties.durability(DURABILITY));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod.lightning_staff.flavor", ChatFormatting.GRAY);
        TooltipHelper.addAbilitySection(
                builder,
                "tooltip.moreswordsmod.lightning_staff.ability_name",
                "tooltip.moreswordsmod.lightning_staff.ability_desc_1",
                "tooltip.moreswordsmod.lightning_staff.ability_desc_2"
        );
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!(user.level() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }

        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        int newStacks = updateStaticBuildup(user, target, level.getGameTime());
        float staticDamage = BASE_STATIC_DAMAGE + (newStacks - 1) * BONUS_DAMAGE_PER_STACK;
        target.hurt(user.damageSources().indirectMagic(user, user), staticDamage);

        if (newStacks >= MAX_STATIC_STACKS) {
            triggerChainLightning(level, user, target);
            clearStaticBuildup(user);
        } else {
            spawnStaticParticles(level, target);
        }

        stack.hurtAndBreak(1, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        user.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        return InteractionResult.SUCCESS_SERVER;
    }

    // buildup is tracked per player so swapping targets or waiting too long resets the chain
    private static int updateStaticBuildup(Player user, LivingEntity target, long gameTime) {
        StaticBuildupState state = STATIC_BUILDUP.get(user.getUUID());
        boolean sameTarget = state != null && state.targetId.equals(target.getUUID());
        boolean buildupActive = state != null && sameTarget && gameTime <= state.expireTick;
        int currentStacks = state != null ? state.stacks : 0;
        int newStacks = buildupActive ? Math.min(MAX_STATIC_STACKS, currentStacks + 1) : 1;
        STATIC_BUILDUP.put(user.getUUID(), new StaticBuildupState(target.getUUID(), newStacks, gameTime + STACK_DECAY_TICKS));
        return newStacks;
    }

    private static void clearStaticBuildup(Player user) {
        STATIC_BUILDUP.remove(user.getUUID());
    }

    // the primary target gets a real lightning strike while chained targets get visual-only bolts
    // rain doubles the search radius to make wet-weather fights feel stronger without changing base damage
    private static void triggerChainLightning(ServerLevel level, Player user, LivingEntity primaryTarget) {
        summonLightning(level, primaryTarget);
        spawnStaticParticles(level, primaryTarget);

        double range = level.isRainingAt(primaryTarget.blockPosition()) ? CHAIN_RANGE * 2.0 : CHAIN_RANGE;
        AABB searchBox = primaryTarget.getBoundingBox().inflate(range);
        List<LivingEntity> nearbyTargets = level.getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                entity -> entity != user && entity != primaryTarget && entity.isAlive()
        );

        nearbyTargets.stream()
                .sorted(Comparator.comparingDouble(primaryTarget::distanceToSqr))
                .limit(MAX_CHAIN_TARGETS)
                .forEach(target -> {
                    target.hurt(user.damageSources().indirectMagic(user, user), CHAIN_LIGHTNING_DAMAGE);
                    summonVisualLightning(level, target);
                    spawnStaticParticles(level, target);
                });
    }

    private static void summonLightning(ServerLevel level, LivingEntity target) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            lightning.snapTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            level.addFreshEntity(lightning);
        }
    }

    // visual-only bolts sell the chain effect without spawning extra damaging lightning entities
    private static void summonVisualLightning(ServerLevel level, LivingEntity target) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning != null) {
            lightning.snapTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
            lightning.setVisualOnly(true);
            level.addFreshEntity(lightning);
        }
    }

    private static void spawnStaticParticles(ServerLevel level, LivingEntity target) {
        level.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                10,
                0.3,
                0.4,
                0.3,
                0.02
        );
    }

    private record StaticBuildupState(UUID targetId, int stacks, long expireTick) {
    }
}
