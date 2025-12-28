package com.brouken.player.osd.item;

import android.content.Context;

import com.brouken.player.R;
import com.brouken.player.osd.OsdSettingsAdapter;

public final class SubtitleDelayOsdSettingsItem extends IntegerOsdSettingsItem {

    public SubtitleDelayOsdSettingsItem(Context context, int value, IntegerOsdSettingsItem.Listener listener, OsdSettingsAdapter adapter) {
        super(context.getString(R.string.osd_subtitle_delay_title), "", false, value, listener, adapter, 200);
    }

    @Override
    protected String getSummaryText(int value) {
        int absMs = Math.abs(value);
        int totalTenths = (absMs + 50) / 100;
        int minutes = totalTenths / 600;
        int secondsTenths = totalTenths % 600;
        int seconds = secondsTenths / 10;
        int tenths = secondsTenths % 10;

        String secondsPart = seconds + "." + tenths + " s";
        String formatted = minutes > 0 ? minutes + " m " + secondsPart : secondsPart;

        if (value < 0 && totalTenths > 0) {
            return "- " + formatted;
        } else {
            return formatted;
        }
    }

}
