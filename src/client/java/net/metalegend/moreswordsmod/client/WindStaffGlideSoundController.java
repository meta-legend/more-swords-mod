package net.metalegend.moreswordsmod.client;

import net.metalegend.moreswordsmod.item.ModItems;
import net.metalegend.moreswordsmod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

// client-owned continuous glide audio so the wind staff can fade in and out without
// leaving server-started elytra sound instances playing after glide stops
public final class WindStaffGlideSoundController {
    private static final float GLIDE_TARGET_VOLUME = 0.26f;
    private static final float GLIDE_PITCH = 0.90f;
    private static final float GLIDE_START_VOLUME = 0.01f;
    private static final float GLIDE_FADE_IN_PER_TICK = GLIDE_TARGET_VOLUME / 12.0f;
    private static final float GLIDE_FADE_OUT_PER_TICK = GLIDE_TARGET_VOLUME / 12.0f;
    private static final int GLIDE_SUBTITLE_REFRESH_TICKS = 40;

    private static GlideSound activeSound;
    private static int subtitleRefreshTicks;

    private WindStaffGlideSoundController() {
    }

    public static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            if (activeSound != null) {
                activeSound.stopImmediately();
                activeSound = null;
            }
            subtitleRefreshTicks = 0;
            return;
        }

        LocalPlayer player = client.player;
        boolean shouldGlide = shouldPlayGlideSound(player);
        tickSubtitleRefresh(client, player, shouldGlide);

        if (activeSound == null || activeSound.isStopped()) {
            activeSound = null;
            if (shouldGlide) {
                activeSound = new GlideSound(player);
                client.getSoundManager().play(activeSound);
            }
            return;
        }

        activeSound.setGliding(shouldGlide);
    }

    private static void tickSubtitleRefresh(Minecraft client, LocalPlayer player, boolean shouldGlide) {
        if (!shouldGlide) {
            subtitleRefreshTicks = 0;
            return;
        }

        if (subtitleRefreshTicks > 0) {
            subtitleRefreshTicks--;
            return;
        }

        client.getSoundManager().play(new GlideSubtitleRefreshSound(player));
        subtitleRefreshTicks = GLIDE_SUBTITLE_REFRESH_TICKS;
    }

    private static boolean shouldPlayGlideSound(LocalPlayer player) {
        return isHoldingWindStaff(player)
                && !player.onGround()
                && player.isShiftKeyDown()
                && !player.getAbilities().flying
                && player.getDeltaMovement().y < 0.0
                && !player.isFallFlying()
                && !player.isInWater();
    }

    private static boolean isHoldingWindStaff(LocalPlayer player) {
        return player.getMainHandItem().is(ModItems.WIND_STAFF) || player.getOffhandItem().is(ModItems.WIND_STAFF);
    }

    private static final class GlideSubtitleRefreshSound extends AbstractSoundInstance {
        private GlideSubtitleRefreshSound(LocalPlayer player) {
            super(ModSounds.WIND_STAFF_GALE_GLIDE, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.volume = 0.0f;
            this.pitch = GLIDE_PITCH;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }
    }

    private static final class GlideSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        private boolean gliding = true;

        private GlideSound(LocalPlayer player) {
            super(ModSounds.WIND_STAFF_GALE_GLIDE, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = GLIDE_START_VOLUME;
            this.pitch = GLIDE_PITCH;
        }

        private void setGliding(boolean gliding) {
            this.gliding = gliding;
        }

        private void stopImmediately() {
            stop();
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            if (player.isRemoved()) {
                stop();
                return;
            }

            x = player.getX();
            y = player.getY();
            z = player.getZ();
            pitch = GLIDE_PITCH;

            float targetVolume = gliding ? GLIDE_TARGET_VOLUME : 0.0f;
            float step = gliding ? GLIDE_FADE_IN_PER_TICK : GLIDE_FADE_OUT_PER_TICK;
            volume = Mth.clamp(volume + Math.signum(targetVolume - volume) * step, 0.0f, GLIDE_TARGET_VOLUME);
            if (!gliding && volume <= 0.001f) {
                stop();
            }
        }
    }
}
