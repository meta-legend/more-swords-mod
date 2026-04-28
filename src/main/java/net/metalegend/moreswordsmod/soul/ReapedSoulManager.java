package net.metalegend.moreswordsmod.soul;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.metalegend.moreswordsmod.mixin.MobAccessorMixin;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.illager.Evoker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// server-side owner for reaped summon lifecycle
// BoneScytheItem stores charges imprints and summon-window state on the item stack
// this manager owns the summoned entities themselves: spawn setup team binding
// ticking recall and cleanup
public final class ReapedSoulManager {
    private static final String TEMP_TEAM_PREFIX = "mws_reaped_";
    private static final int SUMMON_LIFETIME_TICKS = 300;
    private static final int MAX_REAPED_VEXES = 2;
    private static final double TARGET_RANGE = 16.0;
    private static final double TELEPORT_DISTANCE_SQR = 144.0;
    private static final double VEX_BIND_RANGE = 24.0;
    private static final Map<UUID, SummonState> ACTIVE_SUMMONS = new HashMap<>();
    private static final Map<UUID, HelperState> ACTIVE_HELPERS = new HashMap<>();

    private ReapedSoulManager() {
    }

    // registers the global server tick that drives summon lifetime following and combat
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                tickWorld(level);
            }
        });
    }

    public static int countActiveSummons(ServerPlayer owner) {
        int count = 0;
        for (SummonState state : ACTIVE_SUMMONS.values()) {
            if (state.ownerUuid.equals(owner.getUUID())) {
                count++;
            }
        }
        return count;
    }

    // recalls only summons owned by the requesting player and computes the refund from
    // stored summon profile weight instead of any mutable runtime mob state
    public static RecallResult recallSummons(ServerPlayer owner) {
        int recalledSummons = 0;
        int totalWeight = 0;

        Iterator<Map.Entry<UUID, SummonState>> summonIterator = ACTIVE_SUMMONS.entrySet().iterator();
        while (summonIterator.hasNext()) {
            Map.Entry<UUID, SummonState> entry = summonIterator.next();
            SummonState state = entry.getValue();
            if (!state.ownerUuid.equals(owner.getUUID())) {
                continue;
            }

            ServerLevel summonLevel = owner.level().getServer().getLevel(state.dimension);
            if (summonLevel != null) {
                Entity entity = summonLevel.getEntity(entry.getKey());
                if (entity != null) {
                    entity.discard();
                }

                cleanupTeamBinding(
                        summonLevel,
                        entry.getKey(),
                        state.ownerUuid,
                        state.teamName,
                        state.ownerScoreboardName,
                        state.summonScoreboardName,
                        state.ownerTemporaryTeam
                );
            }

            recalledSummons++;
            totalWeight += state.profile.weight();
            summonIterator.remove();
        }

        Iterator<Map.Entry<UUID, HelperState>> helperIterator = ACTIVE_HELPERS.entrySet().iterator();
        while (helperIterator.hasNext()) {
            Map.Entry<UUID, HelperState> entry = helperIterator.next();
            HelperState state = entry.getValue();
            if (!state.ownerUuid.equals(owner.getUUID())) {
                continue;
            }

            ServerLevel helperLevel = owner.level().getServer().getLevel(state.dimension);
            if (helperLevel != null) {
                Entity entity = helperLevel.getEntity(entry.getKey());
                if (entity != null) {
                    entity.discard();
                }

                cleanupTeamBinding(
                        helperLevel,
                        entry.getKey(),
                        state.ownerUuid,
                        state.teamName,
                        state.ownerScoreboardName,
                        state.summonScoreboardName,
                        state.ownerTemporaryTeam
                );
            }

            helperIterator.remove();
        }

        return new RecallResult(recalledSummons, totalWeight, Math.round(totalWeight * 0.5f));
    }

    public static boolean isActiveSummon(LivingEntity entity) {
        return isTrackedReapedAlly(entity.getUUID());
    }

    public static boolean isReapedFriendlyContact(Slime slime, LivingEntity target) {
        return isTrackedReapedAlly(slime.getUUID()) && target.isAlliedTo(slime);
    }

    public static void adoptSplitChildren(Slime parent) {
        SummonState parentState = ACTIVE_SUMMONS.get(parent.getUUID());
        if (parentState == null || !(parent.level() instanceof ServerLevel level)) {
            return;
        }

        List<Slime> splitChildren = level.getEntitiesOfClass(
                Slime.class,
                parent.getBoundingBox().inflate(3.0),
                candidate -> candidate.isAlive()
                        && candidate != parent
                        && candidate.getType() == parent.getType()
                        && !ACTIVE_SUMMONS.containsKey(candidate.getUUID())
        );

        for (Slime child : splitChildren) {
            child.setCustomName(parent.getCustomName());
            child.setCustomNameVisible(false);
            child.setGlowingTag(true);
            child.setPersistenceRequired();
            child.setCanPickUpLoot(false);

            bindSplitChildToStoredTeam(level, parentState, child);
            ACTIVE_SUMMONS.put(
                    child.getUUID(),
                    new SummonState(
                            parentState.dimension,
                            parentState.ownerUuid,
                            parentState.profile,
                            parentState.expireGameTime,
                            parentState.ownerScoreboardName,
                            child.getScoreboardName(),
                            parentState.teamName,
                            parentState.ownerTemporaryTeam
                    )
            );
        }
    }

    public static boolean summon(ServerLevel level, ServerPlayer owner, ReapedSoulImprint imprint, Vec3 spawnPos) {
        BoneScytheSoulProfile profile = imprint.profile();
        Entity entity = profile.sourceType().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (!(entity instanceof Mob mob)) {
            return false;
        }

        mob.setPos(spawnPos.x, owner.getY(), spawnPos.z);
        mob.setCustomName(profile.summonName());
        mob.setCustomNameVisible(false);
        mob.setGlowingTag(true);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        applyProfileStats(mob, imprint);
        applyCapturedEquipment(mob, imprint);
        overrideSpecialSummonAi(mob);

        if (!level.addFreshEntity(mob)) {
            return false;
        }

        TeamBinding teamBinding = bindSummonToOwnerTeam(owner, mob);
        ACTIVE_SUMMONS.put(
                mob.getUUID(),
                new SummonState(
                        level.dimension(),
                        owner.getUUID(),
                        profile,
                        level.getGameTime() + SUMMON_LIFETIME_TICKS,
                        owner.getScoreboardName(),
                        mob.getScoreboardName(),
                        teamBinding.teamName(),
                        teamBinding.ownerTemporaryTeam()
                )
        );
        return true;
    }

    private static void tickWorld(ServerLevel level) {
        long gameTime = level.getGameTime();
        tickPrimarySummons(level, gameTime);
        tickHelpers(level, gameTime);
    }

    private static void tickPrimarySummons(ServerLevel level, long gameTime) {
        Iterator<Map.Entry<UUID, SummonState>> iterator = ACTIVE_SUMMONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SummonState> entry = iterator.next();
            SummonState state = entry.getValue();
            if (!state.dimension.equals(level.dimension())) {
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                cleanupTeamBinding(level, entry.getKey(), state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            if (!(level.getPlayerByUUID(state.ownerUuid) instanceof ServerPlayer owner) || !owner.isAlive() || gameTime >= state.expireGameTime) {
                mob.discard();
                cleanupTeamBinding(level, entry.getKey(), state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            LivingEntity target = resolveTarget(level, owner, mob);
            if (target != null) {
                mob.setTarget(target);
                driveSummonCombat(level, mob, target, state, gameTime);
            } else {
                mob.setTarget(null);
                followOwner(owner, mob, state.profile);
            }

            if (mob instanceof Evoker evoker) {
                bindReapedVexes(level, owner, evoker, state);
            }
        }
    }

    private static void tickHelpers(ServerLevel level, long gameTime) {
        Iterator<Map.Entry<UUID, HelperState>> iterator = ACTIVE_HELPERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HelperState> entry = iterator.next();
            HelperState state = entry.getValue();
            if (!state.dimension.equals(level.dimension())) {
                continue;
            }

            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Vex vex) || !vex.isAlive()) {
                cleanupTeamBinding(level, entry.getKey(), state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            Entity parentEntity = level.getEntity(state.parentUuid);
            if (!(parentEntity instanceof Evoker evoker) || !evoker.isAlive() || gameTime >= state.expireGameTime) {
                vex.discard();
                cleanupTeamBinding(level, entry.getKey(), state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            if (!(level.getPlayerByUUID(state.ownerUuid) instanceof ServerPlayer owner) || !owner.isAlive()) {
                vex.discard();
                cleanupTeamBinding(level, entry.getKey(), state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            vex.setCustomName(Component.translatable("text.moreswordsmod.reaped_vex"));
            vex.setCustomNameVisible(false);
            vex.setPersistenceRequired();
            vex.setCanPickUpLoot(false);

            LivingEntity parentTarget = evoker.getTarget();
            if (parentTarget != null && parentTarget.isAlive() && !parentTarget.isAlliedTo(owner) && !isTrackedReapedAlly(parentTarget.getUUID())) {
                vex.setTarget(parentTarget);
            }
        }
    }

    private static void applyProfileStats(Mob mob, ReapedSoulImprint imprint) {
        BoneScytheSoulProfile profile = imprint.profile();
        if (mob instanceof Slime slime) {
            slime.setSize(imprint.slimeSize(), true);
        }
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(profile.maxHealth());
        }
        if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(profile.attackDamage());
        }
        if (mob.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(profile.movementSpeed());
        }
        if (profile.babyVariant()) {
            if (mob instanceof Zombie zombie) {
                zombie.setBaby(true);
            } else if (mob instanceof Piglin piglin) {
                piglin.setBaby(true);
            } else if (mob instanceof Hoglin hoglin) {
                hoglin.setBaby(true);
            }
        }
        stabilizeNetherSummon(mob);
        mob.setHealth(mob.getMaxHealth());
    }

    // replays captured equipment exactly as stored on the imprint including enchantments and damage
    private static void applyCapturedEquipment(Mob mob, ReapedSoulImprint imprint) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            mob.setItemSlot(slot, ItemStack.EMPTY);
            mob.setDropChance(slot, 0.0f);
        }

        imprint.equipment().forEach((slot, stack) -> {
            mob.setItemSlot(slot, stack.copy());
            mob.setDropChance(slot, 0.0f);
        });
    }

    private static void overrideSpecialSummonAi(Mob mob) {
        ((MobAccessorMixin) mob).moreswordsmod$getTargetSelector().removeAllGoals(goal -> true);

        if (mob instanceof Drowned drowned) {
            drowned.removeAllGoals(goal -> {
                String goalName = goal.getClass().getSimpleName();
                return goalName.startsWith("Drowned") || goalName.equals("RandomStrollGoal");
            });
            drowned.setSearchingForLand(false);
        }

        stabilizeNetherSummon(mob);
    }

    private static TeamBinding bindSummonToOwnerTeam(ServerPlayer owner, Mob summon) {
        Scoreboard scoreboard = owner.level().getScoreboard();
        PlayerTeam ownerTeam = owner.getTeam();
        if (ownerTeam != null) {
            scoreboard.addPlayerToTeam(summon.getScoreboardName(), ownerTeam);
            return new TeamBinding(ownerTeam.getName(), false);
        }

        String teamName = TEMP_TEAM_PREFIX + owner.getUUID().toString().replace("-", "").substring(0, 12);
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
            team.setAllowFriendlyFire(false);
        }

        scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(summon.getScoreboardName(), team);
        return new TeamBinding(teamName, true);
    }

    private static void cleanupTeamBinding(
            ServerLevel level,
            UUID allyUuid,
            UUID ownerUuid,
            String teamName,
            String ownerScoreboardName,
            String summonScoreboardName,
            boolean ownerTemporaryTeam
    ) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            return;
        }

        if (scoreboard.getPlayersTeam(summonScoreboardName) == team) {
            scoreboard.removePlayerFromTeam(summonScoreboardName, team);
        }
        if (!ownerTemporaryTeam) {
            return;
        }

        boolean ownerStillHasSummonInTeam = hasTrackedAllyInTeam(ownerUuid, teamName, allyUuid);
        if (!ownerStillHasSummonInTeam) {
            if (scoreboard.getPlayersTeam(ownerScoreboardName) == team) {
                scoreboard.removePlayerFromTeam(ownerScoreboardName, team);
            }
            scoreboard.removePlayerTeam(team);
        }
    }

    private static void bindSplitChildToStoredTeam(ServerLevel level, SummonState parentState, Mob child) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(parentState.teamName);
        if (team != null) {
            scoreboard.addPlayerToTeam(child.getScoreboardName(), team);
            return;
        }

        if (level.getPlayerByUUID(parentState.ownerUuid) instanceof ServerPlayer owner) {
            bindSummonToOwnerTeam(owner, child);
        }
    }

    private static boolean hasTrackedAllyInTeam(UUID ownerUuid, String teamName, UUID excludedUuid) {
        for (Map.Entry<UUID, SummonState> entry : ACTIVE_SUMMONS.entrySet()) {
            SummonState state = entry.getValue();
            if (!entry.getKey().equals(excludedUuid) && state.ownerUuid.equals(ownerUuid) && state.teamName.equals(teamName)) {
                return true;
            }
        }
        for (Map.Entry<UUID, HelperState> entry : ACTIVE_HELPERS.entrySet()) {
            HelperState state = entry.getValue();
            if (!entry.getKey().equals(excludedUuid) && state.ownerUuid.equals(ownerUuid) && state.teamName.equals(teamName)) {
                return true;
            }
        }
        return false;
    }

    // most mobs can keep vanilla combat once a target is assigned the exceptions here are
    // the mobs whose built-in behavior fights the owner-directed summon fantasy unless
    // they are driven explicitly
    private static void driveSummonCombat(ServerLevel level, Mob mob, LivingEntity target, SummonState state, long gameTime) {
        if (mob instanceof Drowned drowned) {
            drowned.setSearchingForLand(false);
            drowned.getNavigation().moveTo(target, Math.max(1.0, state.profile.movementSpeed() * 3.2));
            drowned.getLookControl().setLookAt(target, 30.0f, (float) drowned.getMaxHeadXRot());

            if (gameTime >= state.nextAttackGameTime && drowned.distanceToSqr(target) <= getMeleeReachSqr(drowned, target)) {
                InteractionHand swingHand = drowned.getMainArm() == HumanoidArm.LEFT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                drowned.swing(swingHand);
                if (drowned.doHurtTarget(level, target)) {
                    state.nextAttackGameTime = gameTime + 20L;
                } else {
                    state.nextAttackGameTime = gameTime + 10L;
                }
            }
            return;
        }

        if (mob instanceof Hoglin || mob instanceof Zoglin || mob instanceof AbstractPiglin) {
            stabilizeNetherSummon(mob);
            mob.getNavigation().moveTo(target, Math.max(1.0, state.profile.movementSpeed() * 3.2));
            mob.getLookControl().setLookAt(target, 30.0f, (float) mob.getMaxHeadXRot());

            if (gameTime >= state.nextAttackGameTime && mob.distanceToSqr(target) <= getMeleeReachSqr(mob, target)) {
                InteractionHand swingHand = mob.getMainArm() == HumanoidArm.LEFT ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                mob.swing(swingHand);
                if (mob.doHurtTarget(level, target)) {
                    state.nextAttackGameTime = gameTime + 20L;
                } else {
                    state.nextAttackGameTime = gameTime + 10L;
                }
            }
        }
    }

    // evoker-spawned vexes are tracked as helper summons so recall and cleanup treat them
    // like the rest of the owner's reaped allies
    private static void bindReapedVexes(ServerLevel level, ServerPlayer owner, Evoker evoker, SummonState state) {
        List<Vex> spawnedVexes = level.getEntitiesOfClass(
                Vex.class,
                new AABB(evoker.blockPosition()).inflate(VEX_BIND_RANGE),
                candidate -> candidate.isAlive() && candidate.getOwner() == evoker
        );

        for (int i = MAX_REAPED_VEXES; i < spawnedVexes.size(); i++) {
            Vex excessVex = spawnedVexes.get(i);
            excessVex.discard();
        }

        for (int i = 0; i < Math.min(MAX_REAPED_VEXES, spawnedVexes.size()); i++) {
            Vex vex = spawnedVexes.get(i);
            if (ACTIVE_HELPERS.containsKey(vex.getUUID())) {
                continue;
            }

            vex.setCustomName(Component.translatable("text.moreswordsmod.reaped_vex"));
            vex.setCustomNameVisible(false);
            vex.setPersistenceRequired();
            vex.setCanPickUpLoot(false);

            TeamBinding teamBinding = bindSummonToOwnerTeam(owner, vex);
            ACTIVE_HELPERS.put(
                    vex.getUUID(),
                    new HelperState(
                            level.dimension(),
                            owner.getUUID(),
                            evoker.getUUID(),
                            state.expireGameTime,
                            owner.getScoreboardName(),
                            vex.getScoreboardName(),
                            teamBinding.teamName(),
                            teamBinding.ownerTemporaryTeam()
                    )
            );
        }
    }

    private static double getMeleeReachSqr(Mob mob, LivingEntity target) {
        double reach = mob.getBbWidth() * 1.6 + target.getBbWidth();
        return reach * reach;
    }

    private static void followOwner(ServerPlayer owner, Mob mob, BoneScytheSoulProfile profile) {
        if (mob instanceof Drowned drowned) {
            drowned.setSearchingForLand(false);
        }
        stabilizeNetherSummon(mob);

        mob.getNavigation().moveTo(owner, Math.max(1.0, profile.movementSpeed() * 3.0));
        mob.getLookControl().setLookAt(owner, 10.0f, (float) mob.getMaxHeadXRot());

        if (mob.distanceToSqr(owner) > TELEPORT_DISTANCE_SQR) {
            Vec3 behindOwner = owner.position().subtract(owner.getLookAngle().scale(1.5));
            mob.setPos(behindOwner.x, owner.getY(), behindOwner.z);
            mob.getNavigation().stop();
        }
    }

    // prefers the owner's recent combat context before falling back to nearby hostile mobs
    // this gives the summons implicit command behavior without requiring a dedicated
    // focus-target input
    private static @Nullable LivingEntity resolveTarget(ServerLevel level, ServerPlayer owner, Mob summon) {
        LivingEntity preferredTarget = owner.getLastHurtMob();
        if (isValidCommandTarget(owner, summon, preferredTarget)) {
            return preferredTarget;
        }

        LivingEntity attacker = owner.getLastHurtByMob();
        if (isValidCommandTarget(owner, summon, attacker)) {
            return attacker;
        }

        return level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(summon.blockPosition()).inflate(TARGET_RANGE),
                candidate -> isValidFallbackTarget(owner, summon, candidate)
        ).stream().min((left, right) -> Double.compare(summon.distanceToSqr(left), summon.distanceToSqr(right))).orElse(null);
    }

    private static boolean isValidCommandTarget(ServerPlayer owner, Mob summon, @Nullable LivingEntity candidate) {
        return candidate != null
                && candidate.isAlive()
                && candidate != summon
                && candidate != owner
                && !candidate.isAlliedTo(owner)
                && !candidate.isAlliedTo(summon)
                && !isTrackedReapedAlly(candidate.getUUID());
    }

    private static boolean isValidFallbackTarget(ServerPlayer owner, Mob summon, @Nullable LivingEntity candidate) {
        return isValidCommandTarget(owner, summon, candidate) && candidate instanceof Monster;
    }

    private static boolean isTrackedReapedAlly(UUID uuid) {
        return ACTIVE_SUMMONS.containsKey(uuid) || ACTIVE_HELPERS.containsKey(uuid);
    }

    private static void stabilizeNetherSummon(Mob mob) {
        if (mob instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }

        if (mob instanceof Hoglin hoglin) {
            hoglin.setImmuneToZombification(true);
        }
    }

    private static final class SummonState {
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final UUID ownerUuid;
        private final BoneScytheSoulProfile profile;
        private final long expireGameTime;
        private final String ownerScoreboardName;
        private final String summonScoreboardName;
        private final String teamName;
        private final boolean ownerTemporaryTeam;
        private long nextAttackGameTime;

        private SummonState(
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                UUID ownerUuid,
                BoneScytheSoulProfile profile,
                long expireGameTime,
                String ownerScoreboardName,
                String summonScoreboardName,
                String teamName,
                boolean ownerTemporaryTeam
        ) {
            this.dimension = dimension;
            this.ownerUuid = ownerUuid;
            this.profile = profile;
            this.expireGameTime = expireGameTime;
            this.ownerScoreboardName = ownerScoreboardName;
            this.summonScoreboardName = summonScoreboardName;
            this.teamName = teamName;
            this.ownerTemporaryTeam = ownerTemporaryTeam;
            this.nextAttackGameTime = 0L;
        }
    }

    private static final class HelperState {
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private final UUID ownerUuid;
        private final UUID parentUuid;
        private final long expireGameTime;
        private final String ownerScoreboardName;
        private final String summonScoreboardName;
        private final String teamName;
        private final boolean ownerTemporaryTeam;

        private HelperState(
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                UUID ownerUuid,
                UUID parentUuid,
                long expireGameTime,
                String ownerScoreboardName,
                String summonScoreboardName,
                String teamName,
                boolean ownerTemporaryTeam
        ) {
            this.dimension = dimension;
            this.ownerUuid = ownerUuid;
            this.parentUuid = parentUuid;
            this.expireGameTime = expireGameTime;
            this.ownerScoreboardName = ownerScoreboardName;
            this.summonScoreboardName = summonScoreboardName;
            this.teamName = teamName;
            this.ownerTemporaryTeam = ownerTemporaryTeam;
        }
    }

    private record TeamBinding(String teamName, boolean ownerTemporaryTeam) {
    }

    public record RecallResult(int recalledSummons, int totalWeight, int refundedCharges) {
    }
}
