package org.blueos.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import java.util.Calendar;

public class BlueOSDuoPetWidgetProvider extends AppWidgetProvider {

    private static final String[] DUO_GREETINGS_RU = {
            "Сэнсэй, мы обе здесь! Готовы помогать в любых делах.",
            "Арона: «Сэнсэй, продуктивного дня!» · Плана: «Отчёты готовы.»",
            "Двойная поддержка Schale OS к вашим услугам, Сэнсэй!",
            "Арона заварила чай, а Плана проверила безопасность терминала.",
            "С возвращением, Сэнсэй! Мы очень вас ждали.",
    };

    private static final String[] DUO_GREETINGS_EN = {
            "Sensei, we are both here! Ready to help with any mission.",
            "Arona: “Productive day, Sensei!” · Plana: “Reports verified.”",
            "Double Schale OS support at your service, Sensei!",
            "Arona brewed the tea, while Plana verified terminal security.",
            "Welcome back, Sensei! We were waiting for you.",
    };

    protected boolean isTransparent() {
        return false;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] ids) {
        for (int id : ids) {
            update(context, wm, id, 0, isTransparent());
        }
    }

    public static void update(Context context, AppWidgetManager wm, int id, int animStep) {
        update(context, wm, id, animStep, false);
    }

    public static void update(Context context, AppWidgetManager wm, int id, int animStep, boolean isTransparent) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.blueos_duo_pet_widget);
        boolean isRu = LanguageManager.isRussian(context);
        boolean playing = BlueOSMediaWidgetProvider.isPlaying(context);

        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String[] greetings = isRu ? DUO_GREETINGS_RU : DUO_GREETINGS_EN;
        int idx = (int) (System.currentTimeMillis() / (1000 * 60 * 60)) % greetings.length;
        String line = greetings[Math.abs(idx)];

        if (hour >= 23 || hour < 6) {
            line = isRu ? "Сэнсэй, уже ночь... Арона спит, а Плана держит дежурство."
                        : "Sensei, it is night... Arona is asleep and Plana is on watch.";
        }

        if (playing) {
            String title = BlueOSMediaWidgetProvider.getTrackTitle();
            String artist = BlueOSMediaWidgetProvider.getTrackArtist();
            if (title != null && !title.isEmpty()) {
                line = "♫ " + title + (artist != null && !artist.isEmpty() ? " — " + artist : "");
            } else {
                line = isRu ? "♫ Слушают музыку вместе с Сэнсэем..." : "♫ Rocking to the beat with Sensei...";
            }
        }

        rv.setTextViewText(R.id.blueos_duo_title, isRu ? "Арона & Плана · Schale" : "Arona & Plana · Schale");
        rv.setTextViewText(R.id.blueos_duo_status, line);

        if (playing) {
            Bitmap aronaBmp = BlueOSAronaPetWidgetProvider.renderSwayFrame(context, R.drawable.blueos_arona_sticker, animStep);
            Bitmap planaBmp = BlueOSAronaPetWidgetProvider.renderSwayFrame(context, R.drawable.blueos_plana_sticker, animStep + 4);
            if (aronaBmp != null) rv.setImageViewBitmap(R.id.blueos_arona_avatar, aronaBmp);
            if (planaBmp != null) rv.setImageViewBitmap(R.id.blueos_plana_avatar, planaBmp);
        } else {
            rv.setImageViewResource(R.id.blueos_arona_avatar, R.drawable.blueos_arona_sticker);
            rv.setImageViewResource(R.id.blueos_plana_avatar, R.drawable.blueos_plana_sticker);
        }

        Intent launch = context.getPackageManager().getLaunchIntentForPackage("org.blueos.arona");
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            rv.setOnClickPendingIntent(android.R.id.background,
                    PendingIntent.getActivity(context, id, launch, PendingIntent.FLAG_IMMUTABLE));
        }

        WidgetThemeManager.applyBackground(context, rv, isTransparent);
        wm.updateAppWidget(id, rv);
    }

    public static void tickMusic(Context context, int animStep) {
        AppWidgetManager wm = AppWidgetManager.getInstance(context);
        int[] ids = wm.getAppWidgetIds(new ComponentName(context, BlueOSDuoPetWidgetProvider.class));
        if (ids != null && ids.length > 0) {
            for (int id : ids) {
                update(context, wm, id, animStep, false);
            }
        }
        int[] transIds = wm.getAppWidgetIds(new ComponentName(context, BlueOSDuoPetWidgetProvider.Transparent.class));
        if (transIds != null && transIds.length > 0) {
            for (int id : transIds) {
                update(context, wm, id, animStep, true);
            }
        }
    }

    public static class Transparent extends BlueOSDuoPetWidgetProvider {
        @Override
        protected boolean isTransparent() {
            return true;
        }
    }
}
