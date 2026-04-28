package net.metalegend.moreswordsmod.mixin;

import net.metalegend.moreswordsmod.item.custom.KatanaItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// applies the katana's dash-defense rules at the point where vanilla resolves incoming
// damage and knockback on living entities
@Mixin(LivingEntity.class)
public abstract class LivingEntityKatanaDashMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void moreswordsmod$blockKatanaDashProjectileDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (KatanaItem.blocksProjectileDamage(entity, source)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float moreswordsmod$reduceKatanaDashMeleeDamage(float damage, ServerLevel level, DamageSource source) {
        return KatanaItem.modifyIncomingDashDamage((LivingEntity) (Object) this, source, damage);
    }

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void moreswordsmod$blockKatanaDashKnockback(double power, double xd, double zd, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (KatanaItem.blocksDashKnockback(entity)) {
            ci.cancel();
        }
    }
}
