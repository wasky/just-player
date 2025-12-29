package com.brouken.player.subtitle.parser;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.text.Cue;
import androidx.media3.common.util.Consumer;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.SubtitleParser;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MicroDvdParser implements SubtitleParser {

    private static final String TAG = "MicroDvdParser";

    private enum SubtitleFormat {MICRO_DVD, MPL2, UNKNOWN}

    private static final Pattern LINE_PATTERN = Pattern.compile("^[\\[{](-?\\d+)[\\]}][\\[{](-?\\d+)[\\]}](.*)$");
    private static final Pattern STYLE_PATTERN = Pattern.compile("\\{y:([^}]*)\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern COLOR_PATTERN = Pattern.compile("\\{c:(\\$[0-9a-fA-F]{6}|[a-zA-Z]+)\\}", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTHER_TAG_PATTERN = Pattern.compile("\\{[A-Za-z]:[^}]*\\}");
    private static final Pattern FRAME_RATE_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    private static final double DEFAULT_FRAME_RATE = 24000.0 / 1001.0;
    private static final double MPL2_FRAME_RATE = 10.0;

    private final SubtitleParser fallbackParser;
    private final int cueReplacementBehavior;

    private final double fallbackFrameRate;
    private double headerFrameRate;

    private boolean isSubtitleFormatDetected;
    private SubtitleFormat subtitleFormat = SubtitleFormat.UNKNOWN;

    public MicroDvdParser(@NonNull Format format, @NonNull SubtitleParser fallbackParser, double fallbackFrameRate) {
        this.fallbackParser = fallbackParser;
        this.cueReplacementBehavior = fallbackParser.getCueReplacementBehavior();

        if (fallbackFrameRate > 0) {
            this.fallbackFrameRate = fallbackFrameRate;
        } else {
            this.fallbackFrameRate = format.frameRate > 0 ? format.frameRate : DEFAULT_FRAME_RATE;
        }
    }

    @Override
    public void parse(@NonNull byte[] data, @NonNull OutputOptions outputOptions, @NonNull Consumer<CuesWithTiming> output) {
        parse(data, 0, data.length, outputOptions, output);
    }

    @Override
    public void parse(@NonNull byte[] data, int offset, int length, @NonNull OutputOptions outputOptions, @NonNull Consumer<CuesWithTiming> output) {
        ensureDetection(data, offset, length);
        if (subtitleFormat == SubtitleFormat.MICRO_DVD || subtitleFormat == SubtitleFormat.MPL2) {
            parseMicroDvd(data, offset, length, outputOptions, output);
        } else {
            fallbackParser.parse(data, offset, length, outputOptions, output);
        }
    }

    @Override
    public void reset() {
        isSubtitleFormatDetected = false;
        subtitleFormat = SubtitleFormat.UNKNOWN;
        headerFrameRate = 0.0;
        fallbackParser.reset();
    }

    @Override
    public int getCueReplacementBehavior() {
        return cueReplacementBehavior;
    }

    private void ensureDetection(byte[] data, int offset, int length) {
        if (isSubtitleFormatDetected || length <= 0) return;
        subtitleFormat = getSubtitleFormat(data, offset, length);
        isSubtitleFormatDetected = true;
    }

    private SubtitleFormat getSubtitleFormat(byte[] data, int offset, int length) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data, offset, length), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                return getFormatFromSubtitleLine(line);
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return SubtitleFormat.UNKNOWN;
    }

    private SubtitleFormat getFormatFromSubtitleLine(String line) {
        String normalized = normalizeLine(line);
        boolean matches = LINE_PATTERN.matcher(normalized).matches();
        if (matches) {
            if (normalized.charAt(0) == '{') {
                return SubtitleFormat.MICRO_DVD;
            } else {
                return SubtitleFormat.MPL2;
            }
        } else {
            return SubtitleFormat.UNKNOWN;
        }
    }

    private void parseMicroDvd(byte[] data, int offset, int length, OutputOptions outputOptions, Consumer<CuesWithTiming> output) {
        List<CuesWithTiming> cuesWithTimingBeforeRequestedStartTimeUs =
                outputOptions.startTimeUs != C.TIME_UNSET && outputOptions.outputAllCues
                        ? new ArrayList<>()
                        : null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(data, offset, length), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                ParsedCue parsedCue = parseCueLine(line);
                if (parsedCue == null) continue;
                CuesWithTiming cuesWithTiming = parsedCue.toCuesWithTiming();
                if (outputOptions.startTimeUs == C.TIME_UNSET || parsedCue.endTimeUs >= outputOptions.startTimeUs) {
                    output.accept(cuesWithTiming);
                } else if (cuesWithTimingBeforeRequestedStartTimeUs != null) {
                    cuesWithTimingBeforeRequestedStartTimeUs.add(cuesWithTiming);
                }
            }
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        if (cuesWithTimingBeforeRequestedStartTimeUs != null) {
            for (CuesWithTiming cuesWithTiming : cuesWithTimingBeforeRequestedStartTimeUs) {
                output.accept(cuesWithTiming);
            }
        }
    }

    @Nullable
    private ParsedCue parseCueLine(String line) {
        String normalized = normalizeLine(line);
        Matcher match = LINE_PATTERN.matcher(normalized);

        if (!match.matches()) return null;

        long startFrame = Long.parseLong(match.group(1));
        long endFrame = Long.parseLong(match.group(2));

        String rawText = match.group(3);
        if (isFrameRateHeader(startFrame, endFrame, rawText)) {
            applyFrameRateHeader(rawText);
            return null;
        }

        long startTimeUs = frameToTimeUs(startFrame);
        long endTimeUs = frameToTimeUs(endFrame);

        if (endTimeUs < startTimeUs) {
            endTimeUs = startTimeUs;
        }

        CharSequence text = convertText(rawText);

        Cue cue = new Cue.Builder()
                .setText(text)
                .build();

        return new ParsedCue(cue, startTimeUs, endTimeUs);
    }

    private String normalizeLine(String line) {
        String normalized = line.trim();
        if (!normalized.isEmpty() && normalized.charAt(0) == '\uFEFF') {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private boolean isFrameRateHeader(long startFrame, long endFrame, String text) {
        if (startFrame != endFrame || startFrame > 1L) return false;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return false;
        return FRAME_RATE_PATTERN.matcher(trimmed).matches();
    }

    private long frameToTimeUs(long frame) {
        long safeFrame = Math.max(frame, 0);
        double frameRate = resolvedFrameRate();
        if (frameRate == DEFAULT_FRAME_RATE) {
            return (safeFrame * 1_000_000L * 1001L) / 24_000L;
        } else {
            return (long) ((safeFrame * 1_000_000.0) / frameRate);
        }
    }

    private double resolvedFrameRate() {
        if (headerFrameRate > 0) return headerFrameRate;
        if (subtitleFormat == SubtitleFormat.MPL2) return MPL2_FRAME_RATE;
        return fallbackFrameRate;
    }

    private void applyFrameRateHeader(String text) {
        String trimmedText = text.trim();
        if (trimmedText.startsWith("23.97")) {
            headerFrameRate = DEFAULT_FRAME_RATE;
        } else {
            double parsed = parseDoubleOrZero(trimmedText);
            if (parsed > 0) headerFrameRate = parsed;
        }
    }

    private CharSequence convertText(String text) {
        TextWithStyle extracted = extractStyles(text);
        String withBreaks = convertLineBreaks(extracted.text);
        SpannableStringBuilder builder = applyLineItalics(withBreaks, extracted.style.italic);

        int length = builder.length();
        if (length == 0) return builder;

        if (extracted.style.bold && extracted.style.italic) {
            builder.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            if (extracted.style.bold) {
                builder.setSpan(new StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (extracted.style.italic) {
                builder.setSpan(new StyleSpan(Typeface.ITALIC), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        if (extracted.style.underline) {
            builder.setSpan(new UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (extracted.style.color != null) {
            builder.setSpan(new ForegroundColorSpan(extracted.style.color), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return builder;
    }

    private String convertLineBreaks(String text) {
        String placeholder = "\u0000";
        return text
                .replace("\\|", placeholder)
                .replace("|", "\n")
                .replace(placeholder, "|");
    }

    private SpannableStringBuilder applyLineItalics(String text, boolean globalItalic) {
        String[] lines = text.split("\n");
        SpannableStringBuilder builder = new SpannableStringBuilder();

        for (int i = 0; i < lines.length; i++) {
            if (i > 0) builder.append('\n');
            String line = lines[i];
            boolean lineItalic = false;
            if (line.startsWith("/")) {
                line = line.substring(1);
                if (!globalItalic && !line.isEmpty()) {
                    lineItalic = true;
                }
            }
            int start = builder.length();
            builder.append(line);
            int end = builder.length();
            if (lineItalic) {
                builder.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }

        return builder;
    }

    private TextWithStyle extractStyles(String text) {
        StyleFlags style = new StyleFlags();
        String result = stripStyleTags(text, style);
        result = stripColorTags(result, style);
        result = OTHER_TAG_PATTERN.matcher(result).replaceAll("");
        return new TextWithStyle(result, style);
    }

    private String stripStyleTags(String text, StyleFlags style) {
        Matcher matcher = STYLE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String styles = matcher.group(1);
            for (int i = 0; i < styles.length(); i++) {
                char ch = Character.toLowerCase(styles.charAt(i));
                switch (ch) {
                    case 'i':
                        style.italic = true;
                        break;
                    case 'b':
                        style.bold = true;
                        break;
                    case 'u':
                        style.underline = true;
                        break;
                    default:
                        break;
                }
            }
            matcher.appendReplacement(buffer, "");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String stripColorTags(String text, StyleFlags style) {
        Matcher matcher = COLOR_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            if (style.color == null) {
                String colorGroup = matcher.group(1);
                try {
                    if (colorGroup.charAt(0) == '$') {
                        style.color = Color.parseColor('#' + colorGroup.substring(1));
                    } else {
                        style.color = Color.parseColor(colorGroup);
                    }
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, e);
                }
            }
            matcher.appendReplacement(buffer, "");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double parseDoubleOrZero(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final class TextWithStyle {

        private final String text;
        private final StyleFlags style;

        private TextWithStyle(String text, StyleFlags style) {
            this.text = text;
            this.style = style;
        }
    }

    private static final class StyleFlags {
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private Integer color;
    }

    private static final class ParsedCue {

        private final Cue cue;
        private final long startTimeUs;
        private final long endTimeUs;

        private ParsedCue(Cue cue, long startTimeUs, long endTimeUs) {
            this.cue = cue;
            this.startTimeUs = startTimeUs;
            this.endTimeUs = endTimeUs;
        }

        private CuesWithTiming toCuesWithTiming() {
            long durationUs = endTimeUs - startTimeUs;
            return new CuesWithTiming(Collections.singletonList(cue), startTimeUs, durationUs);
        }
    }
}
