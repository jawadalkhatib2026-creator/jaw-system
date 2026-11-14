package dev.sultan;

import android.graphics.drawable.GradientDrawable;

final class Theme {
    static final int[][] GRADIENTS = {
        {0xFF0B1026, 0xFF1B2A5C},
        {0xFF04202C, 0xFF0F5C6E},
        {0xFF2B0F1E, 0xFFB5482F},
        {0xFF04231A, 0xFF1F7A55},
        {0xFF0A0A0A, 0xFF2A2A2E},
    };
    static final int[] NAMES = {
        R.string.theme_midnight, R.string.theme_ocean, R.string.theme_sunset,
        R.string.theme_emerald, R.string.theme_graphite
    };

    static GradientDrawable background(int i) {
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, GRADIENTS[i % GRADIENTS.length]);
    }
}
