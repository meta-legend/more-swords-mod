package net.metalegend.moreswordsmod.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class WindStaffItem extends Item {

    private static final int COOLDOWN_TICKS = 40; // 2 seconds

    public WindStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide()) {
            if (!user.getCooldowns().isOnCooldown(stack)) {
                // Launch target upward and slightly away from the player
                Vec3 knockback = target.position().subtract(user.position()).normalize().scale(1.5);
                target.setDeltaMovement(knockback.x, 2.5, knockback.z);
                target.hurtMarked = true;

                user.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
