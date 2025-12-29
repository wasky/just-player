package com.brouken.player.subtitle.parser;

import androidx.annotation.NonNull;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.extractor.text.DefaultSubtitleParserFactory;
import androidx.media3.extractor.text.SubtitleParser;

public final class EnhancedSubtitleParserFactory implements SubtitleParser.Factory {

    private final DefaultSubtitleParserFactory defaultFactory = new DefaultSubtitleParserFactory();
    private final double fallbackFrameRate;

    public EnhancedSubtitleParserFactory(double fallbackFrameRate) {
        this.fallbackFrameRate = fallbackFrameRate;
    }

    @Override
    public boolean supportsFormat(@NonNull Format format) {
        return defaultFactory.supportsFormat(format);
    }

    @Override
    public int getCueReplacementBehavior(@NonNull Format format) {
        return defaultFactory.getCueReplacementBehavior(format);
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    @NonNull
    @Override
    public SubtitleParser create(@NonNull Format format) {
        SubtitleParser fallbackParser;
        String mimeType = format.sampleMimeType != null ? format.sampleMimeType : "";
        switch (mimeType) {
            case MimeTypes.APPLICATION_SUBRIP:
                fallbackParser = new EnhancedSubripParser();
                break;
            default:
                fallbackParser = defaultFactory.create(format);
                break;
        }
        return new MicroDvdParser(format, fallbackParser, fallbackFrameRate);
    }

}
