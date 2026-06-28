package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.advancement.ModCriteriaTriggers;
import net.metalegend.moreswordsmod.config.ModConfig;
import net.metalegend.moreswordsmod.item.ModItemTags;
import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

// direct storm-calling staff: right-click picks a point along the player's view and
// applies vanilla lightning behavior to directly struck mobs without leaving ground fires
public class LightningStaffItem extends Item {
    public LightningStaffItem(Item.Properties properties) {
        super(properties
                .durability(config().durability)
                .repairable(ModItemTags.LIGHTNING_STAFF_REPAIR_MATERIALS)
                .enchantable(config().enchantability));
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod.lightning_staff.flavor", ChatFormatting.GRAY);
        TooltipHelper.addAbilitySection(
                builder,
                "tooltip.moreswordsmod.lightning_staff.ability_name",
                "tooltip.moreswordsmod.lightning_staff.ability_desc_1",
                "tooltip.moreswordsmod.lightning_staff.ability_desc_2",
                "tooltip.moreswordsmod.lightning_staff.ability_desc_3"
        );
        TooltipHelper.addEnchantmentSeparatorIfNeeded(stack, builder);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (user.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        return castLightningAt(stack, user, hand, new LightningTarget(target.position(), target))
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult use(Level level, Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);
        if (user.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        return castLightningAt(stack, user, hand, findLightningTarget(level, user))
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.FAIL;
    }

    private static LightningTarget findLightningTarget(Level level, Player user) {
        Vec3 from = user.getEyePosition();
        Vec3 look = user.getLookAngle();
        Vec3 to = from.add(look.scale(config().boltRange));
        BlockHitResult blockHit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, user));
        Vec3 blockedTo = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        AABB searchArea = user.getBoundingBox().expandTowards(look.scale(config().boltRange)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level,
                user,
                from,
                blockedTo,
                searchArea,
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity.isPickable(),
                config().entityRaycastMargin
        );

        if (entityHit != null) {
            return new LightningTarget(entityHit.getEntity().position(), (LivingEntity) entityHit.getEntity());
        }

        if (blockHit.getType() != HitResult.Type.MISS) {
            return new LightningTarget(blockHit.getLocation(), null);
        }

        return new LightningTarget(to, null);
    }

    private static boolean castLightningAt(ItemStack stack, Player user, InteractionHand hand, LightningTarget target) {
        if (!(user.level() instanceof ServerLevel level)) {
            return false;
        }

        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (lightning == null) {
            return false;
        }

        Vec3 position = target.position();
        lightning.snapTo(position.x, position.y, position.z, user.getYRot(), 0.0f);
        lightning.setVisualOnly(true);
        if (user instanceof ServerPlayer serverPlayer) {
            lightning.setCause(serverPlayer);
            ModCriteriaTriggers.STORMCALL.trigger(serverPlayer);
        }

        level.addFreshEntity(lightning);
        if (target.entity() != null) {
            target.entity().thunderHit(level, lightning);
        }

        stack.hurtAndBreak(config().durabilityCost, user, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        user.getCooldowns().addCooldown(stack, config().cooldownTicks);
        return true;
    }

    private static ModConfig.LightningStaff config() {
        return ModConfig.get().lightningStaff;
    }

    private record LightningTarget(Vec3 position, @Nullable LivingEntity entity) {
    }
}
