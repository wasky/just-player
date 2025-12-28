package com.brouken.player.osd.item;

import com.brouken.player.osd.OsdSettingsAdapter;

public class IntegerOsdSettingsItem extends LeftOrRightOsdSettingsItem {

    private final Listener listener;
    private final OsdSettingsAdapter adapter;
    private final String labelDefault;
    private final boolean addPlusToValue;
    private final int step;

    private int currentValue;

    public IntegerOsdSettingsItem(String title, String labelDefault, boolean addPlusToValue, int value, Listener listener, OsdSettingsAdapter adapter, int step) {
        super(title, null);

        super.listener = createLeftOrRightListener();
        this.listener = listener;
        this.adapter = adapter;
        this.labelDefault = labelDefault;
        this.addPlusToValue = addPlusToValue;
        this.step = step;

        this.currentValue = value;
        summary = getSummaryText(value);
    }

    public IntegerOsdSettingsItem(String title, String labelDefault, boolean addPlusToValue, int value, Listener listener, OsdSettingsAdapter adapter) {
        this(title, labelDefault, addPlusToValue, value, listener, adapter, 1);
    }

    protected String getSummaryText(int value) {
        if (value == 0 && labelDefault != null) {
            return labelDefault;
        } else if (value > 0 && addPlusToValue) {
            return "+" + value;
        } else {
            return String.valueOf(value);
        }
    }

    private void updateCurrentValue(int position, int newValue) {
        currentValue = newValue;
        summary = getSummaryText(newValue);
        adapter.notifyItemChanged(position);
        listener.onSettingChanged(position, newValue);
    }

    private LeftOrRightOsdSettingsItem.Listener createLeftOrRightListener() {
        return new LeftOrRightOsdSettingsItem.Listener() {
            @Override
            public void onSettingLeftClick(int position) {
                updateCurrentValue(position, currentValue - step);
            }

            @Override
            public void onSettingRightClick(int position) {
                updateCurrentValue(position, currentValue + step);
            }
        };
    }

    public interface Listener {
        void onSettingChanged(int position, int newValue);
    }

}
