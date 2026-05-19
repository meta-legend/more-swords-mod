package net.metalegend.moreswordsmod.soul;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.metalegend.moreswordsmod.enchantment.ModEnchantments;
import net.metalegend.moreswordsmod.item.custom.BoneScytheItem;
import net.metalegend.moreswordsmod.mixin.MobAccessorMixin;
import net.metalegend.moreswordsmod.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// server-side owner for reaped summon lifecycle
// BoneScytheItem stores charges imprints and summon-window state on the item stack
// this manager owns the summoned entities themselves: spawn setup team binding
// ticking recall and cleanup
public final class ReapedSoulManager {
    private static final String TEMP_TEAM_PREFIX = "mws_reaped_";
    private static final String REAPED_PRIMARY_TAG = "moreswordsmod.reaped.primary";
    private static final String REAPED_HELPER_TAG = "moreswordsmod.reaped.helper";
    private static final String REAPED_OWNER_TAG_PREFIX = "moreswordsmod.reaped.owner.";
    private static final String REAPED_TOKEN_TAG_PREFIX = "moreswordsmod.reaped.token.";
    private static final int SUMMON_LIFETIME_TICKS = 300;
    private static final int OSSUARY_LIFETIME_BONUS_TICKS = 100;
    private static final int MAX_REAPED_VEXES = 2;
    private static final int GRAVE_BOUND_LINK_INTERVAL_TICKS = 5;
    private static final int GRAVE_BOUND_LINK_POINTS = 6;
    private static final double GRAVE_BOUND_LINK_DISTANCE_SQR = 2304.0;
    private static final double GRAVE_BOUND_SWIRL_RADIUS = 0.12;
    private static final int RECALL_SMOKE_PARTICLES = 18;
    private static final int RECALL_SOUL_PARTICLES = 7;
    private static final float RECALL_DISSIPATE_VOLUME = 0.55f;
    private static final float RECALL_DISSIPATE_PITCH = 1.15f;
    private static final double GARRISON_TARGET_RANGE = 18.0;
    private static final double GARRISON_RING_RADIUS = 1.75;
    private static final int GARRISON_PARTICLES = 14;
    private static final double TARGET_RANGE = 16.0;
    private static final double TELEPORT_DISTANCE_SQR = 144.0;
    private static final double VEX_BIND_RANGE = 24.0;
    private static final int ORPHAN_CLEANUP_INTERVAL_TICKS = 20;
    private static final long NOT_MISSING_GAME_TIME = -1L;
    private static final Map<UUID, SummonState> ACTIVE_SUMMONS = new HashMap<>();
    private static final Map<UUID, HelperState> ACTIVE_HELPERS = new HashMap<>();

    private ReapedSoulManager() {
    }

