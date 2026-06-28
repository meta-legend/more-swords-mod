package net.metalegend.moreswordsmod.mixin;

import net.metalegend.moreswordsmod.soul.ReapedSoulManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// keeps reaped cube mobs allied to the owner and rebinds split children into summon tracking
@Mixin(AbstractCubeMob.class)
public abstract class ReapedSlimeMixin {
    @Inject(method = "dealDamage", at = @At("HEAD"), cancellable = true)
    private void moreswordsmod$preventFriendlyContactDamage(LivingEntity target, CallbackInfo ci) {
        AbstractCubeMob cubeMob = (AbstractCubeMob) (Object) this;
        if (ReapedSoulManager.isReapedFriendlyContact(cubeMob, target)) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void moreswordsmod$adoptSplitChildren(Entity.RemovalReason reason, CallbackInfo ci) {
        AbstractCubeMob cubeMob = (AbstractCubeMob) (Object) this;
        if (cubeMob.isDeadOrDying()) {
            ReapedSoulManager.adoptSplitChildren(cubeMob);
        }
    }
}
