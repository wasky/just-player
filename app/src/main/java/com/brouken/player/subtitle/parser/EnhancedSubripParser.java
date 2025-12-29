package com.brouken.player.subtitle.parser;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;

import androidx.annotation.NonNull;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Consumer;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.SubtitleParser;
import androidx.media3.extractor.text.subrip.SubripParser;

import java.util.ArrayList;
import java.util.List;

public class EnhancedSubripParser implements SubtitleParser {

    private final SubripParser delegate = new SubripParser();

    @Override
    public void parse(@NonNull byte[] data, @NonNull OutputOptions outputOptions, @NonNull Consumer<CuesWithTiming> output) {
        delegate.parse(data, outputOptions, cuesWithTiming -> output.accept(applySlashLineStyling(cuesWithTiming)));
    }

    @Override
    public void parse(@NonNull byte[] data, int offset, int length, @NonNull OutputOptions outputOptions, @NonNull Consumer<CuesWithTiming> output) {
        delegate.parse(data, offset, length, outputOptions, cuesWithTiming -> output.accept(applySlashLineStyling(cuesWithTiming)));
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public int getCueReplacementBehavior() {
        return delegate.getCueReplacementBehavior();
    }

    private CuesWithTiming applySlashLineStyling(CuesWithTiming cuesWithTiming) {
        List<Cue> cues = cuesWithTiming.cues;
        List<Cue> updatedCues = null;

        for (int i = 0; i < cues.size(); i++) {
            Cue cue = cues.get(i);
            Cue updatedCue = applySlashLineStyling(cue);
            if (updatedCue != cue) {
                if (updatedCues == null) updatedCues = new ArrayList<>(cues);
                updatedCues.set(i, updatedCue);
            }
        }

        if (updatedCues == null) {
            return cuesWithTiming;
        } else {
            return new CuesWithTiming(updatedCues, cuesWithTiming.startTimeUs, cuesWithTiming.durationUs);
        }
    }

    private Cue applySlashLineStyling(Cue cue) {
        CharSequence text = cue.text;

        if (text == null || text.length() == 0) return cue;

        String rawText = text.toString();
        SpannableStringBuilder builder = new SpannableStringBuilder(text);

        if (!stripLeadingSlashAndApplyItalicStyle(builder, rawText)) {
            return cue;
        } else {
            return cue.buildUpon()
                    .setText(builder)
                    .build();
        }
    }

    private boolean stripLeadingSlashAndApplyItalicStyle(SpannableStringBuilder builder, String text) {
        int length = text.length();
        boolean modified = false;
        int lineStart = 0;
        int removed = 0;

        while (lineStart < length) {
            int lineEnd = lineStart;
            while (lineEnd < length) {
                char ch = text.charAt(lineEnd);
                if (ch == '\n' || ch == '\r') {
                    break;
                }
                lineEnd++;
            }

            if (text.charAt(lineStart) == '/') {
                int adjustedStart = lineStart - removed;
                builder.delete(adjustedStart, adjustedStart + 1);
                removed++;
                int adjustedEnd = lineEnd - removed;
                if (adjustedEnd > adjustedStart) {
                    builder.setSpan(new StyleSpan(Typeface.ITALIC), adjustedStart, adjustedEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                modified = true;
            }

            if (lineEnd < length && text.charAt(lineEnd) == '\r') lineEnd++;
            if (lineEnd < length && text.charAt(lineEnd) == '\n') lineEnd++;

            lineStart = lineEnd;
        }

        return modified;
    }

}
