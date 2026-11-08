package org.blueos.widgets;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.RemoteViews;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public class BlueOSCountdownWidgetProvider extends AppWidgetProvider {

    static final String PREFS = "blueos_countdown";
    private static final String ACTION_MIDNIGHT = "org.blueos.calendar.action.COUNTDOWN_MIDNIGHT";

    static String keyTitle(int id) {
        return "title_" + id;
    }

    static String keyDate(int id) {
        return "date_" + id;
    }

    static String keyYearly(int id) {
        return "yearly_" + id;
    }

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        final String action = intent.getAction();
        if (ACTION_MIDNIGHT.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            final AppWidgetManager wm = AppWidgetManager.getInstance(context);
            final int[] ids = wm.getAppWidgetIds(new ComponentName(context, getClass()));
            onUpdate(context, wm, ids);
            scheduleMidnight(context);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        for (int id : widgetIds) {
            update(context, wm, id);
        }
        scheduleMidnight(context);
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        final SharedPreferences.Editor edit = prefs(context).edit();
        for (int id : widgetIds) {
            edit.remove(keyTitle(id)).remove(keyDate(id)).remove(keyYearly(id));
        }
        edit.apply();
    }

    @Override
    public void onDisabled(Context context) {
        final AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am != null) {
            am.cancel(midnightIntent(context));
        }
    }

    public static void update(Context context, AppWidgetManager wm, int id) {
        final SharedPreferences prefs = prefs(context);
        final RemoteViews rv = new RemoteViews(context.getPackageName(),
                R.layout.blueos_countdown_widget);

        final long epochDay = prefs.getLong(keyDate(id), Long.MIN_VALUE);
        if (epochDay == Long.MIN_VALUE) {
            rv.setTextViewText(R.id.blueos_countdown_title,
                    context.getString(R.string.blueos_countdown_widget_description));
            rv.setTextViewText(R.id.blueos_countdown_number, "…");
            rv.setViewVisibility(R.id.blueos_countdown_caption, View.GONE);
        } else {
            final boolean yearly = prefs.getBoolean(keyYearly(id), false);
            final LocalDate today = LocalDate.now(ZoneId.systemDefault());
            final LocalDate target = nextDate(LocalDate.ofEpochDay(epochDay), today, yearly);
            final long days = ChronoUnit.DAYS.between(today, target);
            final String title = prefs.getString(keyTitle(id), "");
            rv.setTextViewText(R.id.blueos_countdown_title, title);
            rv.setViewVisibility(R.id.blueos_countdown_title,
                    title.isEmpty() ? View.GONE : View.VISIBLE);

            final String date = DateFormat.format(
                    DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMM"),
                    target.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    .toString();
            if (days == 0) {
                rv.setTextViewText(R.id.blueos_countdown_number,
                        context.getString(R.string.blueos_countdown_today));
                rv.setTextViewText(R.id.blueos_countdown_caption, date);
            } else {
                final int n = (int) Math.min(Integer.MAX_VALUE, Math.abs(days));
                rv.setTextViewText(R.id.blueos_countdown_number, String.valueOf(n));
                rv.setTextViewText(R.id.blueos_countdown_caption,
                        context.getResources().getQuantityString(days > 0
                                ? R.plurals.blueos_countdown_days_until
                                : R.plurals.blueos_countdown_days_since, n, date));
            }
            rv.setViewVisibility(R.id.blueos_countdown_caption, View.VISIBLE);
        }

        final Intent setup = new Intent(context, BlueOSCountdownConfigActivity.class)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        rv.setOnClickPendingIntent(android.R.id.background, PendingIntent.getActivity(
                context, id, setup, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));
        WidgetThemeManager.applyBackground(context, rv);
        wm.updateAppWidget(id, rv);
    }

    private static LocalDate nextDate(LocalDate picked, LocalDate today, boolean yearly) {
        if (!yearly) return picked;
        LocalDate next = picked.withYear(today.getYear());
        if (next.isBefore(today)) next = next.plusYears(1);
        return next;
    }

    private static void scheduleMidnight(Context context) {
        final AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        final long nextMidnight = LocalDate.now(ZoneId.systemDefault())
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        am.setExactAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, midnightIntent(context));
    }

    private static PendingIntent midnightIntent(Context context) {
        return PendingIntent.getBroadcast(context, 0,
                new Intent(ACTION_MIDNIGHT).setClass(context, BlueOSCountdownWidgetProvider.class),
                PendingIntent.FLAG_IMMUTABLE);
    }
}
