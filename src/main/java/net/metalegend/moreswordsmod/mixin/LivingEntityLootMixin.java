package net.metalegend.moreswordsmod.mixin;

import net.metalegend.moreswordsmod.soul.ReapedSoulManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// prevents reaped summons from dropping normal death loot while still letting equipment be cleared
@Mixin(LivingEntity.class)
public abstract class LivingEntityLootMixin {
    @Shadow
    protected abstract void dropEquipment(ServerLevel level);

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
    private void moreswordsmod$preventReapedSoulDeathLoot(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, CallbackInfo ci) {
        if (ReapedSoulManager.isActiveSummon((LivingEntity) (Object) this)) {
            this.dropEquipment(level);
            ci.cancel();
        }
    }
}
