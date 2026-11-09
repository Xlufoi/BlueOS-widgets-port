package org.blueos.widgets;

import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.text.format.DateFormat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AlarmHelper {

    public static class AlarmInfo {
        public final String timeStr;
        public final String subtext;
        public final boolean hasAlarm;

        public AlarmInfo(String timeStr, String subtext, boolean hasAlarm) {
            this.timeStr = timeStr;
            this.subtext = subtext;
            this.hasAlarm = hasAlarm;
        }
    }

    public static AlarmInfo getNextAlarm(Context context) {
        // 1. Check AlarmManager.getNextAlarmClock() (Standard Android API)
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                AlarmManager.AlarmClockInfo clockInfo = am.getNextAlarmClock();
                if (clockInfo != null && clockInfo.getTriggerTime() > 0) {
                    long triggerTime = clockInfo.getTriggerTime();
                    Date date = new Date(triggerTime);
                    String time = DateFormat.getTimeFormat(context).format(date);
                    String day = formatAlarmDay(context, triggerTime);
                    return new AlarmInfo(time, day, true);
                }
            }
        } catch (Throwable ignored) {}

        // 2. Check Settings.System next_alarm_formatted (MIUI / HyperOS legacy setting)
        try {
            String miuiFormatted = Settings.System.getString(context.getContentResolver(), "next_alarm_formatted");
            if (miuiFormatted != null && !miuiFormatted.trim().isEmpty()) {
                String clean = miuiFormatted.trim();
                return new AlarmInfo(clean, "HyperOS", true);
            }
        } catch (Throwable ignored) {}

        // 3. Query Xiaomi DeskClock content provider (content://com.android.deskclock/alarm)
        AlarmInfo miuiDbAlarm = queryXiaomiDeskClock(context);
        if (miuiDbAlarm != null) {
            return miuiDbAlarm;
        }

        // 4. Fallback: Try reading via root dumpsys if user has root
        AlarmInfo rootAlarm = queryRootAlarm();
        if (rootAlarm != null) {
            return rootAlarm;
        }

        return new AlarmInfo("—", context.getString(R.string.blueos_clock_widget_no_alarms), false);
    }

    private static String formatAlarmDay(Context context, long triggerTime) {
        Calendar now = Calendar.getInstance();
        Calendar alarmCal = Calendar.getInstance();
        alarmCal.setTimeInMillis(triggerTime);

        long diffDays = alarmCal.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR);
        if (alarmCal.get(Calendar.YEAR) > now.get(Calendar.YEAR)) {
            diffDays += 365;
        }

        boolean isRu = LanguageManager.isRussian(context);
        if (diffDays == 0) {
            return context.getString(R.string.blueos_countdown_today);
        } else if (diffDays == 1) {
            return isRu ? "Завтра" : "Tomorrow";
        } else {
            return DateFormat.format(DateFormat.getBestDateTimePattern(LanguageManager.getLocale(context), "EEEMMMd"), new Date(triggerTime)).toString();
        }
    }

    private static AlarmInfo queryXiaomiDeskClock(Context context) {
        String[] uris = new String[] {
                "content://com.android.deskclock/alarm",
                "content://com.android.deskclock/alarm_clock",
                "content://com.google.android.deskclock/alarm"
        };

        ContentResolver cr = context.getContentResolver();
        for (String uriStr : uris) {
            Cursor cursor = null;
            try {
                Uri uri = Uri.parse(uriStr);
                cursor = cr.query(uri, null, "enabled=1", null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int hourIdx = cursor.getColumnIndex("hour");
                    int minIdx = cursor.getColumnIndex("minutes");
                    if (hourIdx == -1) hourIdx = cursor.getColumnIndex("alarm_hour");
                    if (minIdx == -1) minIdx = cursor.getColumnIndex("alarm_minute");

                    if (hourIdx != -1 && minIdx != -1) {
                        int earliestMinutesFromNow = Integer.MAX_VALUE;
                        int bestHour = 0;
                        int bestMin = 0;

                        Calendar now = Calendar.getInstance();
                        int nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

                        do {
                            int h = cursor.getInt(hourIdx);
                            int m = cursor.getInt(minIdx);
                            int targetMin = h * 60 + m;
                            int diff = targetMin - nowMin;
                            if (diff <= 0) diff += 24 * 60; // Next day
                            if (diff < earliestMinutesFromNow) {
                                earliestMinutesFromNow = diff;
                                bestHour = h;
                                bestMin = m;
                            }
                        } while (cursor.moveToNext());

                        Calendar targetCal = Calendar.getInstance();
                        targetCal.set(Calendar.HOUR_OF_DAY, bestHour);
                        targetCal.set(Calendar.MINUTE, bestMin);
                        targetCal.set(Calendar.SECOND, 0);

                        String timeStr = DateFormat.getTimeFormat(context).format(targetCal.getTime());
                        String day = formatAlarmDay(context, targetCal.getTimeInMillis());
                        return new AlarmInfo(timeStr, day, true);
                    }
                }
            } catch (Throwable ignored) {
            } finally {
                if (cursor != null) {
                    try { cursor.close(); } catch (Throwable ignored) {}
                }
            }
        }
        return null;
    }

    private static AlarmInfo queryRootAlarm() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "dumpsys alarm | grep -E 'Next alarm|AlarmClockInfo|deskclock' | head -n 5"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                // Example lines:
                // Next alarm clock: AlarmClockInfo [triggerTime=..., showIntent=...]
                // or contains HH:mm
                if (line.contains("triggerTime=")) {
                    int start = line.indexOf("triggerTime=") + 12;
                    int end = line.indexOf(",", start);
                    if (end == -1) end = line.indexOf(" ", start);
                    if (end > start) {
                        long trigger = Long.parseLong(line.substring(start, end).trim());
                        if (trigger > 0) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTimeInMillis(trigger);
                            String time = String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
                            return new AlarmInfo(time, "HyperOS Root", true);
                        }
                    }
                }
            }
            p.waitFor();
        } catch (Throwable ignored) {}
        return null;
    }
}