    // registers the global server tick that drives summon lifetime following and combat
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ReapedSoulManager::tickServer);
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
        int refundableWeight = 0;
        long gameTime = owner.level().getServer().overworld().getGameTime();

        Iterator<Map.Entry<UUID, SummonState>> summonIterator = ACTIVE_SUMMONS.entrySet().iterator();
        while (summonIterator.hasNext()) {
            Map.Entry<UUID, SummonState> entry = summonIterator.next();
            SummonState state = entry.getValue();
            if (!state.ownerUuid.equals(owner.getUUID())) {
                continue;
            }

            MinecraftServer server = owner.level().getServer();
            LocatedEntity<Entity> locatedEntity = findTrackedEntity(server, entry.getKey(), state.trackingToken, Entity.class);
            if (locatedEntity != null) {
                state.dimension = locatedEntity.level().dimension();
                UUID currentUuid = locatedEntity.entity().getUUID();
                state.summonScoreboardName = locatedEntity.entity().getScoreboardName();
                dissipateRecalledEntity(locatedEntity.level(), locatedEntity.entity());
                cleanupTeamBinding(
                        locatedEntity.level(),
                        currentUuid,
                        state.ownerUuid,
                        state.teamName,
                        state.ownerScoreboardName,
                        state.summonScoreboardName,
                        state.ownerTemporaryTeam
                );
            } else {
                cleanupStoredTeamBinding(server, entry.getKey(), state);
            }

            recalledSummons++;
            totalWeight += state.profile.weight();
            if (isRecallRefundEligible(state, gameTime)) {
                refundableWeight += state.profile.weight();
            }
            summonIterator.remove();
        }

        Iterator<Map.Entry<UUID, HelperState>> helperIterator = ACTIVE_HELPERS.entrySet().iterator();
        while (helperIterator.hasNext()) {
            Map.Entry<UUID, HelperState> entry = helperIterator.next();
            HelperState state = entry.getValue();
            if (!state.ownerUuid.equals(owner.getUUID())) {
                continue;
            }

            MinecraftServer server = owner.level().getServer();
            LocatedEntity<Entity> locatedEntity = findTrackedEntity(server, entry.getKey(), state.trackingToken, Entity.class);
            if (locatedEntity != null) {
                state.dimension = locatedEntity.level().dimension();
                UUID currentUuid = locatedEntity.entity().getUUID();
                state.summonScoreboardName = locatedEntity.entity().getScoreboardName();
                dissipateRecalledEntity(locatedEntity.level(), locatedEntity.entity());
                cleanupTeamBinding(
                        locatedEntity.level(),
                        currentUuid,
                        state.ownerUuid,
                        state.teamName,
                        state.ownerScoreboardName,
                        state.summonScoreboardName,
                        state.ownerTemporaryTeam
                );
            } else {
                cleanupStoredTeamBinding(server, entry.getKey(), state);
            }

            helperIterator.remove();
        }

        return new RecallResult(recalledSummons, totalWeight, refundableWeight, Math.round(refundableWeight * 0.5f));
    }

    private static boolean isRecallRefundEligible(SummonState state, long gameTime) {
        return gameTime - state.spawnGameTime < state.lifetimeTicks / 2L;
    }

    public static GarrisonResult garrisonSummons(ServerPlayer owner) {
        MinecraftServer server = owner.level().getServer();
        long gameTime = server.overworld().getGameTime();
        ServerLevel ownerLevel = (ServerLevel) owner.level();
        LivingEntity focusTarget = resolveGarrisonFocusTarget(ownerLevel, owner);
        int garrisonedSummons = 0;
        int failedSummons = 0;
        int garrisonIndex = 0;
        Map<UUID, SummonState> remappedSummons = new HashMap<>();
        Map<UUID, HelperState> remappedHelpers = new HashMap<>();

        Iterator<Map.Entry<UUID, SummonState>> summonIterator = ACTIVE_SUMMONS.entrySet().iterator();
        while (summonIterator.hasNext()) {
            Map.Entry<UUID, SummonState> entry = summonIterator.next();
            SummonState state = entry.getValue();
            if (!state.ownerUuid.equals(owner.getUUID())) {
                continue;
            }

            LocatedEntity<Mob> locatedEntity = findTrackedEntity(server, entry.getKey(), state.trackingToken, Mob.class);
            if (locatedEntity == null) {
                if (gameTime < state.expireGameTime) {
                    UUID oldUuid = entry.getKey();
                    UUID oldTrackingToken = state.trackingToken;
                    Vec3 garrisonPosition = getGarrisonPosition(owner, garrisonIndex++);
                    removeHelpersForParent(server, oldUuid, oldTrackingToken);
                    Mob recreatedMob = recreateMissingSummonAtGarrison(server, ownerLevel, owner, oldUuid, state, garrisonPosition);
                    if (recreatedMob == null) {
                        shouldKeepMissingSummonState(state, gameTime);
                        failedSummons++;
                        continue;
                    }

                    recreatedMob.getNavigation().stop();
                    recreatedMob.setTarget(focusTarget);
                    spawnGarrisonArrivalParticles(ownerLevel, recreatedMob);
                    garrisonedSummons++;
                    summonIterator.remove();
                    remappedSummons.put(recreatedMob.getUUID(), state);
                    continue;
                }

                if (shouldKeepMissingSummonState(state, gameTime)) {
                    failedSummons++;
                    continue;
                }

                cleanupStoredTeamBinding(server, entry.getKey(), state);
                summonIterator.remove();
                continue;
            }

            if (!locatedEntity.entity().isAlive()) {
                cleanupStoredTeamBinding(server, entry.getKey(), state);
                summonIterator.remove();
                continue;
            }

            Mob mob = locatedEntity.entity();
            state.missingSinceGameTime = NOT_MISSING_GAME_TIME;
            markPrimarySummon(mob, state.ownerUuid, state.trackingToken);
            Vec3 garrisonPosition = getGarrisonPosition(owner, garrisonIndex++);
            if (!teleportSummonToGarrison(ownerLevel, owner, mob, garrisonPosition)) {
                failedSummons++;
                continue;
            }

            Mob movedMob = findTrackedEntityInLevel(ownerLevel, state.trackingToken, Mob.class);
            if (movedMob != null) {
                mob = movedMob;
            }

            state.dimension = ownerLevel.dimension();
            state.summonScoreboardName = mob.getScoreboardName();
            refreshPrimarySummonRuntimeState(mob, state);
            ensurePrimaryTeamBinding(ownerLevel, state, owner, mob);
            mob.getNavigation().stop();
            mob.setTarget(focusTarget);
            spawnGarrisonArrivalParticles(ownerLevel, mob);
            garrisonedSummons++;
            if (!entry.getKey().equals(mob.getUUID())) {
                summonIterator.remove();
                remappedSummons.put(mob.getUUID(), state);
            }
        }

        Iterator<Map.Entry<UUID, HelperState>> helperIterator = ACTIVE_HELPERS.entrySet().iterator();
        while (helperIterator.hasNext()) {
            Map.Entry<UUID, HelperState> entry = helperIterator.next();
            HelperState state = entry.getValue();
            if (!state.ownerUuid.equals(owner.getUUID())) {
                continue;
            }

            LocatedEntity<Vex> locatedEntity = findTrackedEntity(server, entry.getKey(), state.trackingToken, Vex.class);
            if (locatedEntity == null) {
                if (shouldKeepMissingHelperState(state, gameTime)) {
                    failedSummons++;
                    continue;
                }

                cleanupStoredTeamBinding(server, entry.getKey(), state);
                helperIterator.remove();
                continue;
            }

            if (!locatedEntity.entity().isAlive()) {
                cleanupStoredTeamBinding(server, entry.getKey(), state);
                helperIterator.remove();
                continue;
            }

            Vex vex = locatedEntity.entity();
            state.missingSinceGameTime = NOT_MISSING_GAME_TIME;
            markHelperSummon(vex, state.ownerUuid, state.trackingToken);
            Vec3 garrisonPosition = getGarrisonPosition(owner, garrisonIndex++);
            if (!teleportSummonToGarrison(ownerLevel, owner, vex, garrisonPosition)) {
                failedSummons++;
                continue;
            }

            Vex movedVex = findTrackedEntityInLevel(ownerLevel, state.trackingToken, Vex.class);
            if (movedVex != null) {
                vex = movedVex;
            }

            state.dimension = ownerLevel.dimension();
            state.summonScoreboardName = vex.getScoreboardName();
            refreshHelperSummonRuntimeState(vex);
            ensureHelperTeamBinding(ownerLevel, state, owner, vex);
            vex.getNavigation().stop();
            vex.setTarget(focusTarget);
            spawnGarrisonArrivalParticles(ownerLevel, vex);
            garrisonedSummons++;
            if (!entry.getKey().equals(vex.getUUID())) {
                helperIterator.remove();
                remappedHelpers.put(vex.getUUID(), state);
            }
        }

        ACTIVE_SUMMONS.putAll(remappedSummons);
        ACTIVE_HELPERS.putAll(remappedHelpers);
        return new GarrisonResult(garrisonedSummons, failedSummons, focusTarget != null);
    }

    private static Vec3 getGarrisonPosition(ServerPlayer owner, int index) {
        double angle = Math.toRadians(owner.getYRot()) + index * ((Math.PI * 2.0) / 3.0);
        return owner.position().add(Math.cos(angle) * GARRISON_RING_RADIUS, 0.0, Math.sin(angle) * GARRISON_RING_RADIUS);
    }

    private static boolean teleportSummonToGarrison(ServerLevel ownerLevel, ServerPlayer owner, Mob mob, Vec3 position) {
        stabilizeNetherSummon(mob);
        if (mob.level().dimension().equals(ownerLevel.dimension())) {
            mob.teleportTo(position.x, owner.getY(), position.z);
            return true;
        }

        return mob.teleportTo(ownerLevel, position.x, owner.getY(), position.z, Set.of(), mob.getYRot(), mob.getXRot(), true);
    }

    private static @Nullable Mob recreateMissingSummonAtGarrison(MinecraftServer server, ServerLevel ownerLevel, ServerPlayer owner, UUID oldUuid, SummonState state, Vec3 position) {
        Entity entity = state.profile.sourceType().create(ownerLevel, EntitySpawnReason.MOB_SUMMONED);
        if (!(entity instanceof Mob mob)) {
            return null;
        }

        mob.setPos(position.x, owner.getY(), position.z);
        mob.setCustomName(state.profile.summonName());
        mob.setCustomNameVisible(false);
        mob.setGlowingTag(true);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        applyProfileStats(mob, state.imprint);
        applyCapturedEquipment(mob, state.imprint);
        overrideSpecialSummonAi(mob);

        if (!ownerLevel.addFreshEntity(mob)) {
            return null;
        }

        cleanupStoredTeamBinding(server, oldUuid, state);
        TeamBinding teamBinding = bindSummonToOwnerTeam(owner, mob);
        state.dimension = ownerLevel.dimension();
        state.trackingToken = UUID.randomUUID();
        state.ownerScoreboardName = owner.getScoreboardName();
        state.summonScoreboardName = mob.getScoreboardName();
        state.teamName = teamBinding.teamName();
        state.ownerTemporaryTeam = teamBinding.ownerTemporaryTeam();
        state.missingSinceGameTime = NOT_MISSING_GAME_TIME;
        markPrimarySummon(mob, state.ownerUuid, state.trackingToken);
        return mob;
    }

    private static void spawnGarrisonArrivalParticles(ServerLevel level, Mob mob) {
        Vec3 center = mob.position().add(0.0, mob.getBbHeight() * 0.45, 0.0);
        double spread = Math.max(0.15, mob.getBbWidth() * 0.35);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, GARRISON_PARTICLES, spread, 0.3, spread, 0.02);
    }

    private static @Nullable LivingEntity resolveGarrisonFocusTarget(ServerLevel level, ServerPlayer owner) {
        LivingEntity preferredTarget = owner.getLastHurtMob();
        if (isValidOwnerFocusTarget(owner, preferredTarget)) {
            return preferredTarget;
        }

        LivingEntity attacker = owner.getLastHurtByMob();
        if (isValidOwnerFocusTarget(owner, attacker)) {
            return attacker;
        }

        return level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(owner.blockPosition()).inflate(GARRISON_TARGET_RANGE),
                candidate -> candidate instanceof Monster && isValidOwnerFocusTarget(owner, candidate)
        ).stream().min((left, right) -> Double.compare(owner.distanceToSqr(left), owner.distanceToSqr(right))).orElse(null);
    }

    private static boolean isValidOwnerFocusTarget(ServerPlayer owner, @Nullable LivingEntity candidate) {
        return candidate != null
                && candidate.isAlive()
                && candidate != owner
                && candidate.level().dimension().equals(owner.level().dimension())
                && !candidate.isAlliedTo(owner)
                && !isTrackedReapedAlly(candidate);
    }

    private static void dissipateRecalledEntity(ServerLevel level, Entity entity) {
        Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        double horizontalSpread = Math.max(0.15, entity.getBbWidth() * 0.35);
        double verticalSpread = Math.max(0.2, entity.getBbHeight() * 0.25);

        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, RECALL_SMOKE_PARTICLES, horizontalSpread, verticalSpread, horizontalSpread, 0.02);
        level.sendParticles(ParticleTypes.SOUL, center.x, center.y, center.z, RECALL_SOUL_PARTICLES, horizontalSpread * 0.7, verticalSpread * 0.8, horizontalSpread * 0.7, 0.01);
        level.playSound(null, entity.blockPosition(), ModSounds.BONE_SCYTHE_REAPED_SOUL_DISSIPATE, SoundSource.PLAYERS, RECALL_DISSIPATE_VOLUME, RECALL_DISSIPATE_PITCH);
        entity.discard();
    }

    public static boolean isActiveSummon(LivingEntity entity) {
        return isTrackedReapedAlly(entity);
    }

    public static boolean isReapedFriendlyContact(Slime slime, LivingEntity target) {
        return isTrackedReapedAlly(slime) && target.isAlliedTo(slime);
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
            UUID childTrackingToken = UUID.randomUUID();
            markPrimarySummon(child, parentState.ownerUuid, childTrackingToken);
            ACTIVE_SUMMONS.put(
                    child.getUUID(),
                    new SummonState(
                            parentState.dimension,
                            parentState.ownerUuid,
                            parentState.profile,
                            ReapedSoulImprint.capture(parentState.profile, child),
                            childTrackingToken,
                            parentState.spawnGameTime,
                            parentState.lifetimeTicks,
                            parentState.expireGameTime,
                            parentState.ownerScoreboardName,
                            child.getScoreboardName(),
                            parentState.teamName,
                            parentState.ownerTemporaryTeam
                    )
            );
        }
    }

    public static boolean summon(ServerLevel level, ServerPlayer owner, ReapedSoulImprint imprint, Vec3 spawnPos, ItemStack scytheStack) {
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

        long spawnGameTime = level.getServer().overworld().getGameTime();
        int summonLifetimeTicks = getSummonLifetimeTicks(owner, scytheStack);
        TeamBinding teamBinding = bindSummonToOwnerTeam(owner, mob);
        UUID trackingToken = UUID.randomUUID();
        markPrimarySummon(mob, owner.getUUID(), trackingToken);
        ACTIVE_SUMMONS.put(
                mob.getUUID(),
                new SummonState(
                        level.dimension(),
                        owner.getUUID(),
                        profile,
                        imprint,
                        trackingToken,
                        spawnGameTime,
                        summonLifetimeTicks,
                        spawnGameTime + summonLifetimeTicks,
                        owner.getScoreboardName(),
                        mob.getScoreboardName(),
                        teamBinding.teamName(),
                        teamBinding.ownerTemporaryTeam()
                )
        );
        return true;
    }

    private static void tickServer(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        tickPrimarySummons(server, gameTime);
        tickHelpers(server, gameTime);
        if (gameTime % ORPHAN_CLEANUP_INTERVAL_TICKS == 0) {
            cleanupUntrackedReapedEntities(server);
        }
    }

    private static void tickPrimarySummons(MinecraftServer server, long gameTime) {
        Iterator<Map.Entry<UUID, SummonState>> iterator = ACTIVE_SUMMONS.entrySet().iterator();
        Map<UUID, SummonState> remappedSummons = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<UUID, SummonState> entry = iterator.next();
            SummonState state = entry.getValue();

            LocatedEntity<Mob> locatedEntity = findTrackedEntity(server, entry.getKey(), state.trackingToken, Mob.class);
            if (locatedEntity == null) {
                if (shouldKeepMissingSummonState(state, gameTime)) {
                    continue;
                }

                cleanupStoredTeamBinding(server, entry.getKey(), state);
                iterator.remove();
                continue;
            }

            if (!locatedEntity.entity().isAlive()) {
                cleanupStoredTeamBinding(server, entry.getKey(), state);
                iterator.remove();
                continue;
            }

            ServerLevel level = locatedEntity.level();
            Mob mob = locatedEntity.entity();
            UUID currentUuid = mob.getUUID();
            state.dimension = level.dimension();
            state.missingSinceGameTime = NOT_MISSING_GAME_TIME;
            state.summonScoreboardName = mob.getScoreboardName();
            markPrimarySummon(mob, state.ownerUuid, state.trackingToken);
            refreshPrimarySummonRuntimeState(mob, state);

            ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerUuid);
            if (owner == null || !owner.isAlive() || gameTime >= state.expireGameTime) {
                mob.discard();
                cleanupTeamBinding(level, currentUuid, state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            ensurePrimaryTeamBinding(level, state, owner, mob);
            if (owner.level().dimension().equals(level.dimension())) {
                LivingEntity target = resolveTarget(level, owner, mob);
                spawnGraveBoundLinkIfNeeded(level, owner, mob, gameTime);
                if (target != null) {
                    mob.setTarget(target);
                    driveSummonCombat(level, mob, target, state, gameTime);
                } else {
                    mob.setTarget(null);
                    followOwner(owner, mob, state.profile);
                }
            } else {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }

            if (mob instanceof Evoker evoker) {
                bindReapedVexes(level, owner, evoker, state);
            }

            if (!entry.getKey().equals(currentUuid)) {
                iterator.remove();
                remappedSummons.put(currentUuid, state);
            }
        }

        ACTIVE_SUMMONS.putAll(remappedSummons);
    }

    private static void tickHelpers(MinecraftServer server, long gameTime) {
        Iterator<Map.Entry<UUID, HelperState>> iterator = ACTIVE_HELPERS.entrySet().iterator();
        Map<UUID, HelperState> remappedHelpers = new HashMap<>();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HelperState> entry = iterator.next();
            HelperState state = entry.getValue();

            LocatedEntity<Vex> locatedEntity = findTrackedEntity(server, entry.getKey(), state.trackingToken, Vex.class);
            if (locatedEntity == null) {
                if (shouldKeepMissingHelperState(state, gameTime)) {
                    continue;
                }

                cleanupStoredTeamBinding(server, entry.getKey(), state);
                iterator.remove();
                continue;
            }

            if (!locatedEntity.entity().isAlive()) {
                cleanupStoredTeamBinding(server, entry.getKey(), state);
                iterator.remove();
                continue;
            }

            ServerLevel level = locatedEntity.level();
            Vex vex = locatedEntity.entity();
            UUID currentUuid = vex.getUUID();
            state.dimension = level.dimension();
            state.missingSinceGameTime = NOT_MISSING_GAME_TIME;
            state.summonScoreboardName = vex.getScoreboardName();
            markHelperSummon(vex, state.ownerUuid, state.trackingToken);
            refreshHelperSummonRuntimeState(vex);

            LocatedEntity<Evoker> parentEntity = findTrackedEntity(server, state.parentUuid, state.parentTrackingToken, Evoker.class);
            if (parentEntity == null || !parentEntity.entity().isAlive() || !parentEntity.level().dimension().equals(level.dimension()) || gameTime >= state.expireGameTime) {
                vex.discard();
                cleanupTeamBinding(level, currentUuid, state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            ServerPlayer owner = server.getPlayerList().getPlayer(state.ownerUuid);
            if (owner == null || !owner.isAlive()) {
                vex.discard();
                cleanupTeamBinding(level, currentUuid, state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
                iterator.remove();
                continue;
            }

            Evoker evoker = parentEntity.entity();
            ensureHelperTeamBinding(level, state, owner, vex);
            vex.setCustomName(Component.translatable("text.moreswordsmod.reaped_vex"));
            vex.setCustomNameVisible(false);
            vex.setPersistenceRequired();
            vex.setCanPickUpLoot(false);

            LivingEntity parentTarget = evoker.getTarget();
            if (parentTarget != null && parentTarget.isAlive() && !parentTarget.isAlliedTo(owner) && !isTrackedReapedAlly(parentTarget)) {
                vex.setTarget(parentTarget);
            }

            if (!entry.getKey().equals(currentUuid)) {
                iterator.remove();
                remappedHelpers.put(currentUuid, state);
            }
        }

        ACTIVE_HELPERS.putAll(remappedHelpers);
    }

    private static boolean shouldKeepMissingSummonState(SummonState state, long gameTime) {
        if (state.missingSinceGameTime == NOT_MISSING_GAME_TIME) {
            state.missingSinceGameTime = gameTime;
        }

        return gameTime < state.expireGameTime;
    }

    private static boolean shouldKeepMissingHelperState(HelperState state, long gameTime) {
        if (state.missingSinceGameTime == NOT_MISSING_GAME_TIME) {
            state.missingSinceGameTime = gameTime;
        }

        return gameTime < state.expireGameTime;
    }

    private static void removeHelpersForParent(MinecraftServer server, UUID parentUuid, UUID parentTrackingToken) {
        Iterator<Map.Entry<UUID, HelperState>> iterator = ACTIVE_HELPERS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, HelperState> entry = iterator.next();
            HelperState state = entry.getValue();
            if (!state.parentUuid.equals(parentUuid) && !state.parentTrackingToken.equals(parentTrackingToken)) {
                continue;
            }

            cleanupStoredTeamBinding(server, entry.getKey(), state);
            iterator.remove();
        }
    }

    private static void refreshPrimarySummonRuntimeState(Mob mob, SummonState state) {
        mob.setCustomName(state.profile.summonName());
        mob.setCustomNameVisible(false);
        mob.setGlowingTag(true);
        mob.setPersistenceRequired();
        mob.setCanPickUpLoot(false);
        overrideSpecialSummonAi(mob);
    }

    private static void refreshHelperSummonRuntimeState(Vex vex) {
        vex.setCustomName(Component.translatable("text.moreswordsmod.reaped_vex"));
        vex.setCustomNameVisible(false);
        vex.setPersistenceRequired();
        vex.setCanPickUpLoot(false);
        stripNaturalTargeting(vex);
    }

    private static void cleanupUntrackedReapedEntities(MinecraftServer server) {
        List<Entity> orphanedEntities = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (hasReapedTrackingTag(entity) && !hasActiveTrackingState(entity)) {
                    orphanedEntities.add(entity);
                }
            }
        }

        for (Entity entity : orphanedEntities) {
            if (entity.level() instanceof ServerLevel level) {
                cleanupOrphanedScoreboardEntry(level, entity);
            }
            entity.discard();
        }
    }

    private static boolean hasReapedTrackingTag(Entity entity) {
        return entity.entityTags().contains(REAPED_PRIMARY_TAG) || entity.entityTags().contains(REAPED_HELPER_TAG);
    }

    private static boolean hasActiveTrackingState(Entity entity) {
        if (ACTIVE_SUMMONS.containsKey(entity.getUUID()) || ACTIVE_HELPERS.containsKey(entity.getUUID())) {
            return true;
        }

        Set<String> tags = entity.entityTags();
        for (SummonState state : ACTIVE_SUMMONS.values()) {
            if (tags.contains(tokenTag(state.trackingToken))) {
                return true;
            }
        }
        for (HelperState state : ACTIVE_HELPERS.values()) {
            if (tags.contains(tokenTag(state.trackingToken))) {
                return true;
            }
        }

        return false;
    }

    private static void cleanupOrphanedScoreboardEntry(ServerLevel level, Entity entity) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayersTeam(entity.getScoreboardName());
        if (team != null) {
            scoreboard.removePlayerFromTeam(entity.getScoreboardName(), team);
        }
    }

    private static int getSummonLifetimeTicks(ServerPlayer owner, ItemStack scytheStack) {
        int ossuaryLevel = owner.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(ModEnchantments.OSSUARY)
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, scytheStack))
                .orElse(0);

        return SUMMON_LIFETIME_TICKS + ossuaryLevel * OSSUARY_LIFETIME_BONUS_TICKS;
    }

    private static void spawnGraveBoundLinkIfNeeded(ServerLevel level, ServerPlayer owner, LivingEntity summon, long gameTime) {
        if (gameTime % GRAVE_BOUND_LINK_INTERVAL_TICKS != 0 || !isHoldingBoneScythe(owner) || owner.distanceToSqr(summon) > GRAVE_BOUND_LINK_DISTANCE_SQR) {
            return;
        }

        Vec3 start = owner.position().add(0.0, owner.getBbHeight() * 0.65, 0.0);
        Vec3 end = summon.position().add(0.0, summon.getBbHeight() * 0.55, 0.0);
        Vec3 link = end.subtract(start);
        if (link.lengthSqr() < 1.0E-6) {
            return;
        }

        Vec3 side = new Vec3(-link.z, 0.0, link.x);
        if (side.lengthSqr() < 1.0E-6) {
            side = new Vec3(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }

        for (int i = 1; i <= GRAVE_BOUND_LINK_POINTS; i++) {
            double progress = i / (double) (GRAVE_BOUND_LINK_POINTS + 1);
            double phase = gameTime * 0.35 + i * 1.4;
            Vec3 swirl = side.scale(Math.sin(phase) * GRAVE_BOUND_SWIRL_RADIUS).add(0.0, Math.cos(phase * 0.7) * 0.05, 0.0);
            Vec3 point = start.add(link.scale(progress)).add(swirl);
            level.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 1, 0.01, 0.01, 0.01, 0.0);
        }
    }

    private static boolean isHoldingBoneScythe(ServerPlayer owner) {
        return owner.getMainHandItem().getItem() instanceof BoneScytheItem
                || owner.getOffhandItem().getItem() instanceof BoneScytheItem;
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
        stripNaturalTargeting(mob);

        if (mob instanceof Drowned drowned) {
            drowned.removeAllGoals(goal -> {
                String goalName = goal.getClass().getSimpleName();
                return goalName.startsWith("Drowned") || goalName.equals("RandomStrollGoal");
            });
            drowned.setSearchingForLand(false);
        }

        stabilizeNetherSummon(mob);
    }

    private static void stripNaturalTargeting(Mob mob) {
        ((MobAccessorMixin) mob).moreswordsmod$getTargetSelector().removeAllGoals(goal -> true);
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

    private static void cleanupStoredTeamBinding(MinecraftServer server, UUID allyUuid, SummonState state) {
        ServerLevel level = server.getLevel(state.dimension);
        if (level != null) {
            cleanupTeamBinding(level, allyUuid, state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
        }
    }

    private static void cleanupStoredTeamBinding(MinecraftServer server, UUID allyUuid, HelperState state) {
        ServerLevel level = server.getLevel(state.dimension);
        if (level != null) {
            cleanupTeamBinding(level, allyUuid, state.ownerUuid, state.teamName, state.ownerScoreboardName, state.summonScoreboardName, state.ownerTemporaryTeam);
        }
    }

    private static void ensurePrimaryTeamBinding(ServerLevel level, SummonState state, ServerPlayer owner, Mob summon) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(state.teamName);
        if (team == null) {
            TeamBinding teamBinding = bindSummonToOwnerTeam(owner, summon);
            state.ownerScoreboardName = owner.getScoreboardName();
            state.summonScoreboardName = summon.getScoreboardName();
            state.teamName = teamBinding.teamName();
            state.ownerTemporaryTeam = teamBinding.ownerTemporaryTeam();
            return;
        }

        if (scoreboard.getPlayersTeam(summon.getScoreboardName()) != team) {
            scoreboard.addPlayerToTeam(summon.getScoreboardName(), team);
        }
        if (state.ownerTemporaryTeam && scoreboard.getPlayersTeam(owner.getScoreboardName()) != team) {
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        }

        state.ownerScoreboardName = owner.getScoreboardName();
        state.summonScoreboardName = summon.getScoreboardName();
    }

    private static void ensureHelperTeamBinding(ServerLevel level, HelperState state, ServerPlayer owner, Vex vex) {
        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(state.teamName);
        if (team == null) {
            TeamBinding teamBinding = bindSummonToOwnerTeam(owner, vex);
            state.ownerScoreboardName = owner.getScoreboardName();
            state.summonScoreboardName = vex.getScoreboardName();
            state.teamName = teamBinding.teamName();
            state.ownerTemporaryTeam = teamBinding.ownerTemporaryTeam();
            return;
        }

        if (scoreboard.getPlayersTeam(vex.getScoreboardName()) != team) {
            scoreboard.addPlayerToTeam(vex.getScoreboardName(), team);
        }
        if (state.ownerTemporaryTeam && scoreboard.getPlayersTeam(owner.getScoreboardName()) != team) {
            scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        }

        state.ownerScoreboardName = owner.getScoreboardName();
        state.summonScoreboardName = vex.getScoreboardName();
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
            UUID helperTrackingToken = UUID.randomUUID();
            markHelperSummon(vex, owner.getUUID(), helperTrackingToken);
            ACTIVE_HELPERS.put(
                    vex.getUUID(),
                    new HelperState(
                            level.dimension(),
                            owner.getUUID(),
                            evoker.getUUID(),
                            state.trackingToken,
                            helperTrackingToken,
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
                && !isTrackedReapedAlly(candidate);
    }

    private static boolean isValidFallbackTarget(ServerPlayer owner, Mob summon, @Nullable LivingEntity candidate) {
        return isValidCommandTarget(owner, summon, candidate) && candidate instanceof Monster;
    }

    private static boolean isTrackedReapedAlly(Entity entity) {
        return isTrackedReapedAlly(entity.getUUID())
                || entity.entityTags().contains(REAPED_PRIMARY_TAG)
                || entity.entityTags().contains(REAPED_HELPER_TAG);
    }

    private static boolean isTrackedReapedAlly(UUID uuid) {
        return ACTIVE_SUMMONS.containsKey(uuid) || ACTIVE_HELPERS.containsKey(uuid);
    }

    private static void markPrimarySummon(Entity entity, UUID ownerUuid, UUID trackingToken) {
        entity.addTag(REAPED_PRIMARY_TAG);
        entity.addTag(ownerTag(ownerUuid));
        entity.addTag(tokenTag(trackingToken));
    }

    private static void markHelperSummon(Entity entity, UUID ownerUuid, UUID trackingToken) {
        entity.addTag(REAPED_HELPER_TAG);
        entity.addTag(ownerTag(ownerUuid));
        entity.addTag(tokenTag(trackingToken));
    }

    private static String ownerTag(UUID ownerUuid) {
        return REAPED_OWNER_TAG_PREFIX + ownerUuid;
    }

    private static String tokenTag(UUID trackingToken) {
        return REAPED_TOKEN_TAG_PREFIX + trackingToken;
    }

    private static <T extends Entity> @Nullable LocatedEntity<T> findTrackedEntity(MinecraftServer server, UUID uuid, UUID trackingToken, Class<T> entityClass) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entityClass.isInstance(entity) && !entity.isRemoved()) {
                return new LocatedEntity<>(level, entityClass.cast(entity));
            }
        }

        String tokenTag = tokenTag(trackingToken);
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entityClass.isInstance(entity) && !entity.isRemoved() && entity.entityTags().contains(tokenTag)) {
                    return new LocatedEntity<>(level, entityClass.cast(entity));
                }
            }
        }

        return null;
    }

    private static <T extends Entity> @Nullable T findTrackedEntityInLevel(ServerLevel level, UUID trackingToken, Class<T> entityClass) {
        String tokenTag = tokenTag(trackingToken);
        for (Entity entity : level.getAllEntities()) {
            if (entityClass.isInstance(entity) && !entity.isRemoved() && entity.entityTags().contains(tokenTag)) {
                return entityClass.cast(entity);
            }
        }

        return null;
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
        private ResourceKey<Level> dimension;
        private final UUID ownerUuid;
        private final BoneScytheSoulProfile profile;
        private final ReapedSoulImprint imprint;
        private UUID trackingToken;
        private final long spawnGameTime;
        private final int lifetimeTicks;
        private final long expireGameTime;
        private String ownerScoreboardName;
        private String summonScoreboardName;
        private String teamName;
        private boolean ownerTemporaryTeam;
        private long nextAttackGameTime;
        private long missingSinceGameTime;

        private SummonState(
                ResourceKey<Level> dimension,
                UUID ownerUuid,
                BoneScytheSoulProfile profile,
                ReapedSoulImprint imprint,
                UUID trackingToken,
                long spawnGameTime,
                int lifetimeTicks,
                long expireGameTime,
                String ownerScoreboardName,
                String summonScoreboardName,
                String teamName,
                boolean ownerTemporaryTeam
        ) {
            this.dimension = dimension;
            this.ownerUuid = ownerUuid;
            this.profile = profile;
            this.imprint = imprint;
            this.trackingToken = trackingToken;
            this.spawnGameTime = spawnGameTime;
            this.lifetimeTicks = lifetimeTicks;
            this.expireGameTime = expireGameTime;
            this.ownerScoreboardName = ownerScoreboardName;
            this.summonScoreboardName = summonScoreboardName;
            this.teamName = teamName;
            this.ownerTemporaryTeam = ownerTemporaryTeam;
            this.nextAttackGameTime = 0L;
            this.missingSinceGameTime = NOT_MISSING_GAME_TIME;
        }
    }

    private static final class HelperState {
        private ResourceKey<Level> dimension;
        private final UUID ownerUuid;
        private final UUID parentUuid;
        private final UUID parentTrackingToken;
        private final UUID trackingToken;
        private final long expireGameTime;
        private String ownerScoreboardName;
        private String summonScoreboardName;
        private String teamName;
        private boolean ownerTemporaryTeam;
        private long missingSinceGameTime;

        private HelperState(
                ResourceKey<Level> dimension,
                UUID ownerUuid,
                UUID parentUuid,
                UUID parentTrackingToken,
                UUID trackingToken,
                long expireGameTime,
                String ownerScoreboardName,
                String summonScoreboardName,
                String teamName,
                boolean ownerTemporaryTeam
        ) {
            this.dimension = dimension;
            this.ownerUuid = ownerUuid;
            this.parentUuid = parentUuid;
            this.parentTrackingToken = parentTrackingToken;
            this.trackingToken = trackingToken;
            this.expireGameTime = expireGameTime;
            this.ownerScoreboardName = ownerScoreboardName;
            this.summonScoreboardName = summonScoreboardName;
            this.teamName = teamName;
            this.ownerTemporaryTeam = ownerTemporaryTeam;
            this.missingSinceGameTime = NOT_MISSING_GAME_TIME;
        }
    }

    private record TeamBinding(String teamName, boolean ownerTemporaryTeam) {
    }

    private record LocatedEntity<T extends Entity>(ServerLevel level, T entity) {
    }

    public record RecallResult(int recalledSummons, int totalWeight, int refundableWeight, int refundedCharges) {
    }

    public record GarrisonResult(int garrisonedSummons, int failedSummons, boolean focusedTarget) {
    }
}
