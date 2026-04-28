package net.metalegend.moreswordsmod.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// exposes mob targetselector so summon setup can strip or replace vanilla targeting goals
@Mixin(Mob.class)
public interface MobAccessorMixin {
    @Accessor("targetSelector")
    GoalSelector moreswordsmod$getTargetSelector();
}
