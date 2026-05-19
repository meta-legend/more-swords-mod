package net.metalegend.moreswordsmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.metalegend.moreswordsmod.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

// client-only visual timeline for a confirmed sheath strike
public final class KatanaSheathStrikeAnimation {
    private static final int DEFAULT_INTRO_TICKS = 3;
    private static final int DEFAULT_SLASH_TICKS = 3;
    private static final int DEFAULT_RETURN_TO_START_TICKS = 2;
    private static final int DEFAULT_OUTRO_TICKS = 4;
    private static final boolean DEFAULT_USE_END_POSE_VALUES = true;
    private static final float DEFAULT_FOV_KICK_AMOUNT = 0.10f;
    private static final float DEFAULT_SCREEN_X_START = -2.10f;
    private static final float DEFAULT_SCREEN_X_END = -2.10f;
    private static final float DEFAULT_SCREEN_Y_START = -0.12f;
    private static final float DEFAULT_SCREEN_Y_END = -0.12f;
    private static final float DEFAULT_SCREEN_Z_START = -1.05f;
    private static final float DEFAULT_SCREEN_Z_END = -1.05f;
    private static final float DEFAULT_SCREEN_SCALE_START = 4.00f;
    private static final float DEFAULT_SCREEN_SCALE_END = 4.00f;
    private static final float DEFAULT_SCREEN_X_ROTATION_START = 178.0f;
    private static final float DEFAULT_SCREEN_X_ROTATION_END = 178.0f;
    private static final float DEFAULT_SCREEN_Y_ROTATION_START = -180.0f;
    private static final float DEFAULT_SCREEN_Y_ROTATION_END = -315.0f;
    private static final float DEFAULT_SCREEN_Z_ROTATION_START = 112.0f;
    private static final float DEFAULT_SCREEN_Z_ROTATION_END = 112.0f;
    private static float screenXStart = DEFAULT_SCREEN_X_START;
    private static float screenXEnd = DEFAULT_SCREEN_X_END;
    private static float screenYStart = DEFAULT_SCREEN_Y_START;
    private static float screenYEnd = DEFAULT_SCREEN_Y_END;
    private static float screenZStart = DEFAULT_SCREEN_Z_START;
    private static float screenZEnd = DEFAULT_SCREEN_Z_END;
    private static float screenScaleStart = DEFAULT_SCREEN_SCALE_START;
    private static float screenScaleEnd = DEFAULT_SCREEN_SCALE_END;
    private static float fovKickAmount = DEFAULT_FOV_KICK_AMOUNT;
    private static float screenXRotationStart = DEFAULT_SCREEN_X_ROTATION_START;
    private static float screenXRotationEnd = DEFAULT_SCREEN_X_ROTATION_END;
    private static float screenYRotationStart = DEFAULT_SCREEN_Y_ROTATION_START;
    private static float screenYRotationEnd = DEFAULT_SCREEN_Y_ROTATION_END;
    private static float screenZRotationStart = DEFAULT_SCREEN_Z_ROTATION_START;
    private static float screenZRotationEnd = DEFAULT_SCREEN_Z_ROTATION_END;
    private static boolean useEndPoseValues = DEFAULT_USE_END_POSE_VALUES;
    private static int introTicks = DEFAULT_INTRO_TICKS;
    private static int slashTicks = DEFAULT_SLASH_TICKS;
    private static int returnToStartTicks = DEFAULT_RETURN_TO_START_TICKS;
    private static int outroTicks = DEFAULT_OUTRO_TICKS;
    private static final Map<Integer, AnimationState> ACTIVE_ANIMATIONS = new HashMap<>();

    private KatanaSheathStrikeAnimation() {
    }

    public static void start(int entityId) {
        AnimationState existingState = ACTIVE_ANIMATIONS.get(entityId);
        if (existingState != null && existingState.elapsedTicks <= 3) {
            return;
        }

        ACTIVE_ANIMATIONS.put(entityId, new AnimationState());
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) {
            ACTIVE_ANIMATIONS.clear();
            return;
        }

