package net.metalegend.moreswordsmod.mixin.client;

import net.metalegend.moreswordsmod.client.KatanaSheathStrikeAnimation;
import net.metalegend.moreswordsmod.item.custom.KatanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// starts the first-person sheath strike pose immediately when the local attack is sent
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeKatanaAnimationMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void moreswordsmod$predictSheathStrikeAnimation(Player player, Entity target, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || player != client.player || target == null) {
            return;
        }

        ItemStack mainHandStack = player.getMainHandItem();
        if (!KatanaItem.isSheathStrikeReadyForAnimation(mainHandStack, player.level().getGameTime())) {
            return;
        }

        KatanaSheathStrikeAnimation.start(player.getId());
    }
}
