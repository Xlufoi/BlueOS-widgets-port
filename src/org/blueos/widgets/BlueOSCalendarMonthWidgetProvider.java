package org.blueos.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;

public class BlueOSCalendarMonthWidgetProvider extends AppWidgetProvider {

    private static final String[] DAYS_RU = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
    private static final String[] DAYS_EN = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"};

    private static final String[] MONTHS_RU = {
            "ЯНВАРЬ", "ФЕВРАЛЬ", "МАРТ", "АПРЕЛЬ", "МАЙ", "ИЮНЬ",
            "ИЮЛЬ", "АВГУСТ", "СЕНТЯБРЬ", "ОКТЯБРЬ", "НОЯБРЬ", "ДЕКАБРЬ"
    };

    private static final String[] MONTHS_EN = {
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
    };

    protected boolean isTransparent() {
        return false;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] ids) {
        for (int id : ids) {
            updateWidget(context, wm, id, isTransparent());
        }
    }

    public static void push(Context context) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        int[] idsCard = wm.getAppWidgetIds(new ComponentName(context, BlueOSCalendarMonthWidgetProvider.class));
        if (idsCard != null && idsCard.length > 0) {
            new BlueOSCalendarMonthWidgetProvider().onUpdate(context, wm, idsCard);
        }
        int[] idsTrans = wm.getAppWidgetIds(new ComponentName(context, BlueOSCalendarMonthWidgetProvider.Transparent.class));
        if (idsTrans != null && idsTrans.length > 0) {
            new BlueOSCalendarMonthWidgetProvider.Transparent().onUpdate(context, wm, idsTrans);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager wm, int id) {
        updateWidget(context, wm, id, false);
    }

    public static void updateWidget(Context context, AppWidgetManager wm, int id, boolean isTransparent) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.blueos_calendar_month_widget);

        Bitmap gridBmp = renderMonthGrid(context, 700, 500);
        rv.setImageViewBitmap(R.id.blueos_calendar_grid, gridBmp);

        Intent open = new Intent(Intent.ACTION_VIEW)
                .setData(Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (open.resolveActivity(context.getPackageManager()) != null) {
            rv.setOnClickPendingIntent(android.R.id.background,
                    PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE));
        }

        WidgetThemeManager.applyBackground(context, rv, isTransparent);
        wm.updateAppWidget(id, rv);
    }

    public static class Transparent extends BlueOSCalendarMonthWidgetProvider {
        @Override
        protected boolean isTransparent() {
            return true;
        }
    }

    private static Bitmap renderMonthGrid(Context context, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        boolean isRu = LanguageManager.isRussian(context);
        Locale locale = LanguageManager.getLocale(context);

        Calendar cal = Calendar.getInstance(locale);
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int currentMonth = cal.get(Calendar.MONTH);
        int currentYear = cal.get(Calendar.YEAR);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);

        // Header: Month & Year
        paint.setColor(WidgetThemeManager.getAccentColor(context));
        paint.setTextSize(36f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        String monthName = (isRu ? MONTHS_RU[currentMonth] : MONTHS_EN[currentMonth]) + " " + currentYear;
        canvas.drawText(monthName, width / 2f, 48f, paint);

        // Days of week header
        String[] daysHeader = isRu ? DAYS_RU : DAYS_EN;
        paint.setColor(Color.parseColor("#94A3B8"));
        paint.setTextSize(24f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        float colWidth = width / 7f;
        float headerY = 100f;
        for (int i = 0; i < 7; i++) {
            canvas.drawText(daysHeader[i], (i + 0.5f) * colWidth, headerY, paint);
        }

        // Days grid calculation
        Calendar gridCal = (Calendar) cal.clone();
        gridCal.set(Calendar.DAY_OF_MONTH, 1);
        int firstDayOfWeek = gridCal.get(Calendar.DAY_OF_WEEK); // 1 = Sunday, 2 = Monday...
        int startCol = (firstDayOfWeek + 5) % 7; // Monday = 0, Sunday = 6
        int maxDays = gridCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        float rowHeight = 65f;
        float startY = 160f;

        paint.setTextSize(26f);
        RectF circleRect = new RectF();
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(WidgetThemeManager.getAccentColor(context));

        for (int d = 1; d <= maxDays; d++) {
            int slot = startCol + d - 1;
            int col = slot % 7;
            int row = slot / 7;

            float cx = (col + 0.5f) * colWidth;
            float cy = startY + row * rowHeight;

            if (d == currentDay) {
                // Today accent circle
                float radius = 24f;
                circleRect.set(cx - radius, cy - radius - 8f, cx + radius, cy + radius - 8f);
                canvas.drawOval(circleRect, circlePaint);

                paint.setColor(Color.BLACK);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            } else {
                paint.setColor(Color.WHITE);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            }

            canvas.drawText(String.valueOf(d), cx, cy, paint);
        }

        return bmp;
    }
}
