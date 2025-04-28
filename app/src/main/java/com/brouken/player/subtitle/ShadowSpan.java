package com.brouken.player.subtitle;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

import androidx.annotation.ColorInt;

public class ShadowSpan extends CharacterStyle {

    private final float shadowRadius;
    private final float shadowOffset;
    private int shadowColor;

    public ShadowSpan(float shadowRadius, float shadowOffset) {
        this.shadowRadius = shadowRadius;
        this.shadowOffset = shadowOffset;
    }

    public void setShadowColor(@ColorInt int shadowColor) {
        this.shadowColor = shadowColor;
    }

    @ColorInt
    public int getShadowColor() {
        return shadowColor;
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, shadowColor);
    }

}