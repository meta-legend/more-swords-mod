package net.metalegend.moreswordsmod.mixin.client;

import net.metalegend.moreswordsmod.client.KatanaSheathStrikeAnimation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// gives other players a lightweight forward lean pose during confirmed sheath strikes
@Mixin(PlayerModel.class)
public abstract class PlayerModelKatanaAnimationMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void moreswordsmod$applySheathStrikePlayerPose(AvatarRenderState renderState, CallbackInfo ci) {
        float leanAmount = KatanaSheathStrikeAnimation.getLeanAmount(renderState.id, 0.0f);
        if (leanAmount <= 0.0f) {
            return;
        }

        boolean rightHanded = renderState.mainArm != HumanoidArm.LEFT;
        int side = rightHanded ? 1 : -1;
        PlayerModel model = (PlayerModel) (Object) this;
        ModelPart swordArm = rightHanded ? model.rightArm : model.leftArm;
        ModelPart swordSleeve = rightHanded ? model.rightSleeve : model.leftSleeve;

        model.body.xRot += 0.35f * leanAmount;
        swordArm.xRot = lerp(swordArm.xRot, -1.35f, 0.85f * leanAmount);
        swordArm.yRot += side * -0.55f * leanAmount;
        swordArm.zRot += side * -0.38f * leanAmount;
        copyRotation(swordArm, swordSleeve);
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * Math.min(1.0f, Math.max(0.0f, amount));
    }

    private static void copyRotation(ModelPart source, ModelPart target) {
        target.xRot = source.xRot;
        target.yRot = source.yRot;
        target.zRot = source.zRot;
    }
}
