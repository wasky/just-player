package com.brouken.player.subtitle;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.fonts.FontStyle;
import android.os.Build;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.DisplayMetrics;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.media3.common.text.Cue;

import com.brouken.player.osd.subtitle.SubtitleEdgeType;
import com.brouken.player.osd.subtitle.SubtitleTypeface;

import java.util.ArrayList;
import java.util.List;

public class CueModifier {

    private final ShadowSpan shadowSpan;

    private SubtitleTypeface subtitleTypeface;
    private Typeface italicTypeface;
    private SubtitleEdgeType subtitleEdgeType;

    public CueModifier(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int oneDpInPx = Math.round((1f * displayMetrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT);
        shadowSpan = new ShadowSpan(oneDpInPx, oneDpInPx);
    }

    @SuppressLint("InlinedApi")
    public void setSubtitleTypeface(SubtitleTypeface subtitleTypeface, Typeface typeface) {
        this.subtitleTypeface = subtitleTypeface;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.italicTypeface = Typeface.create(typeface, FontStyle.FONT_WEIGHT_MEDIUM, true);
        }
    }

    public SubtitleTypeface getSubtitleTypeface() {
        return subtitleTypeface;
    }

    public void setSubtitleEdgeType(SubtitleEdgeType edgeType) {
        this.subtitleEdgeType = edgeType;
    }

    public SubtitleEdgeType getSubtitleEdgeType() {
        return subtitleEdgeType;
    }

    public void setShadowColor(@ColorInt int shadowColor) {
        shadowSpan.setShadowColor(shadowColor);
    }

    @ColorInt
    public int getShadowColor() {
        return shadowSpan.getShadowColor();
    }

    public List<Cue> modifyCues(@NonNull List<Cue> cues) {
        List<Cue> modifiedCues = new ArrayList<>(cues.size());
        for (int i = 0; i < cues.size(); i++) {
            modifiedCues.add(modifyCue(cues.get(i)));
        }
        return modifiedCues;
    }

    // TODO(Wasky) Create documentation for this method
    private Cue modifyCue(Cue cue) {
        SpannableString spannableString;
        if (cue.text instanceof SpannableString) {
            spannableString = (SpannableString) cue.text;
        } else {
            spannableString = SpannableString.valueOf(cue.text);
        }

        boolean modified;
        modified = modifyItalicTypeface(spannableString);
        modified = addShadow(spannableString) || modified;

        if (modified) {
            return cue.buildUpon()
                    .setText(spannableString)
                    .build();
        } else {
            return cue;
        }
    }

    private boolean modifyItalicTypeface(SpannableString spannableString) {
        boolean modified = false;
        if (subtitleTypeface == SubtitleTypeface.Medium && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            StyleSpan[] styleSpans = spannableString.getSpans(0, spannableString.length(), StyleSpan.class);
            for (StyleSpan span : styleSpans) {
                if (span.getStyle() == Typeface.ITALIC) {
                    int start = spannableString.getSpanStart(span);
                    int end = spannableString.getSpanEnd(span);
                    int flags = spannableString.getSpanFlags(span);
                    spannableString.removeSpan(span);
                    TypefaceSpan newSpan = new TypefaceSpan(italicTypeface);
                    spannableString.setSpan(newSpan, start, end, flags);
                    modified = true;
                }
            }
        }
        return modified;
    }

    private boolean addShadow(SpannableString spannableString) {
        if (subtitleEdgeType == SubtitleEdgeType.OutlineShadow) {
            spannableString.setSpan(shadowSpan, 0, spannableString.length(), 0);
            return true;
        }
        return false;
    }

}
