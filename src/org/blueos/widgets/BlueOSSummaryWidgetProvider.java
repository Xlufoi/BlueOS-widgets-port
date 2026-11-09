package org.blueos.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.CalendarContract.Instances;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BlueOSSummaryWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_REFRESH = "org.blueos.widgets.summary.REFRESH";

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)
                || AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED.equals(action)) {
            push(context);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        push(context);
    }

    public static void push(Context context) {
        Context app = LanguageManager.applyLocale(context.getApplicationContext());
        AppWidgetManager wm = AppWidgetManager.getInstance(app);

        // Update transparent
        updateInstances(app, wm, BlueOSSummaryWidgetProvider.class, true);
        // Update card
        updateInstances(app, wm, BlueOSSummaryWidgetProvider.SummaryCard.class, false);
    }

    private static void updateInstances(Context app, AppWidgetManager wm, Class<?> cls, boolean isTransparent) {
        ComponentName provider = new ComponentName(app, cls);
        int[] ids = wm.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return;

        WallpaperColors colors = null;
        try {
            colors = WallpaperManager.getInstance(app).getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
        } catch (Throwable ignored) {}
        boolean darkText = colors != null
                && (colors.getColorHints() & WallpaperColors.HINT_SUPPORTS_DARK_TEXT) != 0;

        int layoutId;
        if (isTransparent) {
            layoutId = darkText ? R.layout.blueos_summary_widget_dark_text : R.layout.blueos_summary_widget_light_text;
        } else {
            layoutId = R.layout.blueos_summary_widget_card;
        }

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int greetingRes;
        if (hour >= 5 && hour < 12) {
            greetingRes = R.string.blueos_summary_morning;
        } else if (hour >= 12 && hour < 17) {
            greetingRes = R.string.blueos_summary_day;
        } else if (hour >= 17 && hour < 23) {
            greetingRes = R.string.blueos_summary_evening;
        } else {
            greetingRes = R.string.blueos_summary_night;
        }

        String nextInfo = getNextEventOrAlarm(app);
        Locale locale = LanguageManager.getLocale(app);

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(app.getPackageName(), layoutId);
            rv.setTextViewText(R.id.blueos_summary_greeting, app.getString(greetingRes));

            String dateStr = DateFormat.format(DateFormat.getBestDateTimePattern(locale, "EEEE, d MMMM"), new Date()).toString();
            rv.setTextViewText(R.id.blueos_summary_date, dateStr);

            if (nextInfo != null && !nextInfo.isEmpty()) {
                rv.setTextViewText(R.id.blueos_summary_next, nextInfo);
                rv.setViewVisibility(R.id.blueos_summary_next, View.VISIBLE);
                rv.setViewVisibility(R.id.blueos_summary_icon, View.VISIBLE);
            } else {
                rv.setViewVisibility(R.id.blueos_summary_next, View.GONE);
                rv.setViewVisibility(R.id.blueos_summary_icon, View.GONE);
            }

            Intent it = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR);
            if (it.resolveActivity(app.getPackageManager()) == null) {
                it = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
            }
            rv.setOnClickPendingIntent(android.R.id.background,
                    PendingIntent.getActivity(app, id, it, PendingIntent.FLAG_IMMUTABLE));

            WidgetThemeManager.applyBackground(app, rv, isTransparent);
            wm.updateAppWidget(id, rv);
        }
    }

    public static class SummaryCard extends BlueOSSummaryWidgetProvider {}

    private static String getNextEventOrAlarm(Context context) {
        try {
            long now = System.currentTimeMillis();
            Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
            android.content.ContentUris.appendId(builder, now);
            android.content.ContentUris.appendId(builder, now + DateUtils.DAY_IN_MILLIS);
            Cursor c = context.getContentResolver().query(builder.build(),
                    new String[]{Instances.TITLE, Instances.BEGIN},
                    null, null, Instances.BEGIN + " ASC LIMIT 1");
            if (c != null) {
                try {
                    if (c.moveToFirst()) {
                        String title = c.getString(0);
                        long begin = c.getLong(1);
                        String time = DateFormat.getTimeFormat(context).format(new Date(begin));
                        return title + " (" + time + ")";
                    }
                } finally {
                    c.close();
                }
            }
        } catch (Throwable ignored) {}

        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                AlarmManager.AlarmClockInfo info = am.getNextAlarmClock();
                if (info != null && info.getTriggerTime() > 0) {
                    return "Будильник: " + DateFormat.getTimeFormat(context).format(new Date(info.getTriggerTime()));
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
