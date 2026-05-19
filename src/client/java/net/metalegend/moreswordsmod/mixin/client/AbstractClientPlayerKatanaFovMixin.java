package net.metalegend.moreswordsmod.mixin.client;

import net.metalegend.moreswordsmod.client.KatanaSheathStrikeAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// adds a small fov kick only while a confirmed sheath strike animation is active
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerKatanaFovMixin {
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void moreswordsmod$applySheathStrikeFov(boolean useFovSetting, float partialTick, CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer player = (AbstractClientPlayer) (Object) this;
        cir.setReturnValue(cir.getReturnValue() * KatanaSheathStrikeAnimation.getFovMultiplier(player.getId(), partialTick));
    }
}
