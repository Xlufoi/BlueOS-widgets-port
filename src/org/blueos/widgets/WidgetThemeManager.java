package org.blueos.widgets;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.widget.RemoteViews;

public class WidgetThemeManager {
    public static final String PREFS_NAME = "blueos_widget_prefs";
    public static final String KEY_THEME = "widget_theme_mode"; // 0: Pure Black, 1: AOSP Dark, 2: Monet, 3: Navy Slate
    public static final String KEY_TRANSLUCENT = "widget_translucent"; // legacy boolean
    public static final String KEY_BG_ALPHA = "widget_bg_alpha"; // 0 - 255

    public static final int THEME_PURE_BLACK = 0;
    public static final int THEME_AOSP_DARK = 1;
    public static final int THEME_MONET = 2;
    public static final int THEME_NAVY_SLATE = 3;

    public static int getAccentColor(Context context) {
        int theme = getTheme(context);
        if (theme == THEME_MONET) {
            return getMonetAccentColor(context);
        } else if (theme == THEME_AOSP_DARK) {
            return 0xFF8AB4F8; // AOSP Pixel Blue
        } else if (theme == THEME_NAVY_SLATE) {
            return 0xFF60A5FA; // Soft Blue
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
                        hsv[1] = Math.min(1.0f, Math.max(0.60f, hsv[1]));
                        hsv[2] = 0.90f;
                        return Color.HSVToColor(hsv);
                    }
                }
            }
        } catch (Throwable ignored) {}

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
                        hsv[1] = Math.min(0.25f, hsv[1]);
                        hsv[2] = 0.12f;
                        return Color.HSVToColor(hsv);
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            return context.getColor(android.R.color.system_neutral1_900);
        } catch (Throwable ignored) {}

        return 0xFF191C20;
    }

    public static int getCardColor(Context context) {
        int theme = getTheme(context);
        if (theme == THEME_MONET) {
            return getMonetBackgroundColor(context);
        } else if (theme == THEME_AOSP_DARK) {
            return 0xFF1E1E1E;
        } else if (theme == THEME_NAVY_SLATE) {
            return 0xFF0F172A;
        } else {
            return 0xFF000000;
        }
    }

    public static int getBgAlpha(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (sp.contains(KEY_BG_ALPHA)) {
            return sp.getInt(KEY_BG_ALPHA, 255);
        }
        return sp.getBoolean(KEY_TRANSLUCENT, false) ? 180 : 255;
    }

    public static void setBgAlpha(Context context, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_BG_ALPHA, alpha)
                .putBoolean(KEY_TRANSLUCENT, alpha < 240)
                .apply();
    }

    public static void applyBackground(Context context, RemoteViews rv) {
        applyBackground(context, rv, false);
    }

    public static void applyBackground(Context context, RemoteViews rv, int appWidgetId) {
        applyBackground(context, rv, false);
    }

    public static void applyBackground(Context context, RemoteViews rv, boolean isTransparent) {
        if (isTransparent) {
            // Transparent variant: completely transparent on wallpaper, NO background, NO outline
            try {
                rv.setInt(android.R.id.background, "setBackgroundColor", Color.TRANSPARENT);
            } catch (Throwable ignored) {}
            try {
                rv.setViewVisibility(R.id.blueos_widget_bg, android.view.View.GONE);
            } catch (Throwable ignored) {}
            return;
        }

        // Card variant: solid or translucent card plate with subtle 1.2dp border
        int color = getCardColor(context);
        int alpha = getBgAlpha(context);

        try {
            rv.setInt(android.R.id.background, "setBackgroundResource", R.drawable.blueos_widget_outline);
        } catch (Throwable ignored) {}
        try {
            rv.setViewVisibility(R.id.blueos_widget_bg, android.view.View.VISIBLE);
        } catch (Throwable ignored) {}

        try {
            rv.setImageViewResource(R.id.blueos_widget_bg, R.drawable.blueos_card_pure_black);
            rv.setInt(R.id.blueos_widget_bg, "setColorFilter", color);
            rv.setInt(R.id.blueos_widget_bg, "setImageAlpha", alpha);
        } catch (Throwable ignored) {}
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
        setBgAlpha(context, translucent ? 180 : 255);
    }

    public static boolean isTranslucent(Context context) {
        return getBgAlpha(context) < 240;
    }
}
