package org.blueos.widgets;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

public class WidgetThemeManager {
    public static final String PREFS_NAME = "blueos_widget_prefs";
    public static final String KEY_THEME = "widget_theme_mode"; // 0: Pure Black, 1: AOSP Dark, 2: Monet
    public static final String KEY_TRANSLUCENT = "widget_translucent"; // boolean

    public static final int THEME_PURE_BLACK = 0;
    public static final int THEME_AOSP_DARK = 1;
    public static final int THEME_MONET = 2;

    public static int getAccentColor(Context context) {
        int theme = getTheme(context);
        if (theme == THEME_MONET) {
            return getMonetAccentColor(context);
        } else if (theme == THEME_AOSP_DARK) {
            return 0xFF8AB4F8; // AOSP Pixel Blue
        } else {
            return 0xFF38BDF8; // Pure Black Cyan
        }
    }

    public static int getMonetAccentColor(Context context) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            if (wm != null) {
                WallpaperColors colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors != null) {
                    Color c = colors.getSecondaryColor();
                    if (c == null) c = colors.getPrimaryColor();
                    if (c != null) {
                        float[] hsv = new float[3];
                        Color.colorToHSV(c.toArgb(), hsv);
                        hsv[1] = Math.min(1.0f, Math.max(0.60f, hsv[1])); // vibrant saturation
                        hsv[2] = 0.90f; // bright for dark mode
                        return Color.HSVToColor(hsv);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // System resource fallback
        try {
            return context.getColor(android.R.color.system_accent1_300);
        } catch (Throwable ignored) {}

        return 0xFF38BDF8;
    }

    public static int getMonetBackgroundColor(Context context) {
        try {
            WallpaperManager wm = WallpaperManager.getInstance(context);
            if (wm != null) {
                WallpaperColors colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                if (colors != null) {
                    Color c = colors.getPrimaryColor();
                    if (c != null) {
                        float[] hsv = new float[3];
                        Color.colorToHSV(c.toArgb(), hsv);
                        hsv[1] = Math.min(0.25f, hsv[1]); // subtle wallpaper tint
                        hsv[2] = 0.12f; // deep dark neutral surface
                        return Color.HSVToColor(hsv);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // Fallback
        try {
            return context.getColor(android.R.color.system_neutral1_900);
        } catch (Throwable ignored) {}

        return 0xFF191C20;
    }

    public static void applyBackground(Context context, RemoteViews rv) {
        int theme = getTheme(context);
        boolean trans = isTranslucent(context);

        if (theme == THEME_MONET) {
            int bg = getMonetBackgroundColor(context);
            try {
                rv.setImageViewResource(R.id.blueos_widget_bg, R.drawable.blueos_card_monet_shape);
                rv.setInt(R.id.blueos_widget_bg, "setColorFilter", bg);
                rv.setInt(R.id.blueos_widget_bg, "setImageAlpha", trans ? 180 : 255);
            } catch (Throwable ignored) {}
            try {
                rv.setInt(android.R.id.background, "setBackgroundColor", Color.TRANSPARENT);
            } catch (Throwable ignored) {}
        } else if (theme == THEME_AOSP_DARK) {
            int bgRes = trans ? R.drawable.blueos_card_aosp_dark_translucent : R.drawable.blueos_card_aosp_dark;
            try {
                rv.setImageViewResource(R.id.blueos_widget_bg, bgRes);
                rv.setInt(R.id.blueos_widget_bg, "setImageAlpha", 255);
            } catch (Throwable ignored) {}
            try {
                rv.setInt(android.R.id.background, "setBackgroundColor", Color.TRANSPARENT);
            } catch (Throwable ignored) {}
        } else {
            int bgRes = trans ? R.drawable.blueos_card_pure_black_translucent : R.drawable.blueos_card_pure_black;
            try {
                rv.setImageViewResource(R.id.blueos_widget_bg, bgRes);
                rv.setInt(R.id.blueos_widget_bg, "setImageAlpha", 255);
            } catch (Throwable ignored) {}
            try {
                rv.setInt(android.R.id.background, "setBackgroundColor", Color.TRANSPARENT);
            } catch (Throwable ignored) {}
        }
    }

    public static void setTheme(Context context, int theme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_THEME, theme).apply();
    }

    public static int getTheme(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_THEME, THEME_PURE_BLACK);
    }

    public static void setTranslucent(Context context, boolean translucent) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_TRANSLUCENT, translucent).apply();
    }

    public static boolean isTranslucent(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_TRANSLUCENT, false);
    }
}
