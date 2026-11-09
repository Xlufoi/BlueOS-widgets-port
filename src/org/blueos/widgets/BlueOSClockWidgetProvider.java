package org.blueos.widgets;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public abstract class BlueOSClockWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_MOE_TICK = "org.blueos.widgets.action.MOE_TICK";

    protected abstract int getLayoutId();

    protected boolean hasDateRow() {
        return true;
    }

    protected boolean isTransparent() {
        return false;
    }

    protected void style(Context context, RemoteViews rv) {
        int accent = WidgetThemeManager.getAccentColor(context);
        try {
            rv.setTextColor(R.id.blueos_minutes, accent);
        } catch (Throwable ignored) {}
    }

    protected void applyThemeBackground(Context context, RemoteViews rv, int widgetId) {
        WidgetThemeManager.applyBackground(context, rv, isTransparent());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        final String action = intent != null ? intent.getAction() : null;
        if (action != null) {
            final AppWidgetManager wm = AppWidgetManager.getInstance(context);
            onUpdate(context, wm, wm.getAppWidgetIds(new ComponentName(context, getClass())));
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        for (int widgetId : widgetIds) {
            wm.updateAppWidget(widgetId, buildViews(context, wm, widgetId));
        }
    }

    protected RemoteViews buildViews(Context context, AppWidgetManager wm, int widgetId) {
        final RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());

        Intent openClock = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        openClock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (openClock.resolveActivity(context.getPackageManager()) == null) {
            openClock = context.getPackageManager().getLaunchIntentForPackage("com.android.deskclock");
        }
        if (openClock != null) {
            final PendingIntent pi = PendingIntent.getActivity(context, 0, openClock, FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(android.R.id.background, pi);
        }

        if (hasDateRow()) {
            final String dateFormat = DateFormat.getBestDateTimePattern(LanguageManager.getLocale(context), "EEEMMMd");
            rv.setCharSequence(R.id.blueos_date, "setFormat12Hour", dateFormat);
            rv.setCharSequence(R.id.blueos_date, "setFormat24Hour", dateFormat);

            AlarmHelper.AlarmInfo alarm = AlarmHelper.getNextAlarm(context);
            final int alarmVisibility = alarm.hasAlarm ? View.VISIBLE : View.GONE;
            rv.setViewVisibility(R.id.blueos_alarm_icon, alarmVisibility);
            rv.setViewVisibility(R.id.blueos_next_alarm, alarmVisibility);
            rv.setTextViewText(R.id.blueos_next_alarm, alarm.timeStr);
        }

        style(context, rv);
        applyThemeBackground(context, rv, widgetId);
        return rv;
    }

    /** 1a. Card Clock (2x2 Card) */
    public static class Card extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_card;
        }
    }

    /** 1b. Card Clock (2x2 Transparent) */
    public static class CardTransparent extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_card_trans;
        }

        @Override
        protected boolean isTransparent() {
            return true;
        }
    }

    public static class CardOutline extends CardTransparent {}

    /** 2a. Card Wide Clock (4x2 Card) */
    public static class CardWide extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_card_wide;
        }
    }

    /** 2b. Card Wide Clock (4x2 Transparent) */
    public static class CardWideTransparent extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_card_wide_trans;
        }

        @Override
        protected boolean isTransparent() {
            return true;
        }
    }

    public static class CardWideOutline extends CardWideTransparent {}

    /** 3a. Halo Clock (2x2 Transparent) */
    public static class Halo extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_halo;
        }

        @Override
        protected boolean isTransparent() {
            return true;
        }

        @Override
        protected void style(Context context, RemoteViews rv) {
            try {
                final WallpaperColors colors = WallpaperManager.getInstance(context)
                        .getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                final boolean darkText = colors != null
                        && (colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;

                final int text = context.getColor(darkText
                        ? R.color.blueos_widget_on_surface
                        : android.R.color.white);
                final int variant = context.getColor(darkText
                        ? R.color.blueos_widget_on_surface_variant
                        : android.R.color.white);

                rv.setTextColor(R.id.blueos_hours, text);
                rv.setTextColor(R.id.blueos_colon, text);
                rv.setTextColor(R.id.blueos_date, variant);
                rv.setTextColor(R.id.blueos_next_alarm, variant);
                rv.setInt(R.id.blueos_alarm_icon, "setColorFilter", variant);
            } catch (Throwable ignored) {}
        }
    }

    /** 3b. Halo Clock (2x2 Card) */
    public static class HaloCard extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_halo_card;
        }

        @Override
        protected boolean isTransparent() {
            return false;
        }
    }

    /** 4a. Tile Clock (2x2 Card) */
    public static class Tile extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_tile;
        }

        @Override
        protected boolean hasDateRow() {
            return false;
        }
    }

    /** 4b. Tile Clock (2x2 Transparent) */
    public static class TileTransparent extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_tile_trans;
        }

        @Override
        protected boolean hasDateRow() {
            return false;
        }

        @Override
        protected boolean isTransparent() {
            return true;
        }
    }

    /** 5a. Next Alarm Widget (2x2 Card) */
    public static class Alarm extends BlueOSClockWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_alarm;
        }

        @Override
        protected boolean hasDateRow() {
            return false;
        }

        @Override
        protected RemoteViews buildViews(Context context, AppWidgetManager wm, int widgetId) {
            final RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
            Intent openClock = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            openClock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (openClock.resolveActivity(context.getPackageManager()) == null) {
                openClock = context.getPackageManager().getLaunchIntentForPackage("com.android.deskclock");
            }
            if (openClock != null) {
                final PendingIntent pi = PendingIntent.getActivity(context, 0, openClock, FLAG_IMMUTABLE);
                rv.setOnClickPendingIntent(android.R.id.background, pi);
            }

            AlarmHelper.AlarmInfo alarm = AlarmHelper.getNextAlarm(context);
            rv.setTextViewText(R.id.blueos_alarm_time, alarm.timeStr);
            rv.setTextViewText(R.id.blueos_alarm_day, alarm.subtext);

            int accent = WidgetThemeManager.getAccentColor(context);
            rv.setTextColor(R.id.blueos_alarm_time, accent);

            WidgetThemeManager.applyBackground(context, rv, isTransparent());
            return rv;
        }
    }

    /** 5b. Next Alarm Widget (2x2 Transparent) */
    public static class AlarmTransparent extends Alarm {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_alarm_trans;
        }

        @Override
        protected boolean isTransparent() {
            return true;
        }
    }

    /** 6a. Kivotos Moe Clock (2x2 Transparent) */
    public static class Moe extends BlueOSClockWidgetProvider {
        private static final int[] DIGIT_RES = {
                R.drawable.blueos_moe_0, R.drawable.blueos_moe_1, R.drawable.blueos_moe_2,
                R.drawable.blueos_moe_3, R.drawable.blueos_moe_4, R.drawable.blueos_moe_5,
                R.drawable.blueos_moe_6, R.drawable.blueos_moe_7, R.drawable.blueos_moe_8,
                R.drawable.blueos_moe_9
        };

        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_moe;
        }

        @Override
        protected boolean isTransparent() {
            return true;
        }

        @Override
        public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
            super.onUpdate(context, wm, widgetIds);
            scheduleMoeAlarm(context);
        }

        @Override
        protected RemoteViews buildViews(Context context, AppWidgetManager wm, int widgetId) {
            final RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
            Intent openClock = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            openClock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (openClock.resolveActivity(context.getPackageManager()) == null) {
                openClock = context.getPackageManager().getLaunchIntentForPackage("com.android.deskclock");
            }
            if (openClock != null) {
                final PendingIntent pi = PendingIntent.getActivity(context, 0, openClock, FLAG_IMMUTABLE);
                rv.setOnClickPendingIntent(android.R.id.background, pi);
            }

            Calendar cal = Calendar.getInstance();
            int h = cal.get(Calendar.HOUR_OF_DAY);
            int m = cal.get(Calendar.MINUTE);

            int h1 = Math.min(9, Math.max(0, h / 10));
            int h2 = Math.min(9, Math.max(0, h % 10));
            int m1 = Math.min(9, Math.max(0, m / 10));
            int m2 = Math.min(9, Math.max(0, m % 10));

            rv.setImageViewResource(R.id.blueos_moe_h1, DIGIT_RES[h1]);
            rv.setImageViewResource(R.id.blueos_moe_h2, DIGIT_RES[h2]);
            rv.setImageViewResource(R.id.blueos_moe_m1, DIGIT_RES[m1]);
            rv.setImageViewResource(R.id.blueos_moe_m2, DIGIT_RES[m2]);

            int accent = WidgetThemeManager.getAccentColor(context);
            rv.setTextColor(R.id.blueos_moe_colon, accent);

            WidgetThemeManager.applyBackground(context, rv, isTransparent());
            return rv;
        }
    }

    /** 6b. Kivotos Moe Clock (2x2 Card) */
    public static class MoeCard extends Moe {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_clock_widget_moe_card;
        }

        @Override
        protected boolean isTransparent() {
            return false;
        }
    }

    public static void updateMoeClocks(Context context) {
        if (context == null) return;
        Context app = context.getApplicationContext();
        AppWidgetManager wm = AppWidgetManager.getInstance(app);

        int[] idsMoe = wm.getAppWidgetIds(new ComponentName(app, Moe.class));
        if (idsMoe != null && idsMoe.length > 0) {
            new Moe().onUpdate(app, wm, idsMoe);
        }
        int[] idsCard = wm.getAppWidgetIds(new ComponentName(app, MoeCard.class));
        if (idsCard != null && idsCard.length > 0) {
            new MoeCard().onUpdate(app, wm, idsCard);
        }

        scheduleMoeAlarm(app);
    }

    public static void scheduleMoeAlarm(Context context) {
        if (context == null) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        long now = System.currentTimeMillis();
        long nextMinute = (now / 60000L + 1L) * 60000L + 50L;

        Intent intent = new Intent(ACTION_MOE_TICK).setClass(context, MoeTickReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 888, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMinute, pi);
        } catch (Throwable t) {
            try {
                am.set(AlarmManager.RTC, nextMinute, pi);
            } catch (Throwable ignored) {}
        }
    }

    public static class MoeTickReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMoeClocks(context);
        }
    }
}
