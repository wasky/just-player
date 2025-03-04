package com.brouken.player.subtitle;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class TextPaintModifierSpan extends CharacterStyle {

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setLinearText(true);
    }

}