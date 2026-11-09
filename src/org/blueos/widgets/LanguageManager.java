package org.blueos.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageManager {
    public static final String KEY_LANG = "app_language"; // "ru" or "en"
    public static final String KEY_ONBOARDING_DONE = "blueos_onboarding_completed";

    public static boolean isOnboardingCompleted(Context context) {
        return context.getSharedPreferences(WidgetThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public static void setOnboardingCompleted(Context context, boolean completed) {
        context.getSharedPreferences(WidgetThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply();
    }

    public static String getLanguage(Context context) {
        SharedPreferences sp = context.getSharedPreferences(WidgetThemeManager.PREFS_NAME, Context.MODE_PRIVATE);
        if (sp.contains(KEY_LANG)) {
            return sp.getString(KEY_LANG, "ru");
        }
        // Detect system locale default on first launch
        Locale def = Locale.getDefault();
        if (def != null && "ru".equalsIgnoreCase(def.getLanguage())) {
            return "ru";
        }
        return "ru"; // Default fallback
    }

    public static void setLanguage(Context context, String lang) {
        if (lang == null || (!lang.equals("ru") && !lang.equals("en"))) {
            lang = "ru";
        }
        context.getSharedPreferences(WidgetThemeManager.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_LANG, lang).apply();
        applyLocale(context);
    }

    public static Context applyLocale(Context context) {
        String lang = getLanguage(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);
        try {
            res.updateConfiguration(config, res.getDisplayMetrics());
        } catch (Throwable ignored) {}
        return context.createConfigurationContext(config);
    }

    public static boolean isRussian(Context context) {
        return "ru".equalsIgnoreCase(getLanguage(context));
    }

    public static Locale getLocale(Context context) {
        return new Locale(getLanguage(context));
    }
}
