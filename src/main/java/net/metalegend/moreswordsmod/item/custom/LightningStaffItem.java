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

import java.util.Timer;
import java.util.TimerTask;

public class LightningStaffItem extends Item {

    public LightningStaffItem(Item.Properties properties) {
        super(properties);
    }

    int iteratorUpdate = -2;
    public boolean onCooldown = true;
    Timer timer = new Timer();

    TimerTask timerTask = new TimerTask() {
        int i = 0;

        @Override
        public void run() {
            iteratorUpdate = i;

            if (i == 2) {
                i = -1;
                onCooldown = false;
            } else if (i == -1 && onCooldown) {
                i++;
            } else {
                i++;
            }
        }
    };

    public void runTimer() {
        timer.schedule(timerTask, 0, 1000);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (iteratorUpdate == -2) {
            runTimer();
        }

        if (!user.level().isClientSide() && !onCooldown) {
            Vec3 targetPos = entity.position();
            LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, user.level());
            lightning.setPos(targetPos.x, targetPos.y, targetPos.z);
            user.level().addFreshEntity(lightning);
            onCooldown = true;
        }

        return InteractionResult.SUCCESS;
    }
}