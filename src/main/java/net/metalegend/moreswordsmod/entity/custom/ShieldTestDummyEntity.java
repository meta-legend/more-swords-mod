package net.metalegend.moreswordsmod.entity.custom;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

public class ShieldTestDummyEntity extends Zombie {
    private int shieldDisabledTicks = 0;

    public ShieldTestDummyEntity(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
        equipShield();
        setDropChance(EquipmentSlot.OFFHAND, 0.0f);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public void tick() {
        super.tick();

        if (shieldDisabledTicks > 0) {
            shieldDisabledTicks--;
        }

        if (!level().isClientSide()) {
            equipShield();
            if (shieldDisabledTicks <= 0 && !isUsingItem()) {
                startUsingItem(InteractionHand.OFF_HAND);
            }
        }
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    public void disableShieldForTicks(int ticks) {
        shieldDisabledTicks = Math.max(shieldDisabledTicks, ticks);
        stopUsingItem();
    }

    private void equipShield() {
        if (!getOffhandItem().is(Items.SHIELD)) {
            setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }
}
