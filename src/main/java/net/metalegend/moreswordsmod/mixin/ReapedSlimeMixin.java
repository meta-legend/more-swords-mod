package net.metalegend.moreswordsmod.mixin;

import net.metalegend.moreswordsmod.soul.ReapedSoulManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// keeps reaped slimes allied to the owner and rebinds split children into summon tracking
@Mixin(Slime.class)
public abstract class ReapedSlimeMixin {
    @Inject(method = "dealDamage", at = @At("HEAD"), cancellable = true)
    private void moreswordsmod$preventFriendlyContactDamage(LivingEntity target, CallbackInfo ci) {
        Slime slime = (Slime) (Object) this;
        if (ReapedSoulManager.isReapedFriendlyContact(slime, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void moreswordsmod$adoptSplitChildren(Entity.RemovalReason reason, CallbackInfo ci) {
        Slime slime = (Slime) (Object) this;
        if (slime.isDeadOrDying()) {
            ReapedSoulManager.adoptSplitChildren(slime);
        }
    }
}
