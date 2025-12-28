package com.brouken.player.subtitle.parser;

import androidx.annotation.NonNull;
import androidx.media3.common.Format;
import androidx.media3.extractor.text.DefaultSubtitleParserFactory;
import androidx.media3.extractor.text.SubtitleParser;

public final class EnhancedSubtitleParserFactory implements SubtitleParser.Factory {

    private final DefaultSubtitleParserFactory defaultFactory = new DefaultSubtitleParserFactory();

    @Override
    public boolean supportsFormat(@NonNull Format format) {
        return defaultFactory.supportsFormat(format);
    }

    @Override
    public int getCueReplacementBehavior(@NonNull Format format) {
        return defaultFactory.getCueReplacementBehavior(format);
    }

    @NonNull
    @Override
    public SubtitleParser create(@NonNull Format format) {
        return defaultFactory.create(format);
    }

}
