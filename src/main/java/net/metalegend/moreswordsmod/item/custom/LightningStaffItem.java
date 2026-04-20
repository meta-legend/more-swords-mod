package net.metalegend.moreswordsmod.item.custom;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;


public class LightningStaffItem extends Item {

    public LightningStaffItem(Item.Properties properties) {
        super(properties);
    }

    // override the result of an interaction with another mob
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        // check if the instance of the mod is on the client
        if (!user.level().isClientSide()) {
            if (!user.getCooldowns().isOnCooldown(stack)) {
                Vec3 targetPos = entity.position();
                LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, user.level());
                lightning.setPos(targetPos.x, targetPos.y, targetPos.z);
                user.level().addFreshEntity(lightning);

                user.getCooldowns().addCooldown(stack, 60); // 60 ticks = 3 seconds
            }
        }

        return InteractionResult.SUCCESS;
    }
}