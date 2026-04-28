package net.metalegend.moreswordsmod.item.custom;

import net.metalegend.moreswordsmod.item.TooltipHelper;
import net.metalegend.moreswordsmod.soul.BoneScytheSoulProfile;
import net.metalegend.moreswordsmod.soul.ReapedSoulImprint;
import net.metalegend.moreswordsmod.soul.ReapedSoulManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// bone scythe state machine
// the stack owns the long-lived resource state: charges, stored imprints, current selection,
// and any active summon window while spawned allies are tracked separately in ReapedSoulManager
public class BoneScytheItem extends Item {
    private static final String SOUL_CHARGES_KEY = "SoulCharges";
    private static final String SOUL_IMPRINTS_KEY = "SoulImprints";
    private static final String SELECTED_IMPRINT_INDEX_KEY = "SelectedImprintIndex";
    private static final String SUMMON_WINDOW_END_TICK_KEY = "SummonWindowEndTick";
    private static final String SUMMON_WEIGHT_REMAINING_KEY = "SummonWeightRemaining";
    private static final String OVERLAY_SUPPRESS_UNTIL_TICK_KEY = "OverlaySuppressUntilTick";
    private static final String LAST_SUMMON_WINDOW_TICK_SOUND_SECOND_KEY = "LastSummonWindowTickSoundSecond";
    private static final int DURABILITY = 512;
    private static final int ENCHANTABILITY = 14;
    private static final int SOUL_CHARGE_CAP = 10;
    private static final int SOUL_IMPRINT_CAP = 8;
    private static final int SUMMON_WINDOW_TICKS = 200;
    private static final int SUMMON_COOLDOWN_TICKS = 240;
    private static final int SUMMON_DURABILITY_COST = 12;
    private static final int MAX_ACTIVE_SUMMONS = 3;
    private static final float SUMMON_WINDOW_OPEN_VOLUME = 0.8f;
    private static final float SUMMON_WINDOW_OPEN_PITCH = 0.95f;
    private static final float SUMMON_WINDOW_CLOSE_VOLUME = 0.8f;
    private static final float SUMMON_WINDOW_CLOSE_PITCH = 1.05f;
    private static final float SUMMON_CAST_VOLUME = 0.9f;
    private static final float SUMMON_CAST_PITCH = 0.85f;
    private static final float SUMMON_WINDOW_TICK_VOLUME = 0.18f;
    private static final float SUMMON_WINDOW_TICK_PITCH = 0.95f;
    private static final float SOUL_SELECTION_VOLUME = 0.45f;
    private static final float RECALL_VOLUME = 0.7f;
    private static final float RECALL_PITCH = 0.9f;
    private static final int PRIORITY_OVERLAY_TICKS = 30;
    private static final int SELECTION_OVERLAY_TICKS = 40;

