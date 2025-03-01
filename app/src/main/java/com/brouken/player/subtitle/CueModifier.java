package com.brouken.player.subtitle;

import android.content.Context;
import android.text.SpannableString;
import android.util.DisplayMetrics;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.media3.common.text.Cue;

import com.brouken.player.osd.subtitle.SubtitleEdgeType;

import java.util.ArrayList;
import java.util.List;

public class CueModifier {

    private final ShadowSpan shadowSpan;

    private SubtitleEdgeType subtitleEdgeType;

    public CueModifier(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int oneDpInPx = Math.round((1f * displayMetrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT);
        shadowSpan = new ShadowSpan(oneDpInPx, oneDpInPx);
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
        if (subtitleEdgeType != SubtitleEdgeType.OutlineShadow) return cue;

        SpannableString spannableString;
        if (cue.text instanceof SpannableString) {
            spannableString = (SpannableString) cue.text;
        } else {
            spannableString = SpannableString.valueOf(cue.text);
        }

        addShadow(spannableString);

        return cue.buildUpon()
                .setText(spannableString)
                .build();
    }

    private void addShadow(SpannableString spannableString) {
        spannableString.setSpan(shadowSpan, 0, spannableString.length(), 0);
    }

}
