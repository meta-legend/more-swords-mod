package net.metalegend.moreswordsmod.item.custom;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Timer;
import java.util.TimerTask;

public class LightningStaffItem extends Item {
    public LightningStaffItem(FabricItemSettings settings) {
        super(settings);
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
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (iteratorUpdate == -2) {
            runTimer();
        }
        if (!user.getWorld().isClient && !onCooldown) {
            Vec3d targetPos = entity.getPos();
            LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, user.getWorld()); // Create the lightning bolt
            lightning.setPosition(targetPos); // Set its position. This will make the lightning bolt strike the player (probably not what you want)
            user.getWorld().spawnEntity(lightning); // Spawn the lightning entity
            onCooldown = true;
        }
        return ActionResult.SUCCESS;
    }
}
