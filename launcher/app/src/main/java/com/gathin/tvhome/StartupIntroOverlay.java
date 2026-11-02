package com.gathin.tvhome;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;

/**
 * Plays the UNNAMED startup clip over the already-created Home screen in the same activity/window.
 * The black frame stays up until Android reports an actual decoded video frame, then the clip fades in.
 * Near the end the entire overlay fades out to expose the launcher that has been rendering underneath.
 */
final class StartupIntroOverlay {
    private static final String ASSET_NAME = "unnamed-intro.mp4";
    private static final long FADE_IN_MS = 360L;
    private static final long FADE_OUT_MS = 500L;
    private static final long FIRST_FRAME_FALLBACK_MS = 700L;

    private final Activity activity;
    private final FrameLayout host;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private FrameLayout overlay;
    private TextureView videoView;
    private MediaPlayer player;
    private Surface surface;
    private boolean fadeInStarted;
    private boolean fadeOutStarted;

    private final Runnable fallbackFadeIn = this::fadeVideoIn;
    private final Runnable scheduledFadeOut = this::fadeOut;

    StartupIntroOverlay(Activity activity, FrameLayout host) {
        this.activity = activity;
        this.host = host;
    }

    /** Safe to call again on each Home-key launch. A clip already playing is not restarted. */
    void play() {
        if (activity.isFinishing() || (Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        if (overlay != null) return;

        fadeInStarted = false;
        fadeOutStarted = false;

        FrameLayout cover = new FrameLayout(activity);
        cover.setBackgroundColor(Color.BLACK);
        cover.setAlpha(1f);
        cover.setFocusable(false);
        cover.setFocusableInTouchMode(false);

        TextureView texture = new TextureView(activity);
        texture.setAlpha(0f);
        // TextureView can composite with alpha, which is needed for the fade over the black cover.
        texture.setOpaque(false);
        texture.setFocusable(false);
        texture.setFocusableInTouchMode(false);

        overlay = cover;
        videoView = texture;
        texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                prepareAndStart(surfaceTexture, cover, texture);
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                // The source is pre-encoded at 16:9 and the launcher is landscape; no per-frame transform needed.
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                // Rendering is signalled by MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START below.
            }
        });
        cover.addView(texture, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        host.addView(cover, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cover.bringToFront();
    }

    private boolean isCurrent(FrameLayout expectedOverlay, MediaPlayer expectedPlayer) {
        return overlay == expectedOverlay && player == expectedPlayer && !fadeOutStarted;
    }

    private void prepareAndStart(SurfaceTexture surfaceTexture, FrameLayout expectedOverlay, TextureView expectedView) {
        if (overlay != expectedOverlay || videoView != expectedView || fadeOutStarted) return;

        MediaPlayer mediaPlayer = new MediaPlayer();
        player = mediaPlayer;
        surface = new Surface(surfaceTexture);
        try {
            // Set mute before preparing the track. The bundled MP4 is also stripped of its audio stream.
            mediaPlayer.setVolume(0f, 0f);
            try (AssetFileDescriptor asset = activity.getAssets().openFd(ASSET_NAME)) {
                mediaPlayer.setDataSource(asset.getFileDescriptor(), asset.getStartOffset(), asset.getLength());
            }
            mediaPlayer.setSurface(surface);
            mediaPlayer.setScreenOnWhilePlaying(false);
            mediaPlayer.setOnPreparedListener(mp -> {
                if (!isCurrent(expectedOverlay, mediaPlayer)) return;
                try {
                    mp.setVolume(0f, 0f);
                    mp.start();
                    int duration = Math.max(0, mp.getDuration());
                    long fadeDelay = Math.max(100L, duration - FADE_OUT_MS);
                    handler.removeCallbacks(scheduledFadeOut);
                    handler.postDelayed(scheduledFadeOut, fadeDelay);
                    // Some vendor MediaPlayer implementations omit VIDEO_RENDERING_START. The fallback avoids
                    // leaving the black cover up forever; when the info callback arrives first it cancels this.
                    handler.removeCallbacks(fallbackFadeIn);
                    handler.postDelayed(fallbackFadeIn, FIRST_FRAME_FALLBACK_MS);
                } catch (IllegalStateException failure) {
                    finishImmediately();
                }
            });
            mediaPlayer.setOnInfoListener((mp, what, extra) -> {
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    fadeVideoIn();
                    return true;
                }
                return false;
            });
            mediaPlayer.setOnCompletionListener(mp -> fadeOut());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                finishImmediately();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
            finishImmediately();
        }
    }

    private void fadeVideoIn() {
        if (overlay == null || videoView == null || fadeInStarted || fadeOutStarted) return;
        fadeInStarted = true;
        handler.removeCallbacks(fallbackFadeIn);
        videoView.animate().cancel();
        videoView.animate().alpha(1f).setDuration(FADE_IN_MS).start();
    }

    private void fadeOut() {
        if (overlay == null || fadeOutStarted) return;
        fadeOutStarted = true;
        handler.removeCallbacks(fallbackFadeIn);
        handler.removeCallbacks(scheduledFadeOut);
        FrameLayout cover = overlay;
        cover.animate().cancel();
        cover.animate().alpha(0f).setDuration(FADE_OUT_MS).withEndAction(this::finishImmediately).start();
    }

    /** Stop now when Home is covered by another activity, so the clip never sits over another app. */
    void stop() {
        finishImmediately();
    }

    boolean isPlaying() {
        return overlay != null && !fadeOutStarted;
    }

    void skip() {
        fadeOut();
    }

    private void finishImmediately() {
        handler.removeCallbacks(fallbackFadeIn);
        handler.removeCallbacks(scheduledFadeOut);

        FrameLayout cover = overlay;
        TextureView texture = videoView;
        MediaPlayer mediaPlayer = player;
        Surface currentSurface = surface;
        overlay = null;
        videoView = null;
        player = null;
        surface = null;
        fadeInStarted = false;
        fadeOutStarted = false;

        if (cover != null) cover.animate().cancel();
        if (texture != null) texture.animate().cancel();
        if (mediaPlayer != null) {
            mediaPlayer.setOnPreparedListener(null);
            mediaPlayer.setOnInfoListener(null);
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnErrorListener(null);
            try {
                mediaPlayer.stop();
            } catch (IllegalStateException ignored) {
                // MediaPlayer may still be preparing or may have completed naturally.
            }
            mediaPlayer.release();
        }
        if (currentSurface != null) currentSurface.release();
        if (cover != null && cover.getParent() == host) host.removeView(cover);
    }
}
