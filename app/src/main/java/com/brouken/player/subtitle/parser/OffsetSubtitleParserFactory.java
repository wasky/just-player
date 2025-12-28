package com.brouken.player.subtitle.parser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.util.Consumer;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.SubtitleParser;

import java.util.concurrent.TimeUnit;

public final class OffsetSubtitleParserFactory implements SubtitleParser.Factory {

    private final SubtitleParser.Factory delegate;
    private final long delayUs;

    public OffsetSubtitleParserFactory(SubtitleParser.Factory delegate, int delayMs) {
        this.delegate = delegate;
        this.delayUs = TimeUnit.MILLISECONDS.toMicros(delayMs);
    }

    @Override
    public boolean supportsFormat(@NonNull Format format) {
        return delegate.supportsFormat(format);
    }

    @Override
    public int getCueReplacementBehavior(@NonNull Format format) {
        return delegate.getCueReplacementBehavior(format);
    }

    @NonNull
    @Override
    public SubtitleParser create(@NonNull Format format) {
        SubtitleParser parser = delegate.create(format);
        if (delayUs == 0) return parser;
        return new OffsetSubtitleParser(parser, delayUs);
    }

    private static final class OffsetSubtitleParser implements SubtitleParser {

        private final SubtitleParser delegate;
        private final long delayUs;

        private OffsetSubtitleParser(SubtitleParser delegate, long delayUs) {
            this.delegate = delegate;
            this.delayUs = delayUs;
        }

        @Override
        public void parse(@NonNull byte[] data, int offset, int length, @NonNull OutputOptions outputOptions, @NonNull Consumer<CuesWithTiming> output) {
            OutputOptions adjustedOptions = adjustOutputOptions(outputOptions);
            delegate.parse(data, offset, length, adjustedOptions, cuesWithTiming -> {
                CuesWithTiming shifted = shiftCues(cuesWithTiming);
                if (shifted != null) output.accept(shifted);
            });
        }

        @Override
        public void reset() {
            delegate.reset();
        }

        @Override
        public int getCueReplacementBehavior() {
            return delegate.getCueReplacementBehavior();
        }

        private OutputOptions adjustOutputOptions(OutputOptions outputOptions) {
            if (outputOptions.startTimeUs == C.TIME_UNSET) return outputOptions;

            long adjustedStartUs = outputOptions.startTimeUs - delayUs;
            if (adjustedStartUs < 0) adjustedStartUs = 0;

            if (outputOptions.outputAllCues) {
                return OutputOptions.cuesAfterThenRemainingCuesBefore(adjustedStartUs);
            } else {
                return OutputOptions.onlyCuesAfter(adjustedStartUs);
            }
        }

        @Nullable
        private CuesWithTiming shiftCues(CuesWithTiming cuesWithTiming) {
            long startTimeUs = cuesWithTiming.startTimeUs;
            if (startTimeUs == C.TIME_UNSET) return cuesWithTiming;

            long durationUs = cuesWithTiming.durationUs;
            long shiftedStartUs = startTimeUs + delayUs;

            if (durationUs == C.TIME_UNSET) {
                if (shiftedStartUs < 0) shiftedStartUs = 0;

                if (shiftedStartUs == startTimeUs) {
                    return cuesWithTiming;
                } else {
                    return new CuesWithTiming(cuesWithTiming.cues, shiftedStartUs, durationUs);
                }
            }

            long endTimeUs = startTimeUs + durationUs;
            long shiftedEndUs = endTimeUs + delayUs;

            if (shiftedEndUs <= 0) return null;

            if (shiftedStartUs < 0) shiftedStartUs = 0;
            long shiftedDurationUs = shiftedEndUs - shiftedStartUs;

            if (shiftedDurationUs > 0) {
                return new CuesWithTiming(cuesWithTiming.cues, shiftedStartUs, shiftedDurationUs);
            } else {
                return null;
            }
        }

    }

}
