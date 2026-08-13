package com.brouken.player.subtitle;

import android.content.Context;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.ForwardingRenderer;
import androidx.media3.exoplayer.Renderer;
import androidx.media3.exoplayer.text.TextOutput;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Applies the user subtitle delay at render time by shifting the playback position seen by the
 * text renderers. Shifting cue timestamps at parse time cannot move embedded subtitles earlier:
 * their parsed timestamps are relative to the container sample (start == 0), so a negative shift
 * gets clamped and only shortens the cue duration. Render-time shifting works in both directions,
 * for embedded, external and image-based subtitles alike.
 */
public final class SubtitleDelayRenderersFactory extends DefaultRenderersFactory {

    private final AtomicInteger subtitleDelayMs;

    public SubtitleDelayRenderersFactory(Context context, AtomicInteger subtitleDelayMs) {
        super(context);
        this.subtitleDelayMs = subtitleDelayMs;
    }

    @Override
    protected void buildTextRenderers(@NonNull Context context, @NonNull TextOutput output, @NonNull Looper outputLooper, int extensionRendererMode, @NonNull ArrayList<Renderer> out) {
        ArrayList<Renderer> textRenderers = new ArrayList<>();
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, textRenderers);
        for (Renderer renderer : textRenderers) {
            if (renderer.getTrackType() == C.TRACK_TYPE_TEXT) {
                out.add(new SubtitleDelayRenderer(renderer, subtitleDelayMs));
            } else {
                out.add(renderer);
            }
        }
    }

    private static final class SubtitleDelayRenderer extends ForwardingRenderer {

        private final AtomicInteger delayMs;

        private SubtitleDelayRenderer(Renderer renderer, AtomicInteger delayMs) {
            super(renderer);
            this.delayMs = delayMs;
        }

        private long delayUs() {
            return delayMs.get() * 1000L;
        }

        @Override
        public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
            // Cues show when position >= cue start: presenting an earlier position delays
            // subtitles, a later one advances them.
            super.render(positionUs - delayUs(), elapsedRealtimeUs);
        }

        @Override
        public long getDurationToProgressUs(long positionUs, long elapsedRealtimeUs) {
            return super.getDurationToProgressUs(positionUs - delayUs(), elapsedRealtimeUs);
        }

    }

}
