package net.metalegend.moreswordsmod.mixin;

import net.metalegend.moreswordsmod.item.custom.WindStaffItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// The Wind Staff ride boost runs through LivingEntity.travel() for any living mount.
// It forces the mount airborne, protects impulse before travel, then restores it after travel.
@Mixin(LivingEntity.class)
public abstract class MountWindBoostMixin {
    @Inject(method = "travel", at = @At("HEAD"))
    private void moreswordsmod$suppressGroundFrictionDuringWindBoost(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        WindStaffItem.forceMountBoostAirborne(self);
        WindStaffItem.stabilizeMountBoostVelocity(self);
    }

    @Inject(method = "travel", at = @At("TAIL"))
    private void moreswordsmod$stabilizeWindBoostVelocity(Vec3 travelVector, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        WindStaffItem.stabilizeMountBoostVelocity(self);
    }
}