        Iterator<Map.Entry<Integer, AnimationState>> iterator = ACTIVE_ANIMATIONS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, AnimationState> entry = iterator.next();
            AnimationState state = entry.getValue();
            Entity entity = level.getEntity(entry.getKey());

            if (entity != null) {
                if (state.elapsedTicks < getDashTicks()) {
                    spawnSpatialTearTrail(level, entity);
                }
            }

            state.elapsedTicks++;
            if (state.elapsedTicks > getTotalTicks()) {
                iterator.remove();
            }
        }
    }

    public static boolean isActive(int entityId) {
        return ACTIVE_ANIMATIONS.containsKey(entityId);
    }

    public static float getFovMultiplier(int entityId, float partialTick) {
        return 1.0f + getLeanAmount(entityId, partialTick) * fovKickAmount;
    }

    public static float getLeanAmount(int entityId, float partialTick) {
        AnimationState state = ACTIVE_ANIMATIONS.get(entityId);
        if (state == null) {
            return 0.0f;
        }

        float elapsed = Math.max(0.0f, state.elapsedTicks + partialTick);
        if (introTicks > 0 && elapsed < introTicks) {
            return smooth(elapsed / introTicks);
        }

        if (elapsed < getDashTicks()) {
            return 1.0f;
        }

        if (outroTicks <= 0) {
            return 0.0f;
        }

        return 1.0f - smooth((elapsed - getDashTicks()) / outroTicks);
    }

    public static void applyFirstPersonTransform(int entityId, float partialTick, int handSide, PoseStack poseStack) {
        float poseAmount = getLeanAmount(entityId, partialTick);
        if (poseAmount <= 0.0f) {
            return;
        }

        float jitter = getItemJitter(entityId, partialTick) * getDashBurstAmount(entityId, partialTick);
        float tipDriveAmount = getTipDriveAmount(entityId, partialTick);
        float crossScreenX = Mth.lerp(tipDriveAmount, -0.78f, -0.50f);
        float crossScreenY = Mth.lerp(tipDriveAmount, 0.08f, 0.22f);
        float crossScreenZ = Mth.lerp(tipDriveAmount, -0.36f, -0.52f);
        poseStack.translate(
                handSide * crossScreenX * poseAmount + jitter,
                crossScreenY * poseAmount + jitter * 0.25f,
                crossScreenZ * poseAmount
        );
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(tipDriveAmount, -6.0f, -14.0f) * poseAmount));
        poseStack.mulPose(Axis.YP.rotationDegrees(handSide * Mth.lerp(tipDriveAmount, -18.0f, -30.0f) * poseAmount));
        poseStack.mulPose(Axis.ZP.rotationDegrees(handSide * Mth.lerp(tipDriveAmount, 52.0f, 66.0f) * poseAmount));
    }

    public static void applyFirstPersonScreenPose(int entityId, float partialTick, int handSide, PoseStack poseStack) {
        float poseAmount = getLeanAmount(entityId, partialTick);
        if (poseAmount <= 0.0f) {
            return;
        }

        ScreenPose screenPose = getCurrentScreenPose(entityId, partialTick);
        if (screenPose == null) {
            return;
        }

        float jitter = getItemJitter(entityId, partialTick) * getDashBurstAmount(entityId, partialTick) * 0.5f;

        poseStack.translate(handSide * screenPose.x + jitter, screenPose.y + jitter * 0.25f, screenPose.z);
        poseStack.mulPose(Axis.XP.rotationDegrees(screenPose.xRotation));
        poseStack.mulPose(Axis.YP.rotationDegrees(handSide * screenPose.yRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(handSide * screenPose.zRotation));
        poseStack.scale(screenPose.scale, screenPose.scale, screenPose.scale);
    }

    public static boolean setScreenPoseValue(String field, float value) {
        switch (field) {
            case "xStart" -> screenXStart = value;
            case "xEnd" -> screenXEnd = value;
            case "yStart" -> screenYStart = value;
            case "yEnd" -> screenYEnd = value;
            case "zStart" -> screenZStart = value;
            case "zEnd" -> screenZEnd = value;
            case "scaleStart" -> screenScaleStart = value;
            case "scaleEnd" -> screenScaleEnd = value;
            case "fov" -> fovKickAmount = value;
            case "fovKick" -> fovKickAmount = value;
            case "xRot" -> {
                screenXRotationStart = value;
                screenXRotationEnd = value;
            }
            case "xRotStart" -> screenXRotationStart = value;
            case "xRotEnd" -> screenXRotationEnd = value;
            case "yRot" -> {
                screenYRotationStart = value;
                screenYRotationEnd = value;
            }
            case "yRotStart" -> screenYRotationStart = value;
            case "yRotEnd" -> screenYRotationEnd = value;
            case "zRot" -> {
                screenZRotationStart = value;
                screenZRotationEnd = value;
            }
            case "zRotStart" -> screenZRotationStart = value;
            case "zRotEnd" -> screenZRotationEnd = value;
            case "introTicks" -> introTicks = clampTickValue(value, 0, 10);
            case "slashTicks" -> slashTicks = clampTickValue(value, 1, 20);
            case "returnTicks" -> returnToStartTicks = clampTickValue(value, 0, 10);
            case "returnToStartTicks" -> returnToStartTicks = clampTickValue(value, 0, 10);
            case "outroTicks" -> outroTicks = clampTickValue(value, 0, 20);
            default -> {
                return false;
            }
        }
        return true;
    }

    public static void setUseEndPoseValues(boolean enabled) {
        useEndPoseValues = enabled;
    }

    public static boolean toggleUseEndPoseValues() {
        useEndPoseValues = !useEndPoseValues;
        return useEndPoseValues;
    }

    public static void resetScreenPoseTuning() {
        screenXStart = DEFAULT_SCREEN_X_START;
        screenXEnd = DEFAULT_SCREEN_X_END;
        screenYStart = DEFAULT_SCREEN_Y_START;
        screenYEnd = DEFAULT_SCREEN_Y_END;
        screenZStart = DEFAULT_SCREEN_Z_START;
        screenZEnd = DEFAULT_SCREEN_Z_END;
        screenScaleStart = DEFAULT_SCREEN_SCALE_START;
        screenScaleEnd = DEFAULT_SCREEN_SCALE_END;
        fovKickAmount = DEFAULT_FOV_KICK_AMOUNT;
        screenXRotationStart = DEFAULT_SCREEN_X_ROTATION_START;
        screenXRotationEnd = DEFAULT_SCREEN_X_ROTATION_END;
        screenYRotationStart = DEFAULT_SCREEN_Y_ROTATION_START;
        screenYRotationEnd = DEFAULT_SCREEN_Y_ROTATION_END;
        screenZRotationStart = DEFAULT_SCREEN_Z_ROTATION_START;
        screenZRotationEnd = DEFAULT_SCREEN_Z_ROTATION_END;
        useEndPoseValues = DEFAULT_USE_END_POSE_VALUES;
        introTicks = DEFAULT_INTRO_TICKS;
        slashTicks = DEFAULT_SLASH_TICKS;
        returnToStartTicks = DEFAULT_RETURN_TO_START_TICKS;
        outroTicks = DEFAULT_OUTRO_TICKS;
    }

    public static String[] getScreenPoseFieldNames() {
        return new String[]{"xStart", "xEnd", "yStart", "yEnd", "zStart", "zEnd", "scaleStart", "scaleEnd", "fov", "fovKick", "xRot", "xRotStart", "xRotEnd", "yRot", "yRotStart", "yRotEnd", "zRot", "zRotStart", "zRotEnd", "introTicks", "slashTicks", "returnTicks", "returnToStartTicks", "outroTicks"};
    }

    public static String getScreenPoseTuningSummary() {
        return "xStart=" + screenXStart
                + ", xEnd=" + screenXEnd
                + ", yStart=" + screenYStart
                + ", yEnd=" + screenYEnd
                + ", zStart=" + screenZStart
                + ", zEnd=" + screenZEnd
                + ", scaleStart=" + screenScaleStart
                + ", scaleEnd=" + screenScaleEnd
                + ", fov=" + fovKickAmount
                + ", xRotStart=" + screenXRotationStart
                + ", xRotEnd=" + screenXRotationEnd
                + ", yRotStart=" + screenYRotationStart
                + ", yRotEnd=" + screenYRotationEnd
                + ", zRotStart=" + screenZRotationStart
                + ", zRotEnd=" + screenZRotationEnd
                + ", useEndValues=" + useEndPoseValues
                + ", introTicks=" + introTicks
                + ", slashTicks=" + slashTicks
                + ", returnTicks=" + returnToStartTicks
                + ", outroTicks=" + outroTicks;
    }

    private static void spawnSpatialTearTrail(ClientLevel level, Entity entity) {
        ParticleOptions particle = getTrailParticle(entity);
        Vec3 direction = getHorizontalDashDirection(entity);
        double baseY = entity.getY() + Math.max(0.25, entity.getBbHeight() * 0.35);

        for (int i = 0; i < 3; i++) {
            double backOffset = 0.2 + i * 0.32 + entity.getRandom().nextDouble() * 0.18;
            double sideOffset = (entity.getRandom().nextDouble() - 0.5) * 0.32;
            Vec3 side = new Vec3(-direction.z, 0.0, direction.x);
            double x = entity.getX() - direction.x * backOffset + side.x * sideOffset;
            double y = baseY + entity.getRandom().nextDouble() * Math.max(0.35, entity.getBbHeight() * 0.55);
            double z = entity.getZ() - direction.z * backOffset + side.z * sideOffset;

            level.addParticle(particle, x, y, z, -direction.x * 0.035, 0.01, -direction.z * 0.035);
        }
    }

    private static ParticleOptions getTrailParticle(Entity entity) {
        if (entity instanceof LivingEntity livingEntity) {
            ItemStack stack = livingEntity.getMainHandItem();
            if (stack.is(ModItems.DIAMOND_KATANA) || stack.is(ModItems.NETHERITE_KATANA)) {
                return ParticleTypes.ELECTRIC_SPARK;
            }
        }

        return ParticleTypes.CRIT;
    }

    private static Vec3 getHorizontalDashDirection(Entity entity) {
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 direction = new Vec3(velocity.x, 0.0, velocity.z);
        if (direction.lengthSqr() < 1.0E-4) {
            Vec3 look = entity.getLookAngle();
            direction = new Vec3(look.x, 0.0, look.z);
        }

        return direction.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : direction.normalize();
    }

    private static float getItemJitter(int entityId, float partialTick) {
        float phase = (entityId * 0.37f) + partialTick * 24.0f + ACTIVE_ANIMATIONS.get(entityId).elapsedTicks * 1.7f;
        return Mth.sin(phase) * 0.018f;
    }

    private static float getDashBurstAmount(int entityId, float partialTick) {
        AnimationState state = ACTIVE_ANIMATIONS.get(entityId);
        if (state == null) {
            return 0.0f;
        }

        float elapsed = Math.max(0.0f, state.elapsedTicks + partialTick);
        if (elapsed >= getDashTicks()) {
            return 0.0f;
        }

        return getLeanAmount(entityId, partialTick);
    }

    private static ScreenPose getCurrentScreenPose(int entityId, float partialTick) {
        AnimationState state = ACTIVE_ANIMATIONS.get(entityId);
        if (state == null) {
            return null;
        }

        float elapsed = Math.max(0.0f, state.elapsedTicks + partialTick);
        ScreenPose neutralPose = ScreenPose.neutral();
        ScreenPose startPose = ScreenPose.start();
        ScreenPose endPose = useEndPoseValues ? ScreenPose.end() : startPose;

        if (introTicks > 0 && elapsed < introTicks) {
            return ScreenPose.lerp(neutralPose, startPose, smooth(elapsed / introTicks));
        }

        float slashStartTick = introTicks;
        float returnStartTick = slashStartTick + slashTicks;
        float outroStartTick = returnStartTick + returnToStartTicks;

        if (elapsed < returnStartTick) {
            return ScreenPose.lerp(startPose, endPose, smooth((elapsed - slashStartTick) / slashTicks));
        }

        if (returnToStartTicks > 0 && elapsed < outroStartTick) {
            return ScreenPose.lerp(endPose, startPose, smooth((elapsed - returnStartTick) / returnToStartTicks));
        }

        if (outroTicks > 0 && elapsed < getTotalTicks()) {
            return ScreenPose.lerpOutro(startPose, neutralPose, (elapsed - outroStartTick) / outroTicks);
        }

        return null;
    }

    private static float getTipDriveAmount(int entityId, float partialTick) {
        AnimationState state = ACTIVE_ANIMATIONS.get(entityId);
        if (state == null) {
            return 0.0f;
        }

        float elapsed = Math.max(0.0f, state.elapsedTicks + partialTick);
        if (elapsed >= getDashTicks()) {
            return 1.0f;
        }

        return smooth(elapsed / getDashTicks());
    }

    private static int getDashTicks() {
        return introTicks + slashTicks + returnToStartTicks;
    }

    private static int getTotalTicks() {
        return getDashTicks() + outroTicks;
    }

    private static int clampTickValue(float value, int min, int max) {
        return Mth.clamp(Math.round(value), min, max);
    }

    private static float smooth(float value) {
        float clamped = Mth.clamp(value, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static final class AnimationState {
        private int elapsedTicks;
    }

    private static final class ScreenPose {
        private final float x;
        private final float y;
        private final float z;
        private final float scale;
        private final float xRotation;
        private final float yRotation;
        private final float zRotation;

        private ScreenPose(float x, float y, float z, float scale, float xRotation, float yRotation, float zRotation) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.scale = scale;
            this.xRotation = xRotation;
            this.yRotation = yRotation;
            this.zRotation = zRotation;
        }

        private static ScreenPose neutral() {
            return new ScreenPose(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f);
        }

        private static ScreenPose start() {
            return new ScreenPose(
                    screenXStart,
                    screenYStart,
                    screenZStart,
                    screenScaleStart,
                    screenXRotationStart,
                    screenYRotationStart,
                    screenZRotationStart
            );
        }

        private static ScreenPose end() {
            return new ScreenPose(
                    screenXEnd,
                    screenYEnd,
                    screenZEnd,
                    screenScaleEnd,
                    screenXRotationEnd,
                    screenYRotationEnd,
                    screenZRotationEnd
            );
        }

        private static ScreenPose lerp(ScreenPose from, ScreenPose to, float amount) {
            return new ScreenPose(
                    Mth.lerp(amount, from.x, to.x),
                    Mth.lerp(amount, from.y, to.y),
                    Mth.lerp(amount, from.z, to.z),
                    Mth.lerp(amount, from.scale, to.scale),
                    Mth.lerp(amount, from.xRotation, to.xRotation),
                    Mth.lerp(amount, from.yRotation, to.yRotation),
                    Mth.lerp(amount, from.zRotation, to.zRotation)
            );
        }

        private static ScreenPose lerpOutro(ScreenPose from, ScreenPose to, float amount) {
            float moveAmount = smooth(amount);
            float rotationAmount = smooth(Mth.clamp((amount - 0.75f) / 0.25f, 0.0f, 1.0f));
            return new ScreenPose(
                    Mth.lerp(moveAmount, from.x, to.x),
                    Mth.lerp(moveAmount, from.y, to.y),
                    Mth.lerp(moveAmount, from.z, to.z),
                    Mth.lerp(moveAmount, from.scale, to.scale),
                    Mth.lerp(rotationAmount, from.xRotation, to.xRotation),
                    Mth.lerp(rotationAmount, from.yRotation, to.yRotation),
                    Mth.lerp(rotationAmount, from.zRotation, to.zRotation)
            );
        }
    }
}
