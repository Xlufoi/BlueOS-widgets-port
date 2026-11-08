package org.blueos.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.widget.RemoteViews;

import java.util.Calendar;

public class BlueOSAronaPetWidgetProvider extends AppWidgetProvider {

    private static final String[] GREETINGS_RU = {
            "Сэнсэй, продуктивного дня! Арона всегда рядом.",
            "Не забывайте отдыхать, Сэнсэй!",
            "Сегодня отличная погода в Кивотосе!",
            "Сэнсэй, поступили новые отчёты из Schale!",
            "С возвращением, Сэнсэй! Жду ваших указаний.",
            "Время чая и клубничного молока!",
    };

    private static final String[] GREETINGS_EN = {
            "Sensei, have a productive day! Arona is always here.",
            "Don't forget to take a break, Sensei!",
            "The weather in Kivotos is wonderful today!",
            "Sensei, new reports from Schale just arrived!",
            "Welcome back, Sensei! Awaiting your orders.",
            "Time for tea and strawberry milk!",
    };

    private static Bitmap sAronaRaw = null;
    private static Bitmap sAnimBmp = null;
    private static Canvas sAnimCanvas = null;
    private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final Matrix sMatrix = new Matrix();

    protected int getLayoutId() {
        return R.layout.blueos_arona_widget;
    }

    protected int getStickerRes() {
        return R.drawable.blueos_arona_sticker;
    }

    protected String getCharacterName(boolean isRu) {
        return isRu ? "Арона · Schale OS" : "Arona · Schale OS";
    }

    protected String getNightLine(boolean isRu) {
        return isRu ? "Сэнсэй, уже поздно... Пора спать. Доброй ночи!"
                    : "Sensei, it's late... Time to sleep. Good night!";
    }

    protected String[] getGreetings(boolean isRu) {
        return isRu ? GREETINGS_RU : GREETINGS_EN;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] ids) {
        for (int id : ids) {
            update(context, wm, id, 0, false);
        }
    }

    public void update(Context context, AppWidgetManager wm, int id, int animStep, boolean isBeat) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), getLayoutId());
        boolean isRu = LanguageManager.isRussian(context);
        boolean playing = BlueOSMediaWidgetProvider.isPlaying(context);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int idx = (int) (System.currentTimeMillis() / (1000 * 60 * 60)) % getGreetings(isRu).length;
        String line = getGreetings(isRu)[Math.abs(idx)];

        if (hour >= 23 || hour < 6) {
            line = getNightLine(isRu);
        }

        if (playing) {
            String title = BlueOSMediaWidgetProvider.getTrackTitle();
            String artist = BlueOSMediaWidgetProvider.getTrackArtist();
            if (title != null && !title.isEmpty()) {
                line = "♫ " + title + (artist != null && !artist.isEmpty() ? " — " + artist : "");
            } else {
                line = isRu ? "♫ Слушает музыку вместе с вами..." : "♫ Rocking to the beat...";
            }
        }

        rv.setTextViewText(R.id.blueos_arona_title, getCharacterName(isRu));
        rv.setTextViewText(R.id.blueos_arona_status, line);

        // Beat rocking animation
        if (playing) {
            Bitmap swayed = renderSwayFrame(context, getStickerRes(), animStep);
            if (swayed != null) {
                rv.setImageViewBitmap(R.id.blueos_arona_avatar, swayed);
            }
        } else {
            rv.setImageViewResource(R.id.blueos_arona_avatar, getStickerRes());
        }

        // Tap launches Arona Companion app
        Intent launch = context.getPackageManager().getLaunchIntentForPackage("org.blueos.arona");
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            rv.setOnClickPendingIntent(android.R.id.background,
                    PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_IMMUTABLE));
        }

        WidgetThemeManager.applyBackground(context, rv);
        wm.updateAppWidget(id, rv);
    }

    public static Bitmap renderSwayFrame(Context context, int resId, int step) {
        try {
            if (sAronaRaw == null) {
                sAronaRaw = BitmapFactory.decodeResource(context.getResources(), resId);
            }
            if (sAronaRaw == null) return null;

            int size = 256;
            if (sAnimBmp == null) {
                sAnimBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
                sAnimCanvas = new Canvas(sAnimBmp);
            }

            sAnimBmp.eraseColor(0);

            // Rocking / bobbing motion: angle tilts left/right, scale pulses
            float angle = (float) Math.sin(step * 0.20f) * 8.0f;
            float scale = 1.0f + 0.05f * (float) Math.abs(Math.sin(step * 0.20f));

            sMatrix.reset();
            float rawW = sAronaRaw.getWidth();
            float rawH = sAronaRaw.getHeight();
            float baseScale = Math.min((size * 0.85f) / rawW, (size * 0.85f) / rawH);

            sMatrix.postTranslate(-rawW / 2f, -rawH);
            sMatrix.postRotate(angle);
            sMatrix.postScale(baseScale * scale, baseScale * scale);
            sMatrix.postTranslate(size / 2f, size * 0.95f);

            sAnimCanvas.drawBitmap(sAronaRaw, sMatrix, sPaint);
            return sAnimBmp;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void tickMusic(Context context, int animStep) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        int[] aronaIds = wm.getAppWidgetIds(new ComponentName(context, BlueOSAronaPetWidgetProvider.class));
        if (aronaIds != null && aronaIds.length > 0) {
            BlueOSAronaPetWidgetProvider prov = new BlueOSAronaPetWidgetProvider();
            for (int id : aronaIds) {
                prov.update(context, wm, id, animStep, true);
            }
        }
        int[] planaIds = wm.getAppWidgetIds(new ComponentName(context, BlueOSPlanaPetWidgetProvider.class));
        if (planaIds != null && planaIds.length > 0) {
            BlueOSPlanaPetWidgetProvider prov = new BlueOSPlanaPetWidgetProvider();
            for (int id : planaIds) {
                prov.update(context, wm, id, animStep, true);
            }
        }
    }
}