    public BoneScytheItem(Properties properties) {
        super(properties
                .durability(DURABILITY)
                .enchantable(ENCHANTABILITY)
                .attributes(
                        ItemAttributeModifiers.builder()
                                .add(
                                        Attributes.ATTACK_DAMAGE,
                                        new AttributeModifier(
                                                Identifier.withDefaultNamespace("base_attack_damage"),
                                                4,
                                                AttributeModifier.Operation.ADD_VALUE
                                        ),
                                        EquipmentSlotGroup.MAINHAND
                                )
                                .add(
                                        Attributes.ATTACK_SPEED,
                                        new AttributeModifier(
                                                Identifier.withDefaultNamespace("base_attack_speed"),
                                                -2.6f,
                                                AttributeModifier.Operation.ADD_VALUE
                                        ),
                                        EquipmentSlotGroup.MAINHAND
                                )
                                .build()
                ));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> builder,
            TooltipFlag tooltipFlag
    ) {
        TooltipHelper.addTooltipLine(builder, "tooltip.moreswordsmod.bone_scythe.flavor", ChatFormatting.GRAY);
        TooltipHelper.addAbilitySection(
                builder,
                "tooltip.moreswordsmod.bone_scythe.ability_name",
                "tooltip.moreswordsmod.bone_scythe.ability_desc_1",
                "tooltip.moreswordsmod.bone_scythe.ability_desc_2",
                "tooltip.moreswordsmod.bone_scythe.ability_desc_3",
                "tooltip.moreswordsmod.bone_scythe.ability_desc_4",
                "tooltip.moreswordsmod.bone_scythe.ability_desc_5",
                "tooltip.moreswordsmod.bone_scythe.ability_desc_6"
        );

        builder.accept(Component.literal("Soul Charges: " + getSoulChargeCount(stack) + "/" + SOUL_CHARGE_CAP).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.literal("Stored Imprints: " + getImprints(stack, context.registries()).size() + "/" + SOUL_IMPRINT_CAP).withStyle(ChatFormatting.DARK_AQUA));
        builder.accept(Component.literal("Summon Weight: " + getStoredSummonWeight(stack) + "/" + SOUL_CHARGE_CAP).withStyle(ChatFormatting.BLUE));

        ReapedSoulImprint selectedImprint = getSelectedImprint(stack, context.registries());
        if (selectedImprint != null) {
            builder.accept(Component.literal("Selected: ").withStyle(ChatFormatting.GRAY).append(selectedImprint.profile().summonName().copy().withStyle(ChatFormatting.DARK_AQUA)));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            return;
        }

        long gameTime = level.getGameTime();
        int remainingWeight = getSummonWeightRemaining(stack, gameTime);
        if (remainingWeight > 0) {
            playSummonWindowTickSoundIfNeeded(player, stack, gameTime);
        }

        if (remainingWeight > 0 && !isPriorityOverlayActive(stack, gameTime)) {
            long remainingTicks = Math.max(0L, getSummonWindowEndTick(stack) - gameTime);
            int wholeSeconds = (int) (remainingTicks / 20L);
            int tenths = (int) ((remainingTicks % 20L) / 2L);
            player.sendOverlayMessage(
                    Component.literal(
                            "Summon Window: "
                                    + wholeSeconds
                                    + "."
                                    + tenths
                                    + "s | Weight: "
                                    + remainingWeight
                                    + " | Active: "
                                    + ReapedSoulManager.countActiveSummons(player)
                                    + "/"
                                    + MAX_ACTIVE_SUMMONS
                    ).withStyle(ChatFormatting.AQUA)
            );
        }

        if (clearExpiredSummonWindow(stack, gameTime)) {
            playSummonWindowCloseSound(level, player);
            player.getCooldowns().addCooldown(stack, SUMMON_COOLDOWN_TICKS);
        }
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);

