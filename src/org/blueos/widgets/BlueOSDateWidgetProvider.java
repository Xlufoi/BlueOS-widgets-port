package org.blueos.widgets;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Instances;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BlueOSDateWidgetProvider extends AppWidgetProvider {

    private static final String ACTION_REFRESH = "org.blueos.widgets.action.DATE_WIDGET_REFRESH";
    private static final int MAX_EVENTS = 2;
    private static final long LOOK_AHEAD_MILLIS = 7 * DateUtils.DAY_IN_MILLIS;

    private static final String[] PROJECTION = {
            Instances.TITLE,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Instances.END_DAY,
    };
    private static final int INDEX_TITLE = 0;
    private static final int INDEX_BEGIN = 1;
    private static final int INDEX_END = 2;
    private static final int INDEX_ALL_DAY = 3;
    private static final int INDEX_END_DAY = 4;

    private static final int[][] EVENT_VIEWS = {
            {R.id.blueos_event1, R.id.blueos_event1_title, R.id.blueos_event1_time},
            {R.id.blueos_event2, R.id.blueos_event2_title, R.id.blueos_event2_time},
    };

    private static final class Event {
        final String title;
        final long begin;
        final long end;
        final boolean allDay;

        Event(String title, long begin, long end, boolean allDay) {
            this.title = title;
            this.begin = begin;
            this.end = end;
            this.allDay = allDay;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        final String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || Intent.ACTION_PROVIDER_CHANGED.equals(action)
                || Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            final AppWidgetManager wm = AppWidgetManager.getInstance(context);
            onUpdate(context, wm, wm.getAppWidgetIds(new ComponentName(context, getClass())));
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        if (widgetIds.length == 0) return;
        final List<Event> events = loadEvents(context);
        final RemoteViews rv = buildViews(context, events);
        WidgetThemeManager.applyBackground(context, rv);
        for (int widgetId : widgetIds) {
            wm.updateAppWidget(widgetId, rv);
        }
        scheduleRefresh(context, events);
    }

    public static void push(Context context) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        int[] ids = wm.getAppWidgetIds(new ComponentName(context, BlueOSDateWidgetProvider.class));
        if (ids != null && ids.length > 0) {
            new BlueOSDateWidgetProvider().onUpdate(context, wm, ids);
        }
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(refreshIntent(context));
        }
    }

    private RemoteViews buildViews(Context context, List<Event> events) {
        final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.blueos_date_widget);
        final Locale locale = Locale.getDefault();
        setClockFormat(rv, R.id.blueos_day, "d");
        setClockFormat(rv, R.id.blueos_weekday, DateFormat.getBestDateTimePattern(locale, "EEEE"));
        setClockFormat(rv, R.id.blueos_month, DateFormat.getBestDateTimePattern(locale, "LLLL"));

        final boolean canRead = canReadCalendar(context);
        for (int i = 0; i < EVENT_VIEWS.length; i++) {
            final int[] views = EVENT_VIEWS[i];
            if (i < events.size()) {
                final Event event = events.get(i);
                rv.setViewVisibility(views[0], View.VISIBLE);
                rv.setTextViewText(views[1], event.title);
                rv.setTextViewText(views[2], formatWhen(context, event));
            } else {
                rv.setViewVisibility(views[0], View.GONE);
            }
        }
        rv.setViewVisibility(R.id.blueos_no_events, events.isEmpty() ? View.VISIBLE : View.GONE);
        rv.setTextViewText(R.id.blueos_no_events, context.getString(canRead
                ? R.string.blueos_date_widget_no_events
                : R.string.blueos_date_widget_no_access));

        Intent open = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (open.resolveActivity(context.getPackageManager()) != null) {
            rv.setOnClickPendingIntent(android.R.id.background,
                    PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE));
        }
        return rv;
    }

    private static void setClockFormat(RemoteViews rv, int viewId, String format) {
        rv.setCharSequence(viewId, "setFormat12Hour", format);
        rv.setCharSequence(viewId, "setFormat24Hour", format);
    }

    private static boolean canReadCalendar(Context context) {
        return context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static List<Event> loadEvents(Context context) {
        final List<Event> events = new ArrayList<>();
        if (!canReadCalendar(context)) {
            return events;
        }
        final long now = System.currentTimeMillis();
        final Uri.Builder uri = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(uri, now - DateUtils.DAY_IN_MILLIS);
        ContentUris.appendId(uri, now + LOOK_AHEAD_MILLIS);
        final int today = julianDay(now);
        try (Cursor c = context.getContentResolver().query(uri.build(), PROJECTION,
                Instances.VISIBLE + "=1", null, Instances.BEGIN + " ASC")) {
            while (c != null && c.moveToNext() && events.size() < MAX_EVENTS) {
                final boolean allDay = c.getInt(INDEX_ALL_DAY) != 0;
                final long end = c.getLong(INDEX_END);
                final boolean ended = allDay ? c.getInt(INDEX_END_DAY) < today : end <= now;
                if (!ended) {
                    events.add(new Event(c.getString(INDEX_TITLE), c.getLong(INDEX_BEGIN), end, allDay));
                }
            }
        } catch (RuntimeException ignored) {}
        return events;
    }

    private static int julianDay(long millis) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        final long offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
        return (int) ((millis + offset) / DateUtils.DAY_IN_MILLIS) + 2440588;
    }

    private static String formatWhen(Context context, Event event) {
        final long now = System.currentTimeMillis();
        if (event.allDay) {
            if (julianDayUtc(event.begin) <= julianDay(now)) {
                return DateUtils.formatDateTime(context, event.begin, DateUtils.FORMAT_SHOW_DATE);
            }
            return DateUtils.formatDateTime(context, event.begin, DateUtils.FORMAT_SHOW_WEEKDAY
                    | DateUtils.FORMAT_ABBREV_WEEKDAY | DateUtils.FORMAT_UTC);
        }
        if (event.begin <= now || DateUtils.isToday(event.begin)) {
            return DateUtils.formatDateRange(context, event.begin, event.end, DateUtils.FORMAT_SHOW_TIME);
        }
        final String time = DateUtils.formatDateTime(context, event.begin, DateUtils.FORMAT_SHOW_TIME);
        if (DateUtils.isToday(event.begin - DateUtils.DAY_IN_MILLIS)) {
            return context.getString(R.string.blueos_date_widget_tomorrow, time);
        }
        return DateUtils.formatDateTime(context, event.begin, DateUtils.FORMAT_SHOW_WEEKDAY
                | DateUtils.FORMAT_ABBREV_WEEKDAY | DateUtils.FORMAT_SHOW_TIME);
    }

    private static int julianDayUtc(long millis) {
        return (int) (millis / DateUtils.DAY_IN_MILLIS) + 2440588;
    }

    private static void scheduleRefresh(Context context, List<Event> events) {
        final Calendar midnight = Calendar.getInstance();
        midnight.add(Calendar.DAY_OF_YEAR, 1);
        midnight.set(Calendar.HOUR_OF_DAY, 0);
        midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 1);
        long at = midnight.getTimeInMillis();
        for (Event event : events) {
            if (!event.allDay) {
                at = Math.min(at, event.end);
            }
        }
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.set(AlarmManager.RTC, at, refreshIntent(context));
        }
    }

    private static PendingIntent refreshIntent(Context context) {
        final Intent intent = new Intent(ACTION_REFRESH)
                .setClass(context, BlueOSDateWidgetProvider.class);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }
}
