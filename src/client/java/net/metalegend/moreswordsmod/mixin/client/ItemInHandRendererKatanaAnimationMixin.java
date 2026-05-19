package net.metalegend.moreswordsmod.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.metalegend.moreswordsmod.client.KatanaSheathStrikeAnimation;
import net.metalegend.moreswordsmod.item.custom.KatanaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// bends the first-person katana into a held iaido pose with a quiet recovery
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererKatanaAnimationMixin {
    @ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true, index = 5)
    private float moreswordsmod$suppressVanillaSwingDuringSheathStrike(float swingProgress) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !KatanaSheathStrikeAnimation.isActive(client.player.getId())) {
            return swingProgress;
        }

        if (client.player.getMainHandItem().getItem() instanceof KatanaItem
                || client.player.getOffhandItem().getItem() instanceof KatanaItem) {
            return 0.0f;
        }

        return swingProgress;
    }

    @Inject(
            method = "renderArmWithItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 1
            )
    )
    private void moreswordsmod$applySheathStrikeItemPose(
            AbstractClientPlayer player,
            float partialTick,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack stack,
            float equipProgress,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int packedLight,
            CallbackInfo ci
    ) {
        if (!(stack.getItem() instanceof KatanaItem) || !KatanaSheathStrikeAnimation.isActive(player.getId())) {
            return;
        }

        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        HumanoidArm renderedArm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        int handSide = renderedArm == HumanoidArm.RIGHT ? 1 : -1;
        KatanaSheathStrikeAnimation.applyFirstPersonScreenPose(player.getId(), partialTick, handSide, poseStack);
    }
}