        if (shouldApplyWither(attacker)) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
        }

        if (attacker.level() instanceof ServerLevel serverLevel && target.isDeadOrDying()) {
            harvestSoul(serverLevel, stack, attacker, target);
        }
    }

    @Override
    public InteractionResult use(Level level, net.minecraft.world.entity.player.Player user, InteractionHand hand) {
        ItemStack stack = user.getItemInHand(hand);

        if (user.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                cycleSelectedImprint((ServerPlayer) user, stack);
            }
            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        return handleUse((ServerLevel) level, (ServerPlayer) user, stack, hand)
                ? InteractionResult.SUCCESS_SERVER
                : InteractionResult.FAIL;
    }

    // recall starts from a client-only empty left-click input so the server revalidates
    // the held item and current sneak state here before touching summon state
    public static boolean tryRecallActiveSummons(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BoneScytheItem) || !player.isShiftKeyDown()) {
            return false;
        }

        ReapedSoulManager.RecallResult recallResult = ReapedSoulManager.recallSummons(player);
        if (recallResult.recalledSummons() <= 0) {
            sendPriorityOverlay(player, stack, Component.literal("No reaped souls answer your recall.").withStyle(ChatFormatting.RED));
            return false;
        }

        int actualRefund = addSoulCharges(stack, recallResult.refundedCharges());
        player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_SAND_BREAK, player.getSoundSource(), RECALL_VOLUME, RECALL_PITCH);
        showRecallStatus(player, stack, recallResult, actualRefund);
        return true;
    }

    private boolean shouldApplyWither(LivingEntity attacker) {
        return attacker instanceof net.minecraft.world.entity.player.Player player
                ? player.getAttackStrengthScale(0.5f) > 0.9f
                : attacker.getRandom().nextFloat() < 0.25f;
    }

    private void harvestSoul(ServerLevel level, ItemStack stack, LivingEntity attacker, LivingEntity target) {
        BoneScytheSoulProfile profile = BoneScytheSoulProfile.fromHarvestTarget(target);
        if (profile == null) {
            return;
        }

        addImprint(stack, ReapedSoulImprint.capture(profile, target), level.registryAccess());

        boolean gainedCharge = false;
        if (attacker.getRandom().nextFloat() < profile.chargeChance()) {
            gainedCharge = addSoulCharge(stack, 1);
        }

        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 0.7, target.getZ(), 8, 0.25, 0.35, 0.25, 0.04);
        level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), attacker.getSoundSource(), 0.65f, 0.95f);

        if (attacker instanceof ServerPlayer player) {
            Component message = Component.literal("Harvested ").withStyle(ChatFormatting.GRAY)
                    .append(profile.summonName().copy().withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(gainedCharge ? " and gained a Soul Charge." : ".").withStyle(ChatFormatting.GRAY));
            sendPriorityOverlay(player, stack, message);
        }
    }

    private void cycleSelectedImprint(ServerPlayer player, ItemStack stack) {
        List<ReapedSoulImprint> imprints = getImprints(stack, player.registryAccess());
        if (imprints.isEmpty()) {
            sendPriorityOverlay(player, stack, Component.literal("No harvested souls are stored in the scythe.").withStyle(ChatFormatting.RED));
            return;
        }

        int nextIndex = (getSelectedImprintIndex(stack) + 1) % imprints.size();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SELECTED_IMPRINT_INDEX_KEY, nextIndex));
        playSoulSelectionSound(player, imprints.get(nextIndex).profile());
        showScytheStatus(player, stack, "Selected soul", SELECTION_OVERLAY_TICKS);
    }

    private boolean handleUse(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        long gameTime = level.getGameTime();
        clearExpiredSummonWindow(stack, gameTime);
        if (getSummonWeightRemaining(stack, gameTime) > 0) {
            return trySpendSummonWeight(level, player, stack, gameTime);
        }

        return tryOpenSummonWindow(level, player, stack, hand);
    }

    // converts all currently stored charges into temporary summon weight for one timed summon window
    private boolean tryOpenSummonWindow(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        List<ReapedSoulImprint> imprints = getImprints(stack, player.registryAccess());
        if (imprints.isEmpty()) {
            sendPriorityOverlay(player, stack, Component.literal("No harvested souls are stored in the scythe.").withStyle(ChatFormatting.RED));
            return false;
        }

        if (ReapedSoulManager.countActiveSummons(player) >= MAX_ACTIVE_SUMMONS) {
            sendPriorityOverlay(player, stack, Component.literal("You cannot sustain more than 3 reaped souls at once.").withStyle(ChatFormatting.RED));
            return false;
        }

        if (player.getCooldowns().isOnCooldown(stack)) {
            sendPriorityOverlay(player, stack, Component.literal("The scythe is still gathering itself.").withStyle(ChatFormatting.RED));
            return false;
        }

        int storedCharges = getSoulChargeCount(stack);
        if (storedCharges <= 0) {
            sendPriorityOverlay(player, stack, Component.literal("You need at least 1 Soul Charge to open the summon window.").withStyle(ChatFormatting.RED));
            return false;
        }

        consumeSoulCharges(stack, storedCharges);
        startSummonWindow(stack, level.getGameTime(), storedCharges);
        stack.hurtAndBreak(SUMMON_DURABILITY_COST, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        playSummonWindowOpenSound(level, player);
        showScytheStatus(player, stack, "Siphoned " + storedCharges + " charge" + (storedCharges == 1 ? "" : "s"), PRIORITY_OVERLAY_TICKS);
        return true;
    }

    // spends weight and consumes exactly one stored imprint once the summon exists
    // its lifetime and ai are delegated to ReapedSoulManager
    private boolean trySpendSummonWeight(ServerLevel level, ServerPlayer player, ItemStack stack, long gameTime) {
        List<ReapedSoulImprint> imprints = getImprints(stack, player.registryAccess());
        if (imprints.isEmpty()) {
            sendPriorityOverlay(player, stack, Component.literal("No harvested souls are stored in the scythe.").withStyle(ChatFormatting.RED));
            clearSummonWindow(stack);
            playSummonWindowCloseSound(level, player);
            player.getCooldowns().addCooldown(stack, SUMMON_COOLDOWN_TICKS);
            return false;
        }

        ReapedSoulImprint selectedImprint = getSelectedImprint(stack, player.registryAccess());
        if (selectedImprint == null) {
            sendPriorityOverlay(player, stack, Component.literal("Select a stored soul before summoning.").withStyle(ChatFormatting.RED));
            return false;
        }
        BoneScytheSoulProfile selectedProfile = selectedImprint.profile();

        if (ReapedSoulManager.countActiveSummons(player) >= MAX_ACTIVE_SUMMONS) {
            sendPriorityOverlay(player, stack, Component.literal("You cannot sustain more than 3 reaped souls at once.").withStyle(ChatFormatting.RED));
            return false;
        }

        int remainingWeight = getSummonWeightRemaining(stack, gameTime);
        if (selectedProfile.weight() > remainingWeight) {
            sendPriorityOverlay(
                    player,
                    stack,
                    Component.literal("That soul is too heavy for the remaining summon weight: ")
                            .withStyle(ChatFormatting.RED)
                            .append(Component.literal(Integer.toString(remainingWeight)).withStyle(ChatFormatting.GOLD))
            );
            return false;
        }

        Vec3 look = player.getLookAngle();
        Vec3 horizontalLook = new Vec3(look.x, 0.0, look.z);
        if (horizontalLook.lengthSqr() < 1.0E-6) {
            horizontalLook = Vec3.directionFromRotation(0.0f, player.getYRot());
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        Vec3 spawnPos = player.position().add(horizontalLook.scale(1.5));
        if (!ReapedSoulManager.summon(level, player, selectedImprint, spawnPos)) {
            sendPriorityOverlay(player, stack, Component.literal("The soul fails to coalesce.").withStyle(ChatFormatting.RED));
            return false;
        }

        consumeSelectedImprint(stack);
        setSummonWeightRemaining(stack, remainingWeight - selectedProfile.weight());
        if (getSummonWeightRemaining(stack, gameTime) <= 0 || ReapedSoulManager.countActiveSummons(player) >= MAX_ACTIVE_SUMMONS) {
            clearSummonWindow(stack);
            playSummonWindowCloseSound(level, player);
            player.getCooldowns().addCooldown(stack, SUMMON_COOLDOWN_TICKS);
        }

        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnPos.x, spawnPos.y + 1.0, spawnPos.z, 10, 0.25, 0.35, 0.25, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_SAND_STEP, player.getSoundSource(), SUMMON_CAST_VOLUME, SUMMON_CAST_PITCH);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), player.getSoundSource(), 0.8f, 0.9f);
        showScytheStatus(player, stack, "Summoned", PRIORITY_OVERLAY_TICKS);
        return true;
    }

    private static void playSummonWindowOpenSound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_SAND_PLACE, player.getSoundSource(), SUMMON_WINDOW_OPEN_VOLUME, SUMMON_WINDOW_OPEN_PITCH);
    }

    private static void playSummonWindowCloseSound(ServerLevel level, ServerPlayer player) {
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_SAND_BREAK, player.getSoundSource(), SUMMON_WINDOW_CLOSE_VOLUME, SUMMON_WINDOW_CLOSE_PITCH);
    }

    private static void showScytheStatus(ServerPlayer player, ItemStack stack, String prefix, int suppressTicks) {
        ReapedSoulImprint selectedImprint = getSelectedImprint(stack, player.registryAccess());
        String selectedText = selectedImprint == null
                ? "None"
                : selectedImprint.profile().summonName().getString();
        String message = prefix
                + ": "
                + selectedText
                + " | Charges "
                + getSoulChargeCount(stack)
                + "/"
                + SOUL_CHARGE_CAP
                + " | Imprints "
                + getImprints(stack, player.registryAccess()).size()
                + "/"
                + SOUL_IMPRINT_CAP
                + " | Weight "
                + getStoredSummonWeight(stack);

        sendPriorityOverlay(player, stack, Component.literal(message).withStyle(ChatFormatting.AQUA), suppressTicks);
    }

    private static void showRecallStatus(ServerPlayer player, ItemStack stack, ReapedSoulManager.RecallResult recallResult, int actualRefund) {
        String message = "Recalled "
                + recallResult.recalledSummons()
                + " soul"
                + (recallResult.recalledSummons() == 1 ? "" : "s")
                + " | Weight "
                + recallResult.totalWeight()
                + " -> Charges "
                + actualRefund;

        if (actualRefund < recallResult.refundedCharges()) {
            message += " (cap reached)";
        }

        sendPriorityOverlay(player, stack, Component.literal(message).withStyle(ChatFormatting.AQUA));
    }

    private static int getSoulChargeCount(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        return customData.copyTag().getIntOr(SOUL_CHARGES_KEY, 0);
    }

    private static boolean addSoulCharge(ItemStack stack, int amount) {
        int before = getSoulChargeCount(stack);
        int after = Math.min(SOUL_CHARGE_CAP, before + amount);
        if (after == before) {
            return false;
        }

        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SOUL_CHARGES_KEY, after));
        return true;
    }

    private static int addSoulCharges(ItemStack stack, int amount) {
        int before = getSoulChargeCount(stack);
        int after = Math.min(SOUL_CHARGE_CAP, before + Math.max(0, amount));
        if (after != before) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SOUL_CHARGES_KEY, after));
        }

        return after - before;
    }

    public static int debugGrantSoulCharges(ItemStack stack, int amount) {
        int clampedAmount = Math.max(0, amount);
        if (clampedAmount > 0) {
            addSoulCharge(stack, clampedAmount);
        }
        return getSoulChargeCount(stack);
    }

    private static void consumeSoulCharges(ItemStack stack, int amount) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SOUL_CHARGES_KEY, Math.max(0, tag.getIntOr(SOUL_CHARGES_KEY, 0) - amount)));
    }

    private static List<ReapedSoulImprint> getImprints(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        List<ReapedSoulImprint> imprints = new ArrayList<>();
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return imprints;
        }

        ListTag listTag = customData.copyTag().getListOrEmpty(SOUL_IMPRINTS_KEY);
        for (int i = 0; i < listTag.size(); i++) {
            Tag entryTag = listTag.get(i);
            if (entryTag instanceof CompoundTag compoundTag) {
                ReapedSoulImprint imprint = ReapedSoulImprint.fromTag(compoundTag, registries);
                if (imprint != null) {
                    imprints.add(imprint);
                }
            } else {
                BoneScytheSoulProfile legacyProfile = BoneScytheSoulProfile.fromId(listTag.getStringOr(i, ""));
                if (legacyProfile != null) {
                    imprints.add(ReapedSoulImprint.bare(legacyProfile));
                }
            }
        }

        return imprints;
    }

    private static void addImprint(ItemStack stack, ReapedSoulImprint imprint, net.minecraft.core.HolderLookup.Provider registries) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            ListTag listTag = tag.getListOrEmpty(SOUL_IMPRINTS_KEY);
            listTag.add(imprint.toTag(registries));
            if (listTag.size() > SOUL_IMPRINT_CAP) {
                listTag.remove(0);
                int selectedIndex = Math.max(0, tag.getIntOr(SELECTED_IMPRINT_INDEX_KEY, 0) - 1);
                tag.putInt(SELECTED_IMPRINT_INDEX_KEY, selectedIndex);
            }
            tag.put(SOUL_IMPRINTS_KEY, listTag);
            clampSelectedIndex(tag);
        });
    }

    private static @Nullable ReapedSoulImprint getSelectedImprint(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        List<ReapedSoulImprint> imprints = getImprints(stack, registries);
        if (imprints.isEmpty()) {
            return null;
        }

        int index = Math.max(0, Math.min(getSelectedImprintIndex(stack), imprints.size() - 1));
        return imprints.get(index);
    }

    private static int getSelectedImprintIndex(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        return customData.copyTag().getIntOr(SELECTED_IMPRINT_INDEX_KEY, 0);
    }

    private static void consumeSelectedImprint(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            ListTag listTag = tag.getListOrEmpty(SOUL_IMPRINTS_KEY);
            if (listTag.isEmpty()) {
                return;
            }

            int selectedIndex = Math.max(0, Math.min(tag.getIntOr(SELECTED_IMPRINT_INDEX_KEY, 0), listTag.size() - 1));
            listTag.remove(selectedIndex);
            tag.put(SOUL_IMPRINTS_KEY, listTag);
            clampSelectedIndex(tag);
        });
    }

    private static void clampSelectedIndex(CompoundTag tag) {
        ListTag listTag = tag.getListOrEmpty(SOUL_IMPRINTS_KEY);
        if (listTag.isEmpty()) {
            tag.putInt(SELECTED_IMPRINT_INDEX_KEY, 0);
            return;
        }

        int selectedIndex = tag.getIntOr(SELECTED_IMPRINT_INDEX_KEY, 0);
        tag.putInt(SELECTED_IMPRINT_INDEX_KEY, Math.max(0, Math.min(selectedIndex, listTag.size() - 1)));
    }

    private static void startSummonWindow(ItemStack stack, long currentGameTime, int weight) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(SUMMON_WINDOW_END_TICK_KEY, currentGameTime + SUMMON_WINDOW_TICKS);
            tag.putInt(SUMMON_WEIGHT_REMAINING_KEY, Math.max(0, weight));
            tag.putInt(LAST_SUMMON_WINDOW_TICK_SOUND_SECOND_KEY, 0);
        });
    }

    private static void clearSummonWindow(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(SUMMON_WINDOW_END_TICK_KEY, 0L);
            tag.putInt(SUMMON_WEIGHT_REMAINING_KEY, 0);
            tag.putInt(LAST_SUMMON_WINDOW_TICK_SOUND_SECOND_KEY, 0);
        });
    }

    private static boolean clearExpiredSummonWindow(ItemStack stack, long currentGameTime) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        CompoundTag tag = customData.copyTag();
        long endTick = tag.getLongOr(SUMMON_WINDOW_END_TICK_KEY, 0L);
        if (endTick > 0L && currentGameTime > endTick) {
            clearSummonWindow(stack);
            return true;
        }

        return false;
    }

    private static int getStoredSummonWeight(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        return customData.copyTag().getIntOr(SUMMON_WEIGHT_REMAINING_KEY, 0);
    }

    private static long getSummonWindowEndTick(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0L;
        }

        return customData.copyTag().getLongOr(SUMMON_WINDOW_END_TICK_KEY, 0L);
    }

    private static int getSummonWeightRemaining(ItemStack stack, long currentGameTime) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return 0;
        }

        CompoundTag tag = customData.copyTag();
        long endTick = tag.getLongOr(SUMMON_WINDOW_END_TICK_KEY, 0L);
        if (endTick <= 0L || currentGameTime > endTick) {
            return 0;
        }

        return tag.getIntOr(SUMMON_WEIGHT_REMAINING_KEY, 0);
    }

    private static void setSummonWeightRemaining(ItemStack stack, int weight) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SUMMON_WEIGHT_REMAINING_KEY, Math.max(0, weight)));
    }

    private static void sendPriorityOverlay(ServerPlayer player, ItemStack stack, Component message) {
        sendPriorityOverlay(player, stack, message, PRIORITY_OVERLAY_TICKS);
    }

    private static void sendPriorityOverlay(ServerPlayer player, ItemStack stack, Component message, int suppressTicks) {
        setPriorityOverlaySuppressUntil(stack, player.level().getGameTime() + suppressTicks);
        player.sendOverlayMessage(message);
    }

    private static boolean isPriorityOverlayActive(ItemStack stack, long currentGameTime) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        return currentGameTime < customData.copyTag().getLongOr(OVERLAY_SUPPRESS_UNTIL_TICK_KEY, 0L);
    }

    private static void setPriorityOverlaySuppressUntil(ItemStack stack, long gameTime) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(OVERLAY_SUPPRESS_UNTIL_TICK_KEY, gameTime));
    }

    // uses window-relative elapsed seconds instead of wall-clock timing so the tick sound
    // fires once per second even if the server lags or the item changes hands mid-window
    private static void playSummonWindowTickSoundIfNeeded(ServerPlayer player, ItemStack stack, long currentGameTime) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return;
        }

        CompoundTag tag = customData.copyTag();
        long endTick = tag.getLongOr(SUMMON_WINDOW_END_TICK_KEY, 0L);
        if (endTick <= 0L || currentGameTime > endTick) {
            return;
        }

        long startTick = endTick - SUMMON_WINDOW_TICKS;
        int elapsedSeconds = (int) ((currentGameTime - startTick) / 20L);
        int lastPlayedSecond = tag.getIntOr(LAST_SUMMON_WINDOW_TICK_SOUND_SECOND_KEY, 0);
        if (elapsedSeconds <= 0 || elapsedSeconds <= lastPlayedSecond) {
            return;
        }

        playOwnerOnlySound(player, SoundEvents.NOTE_BLOCK_HAT, SUMMON_WINDOW_TICK_VOLUME, SUMMON_WINDOW_TICK_PITCH);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, data -> data.putInt(LAST_SUMMON_WINDOW_TICK_SOUND_SECOND_KEY, elapsedSeconds));
    }

    private static void playSoulSelectionSound(ServerPlayer player, BoneScytheSoulProfile profile) {
        playOwnerOnlySound(player, SoundEvents.NOTE_BLOCK_CHIME, SOUL_SELECTION_VOLUME, getSelectionPitch(profile.tier()));
    }

    private static float getSelectionPitch(int tier) {
        return switch (tier) {
            case 1 -> 0.5f;
            case 2 -> 0.8f;
            case 3 -> 1.1f;
            case 4 -> 1.4f;
            case 5 -> 1.8f;
            default -> 1.0f;
        };
    }

    private static void playOwnerOnlySound(ServerPlayer player, Holder<SoundEvent> sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(
                sound,
                player.getSoundSource(),
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                volume,
                pitch,
                player.getRandom().nextLong()
        ));
    }
}
