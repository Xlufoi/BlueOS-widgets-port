package org.blueos.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

import java.io.File;

public class BlueOSPhotoWidgetProvider extends AppWidgetProvider {

    public static final String PREFS = "blueos_photo_widgets";

    public static class Small extends BlueOSPhotoWidgetProvider {}
    public static class Square extends BlueOSPhotoWidgetProvider {}
    public static class Portrait extends BlueOSPhotoWidgetProvider {}
    public static class Wide extends BlueOSPhotoWidgetProvider {}

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        for (int id : widgetIds) {
            update(context, wm, id);
        }
    }

    @Override
    public void onDeleted(Context context, int[] widgetIds) {
        SharedPreferences.Editor ed = prefs(context).edit();
        for (int id : widgetIds) {
            File f = photoFile(context, id);
            if (f != null && f.exists()) f.delete();
            ed.remove(keyFile(id)).remove(keyShape(id));
        }
        ed.apply();
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String keyFile(int id) {
        return "file_" + id;
    }

    public static String keyShape(int id) {
        return "shape_" + id;
    }

    public static File photosDir(Context context) {
        File d = new File(context.getFilesDir(), "widget_photos");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static File photoFile(Context context, int id) {
        String name = prefs(context).getString(keyFile(id), null);
        return name != null ? new File(photosDir(context), name) : null;
    }

    public static void update(Context context, AppWidgetManager wm, int id) {
        String shape = prefs(context).getString(keyShape(id), "ROUNDED");
        int layoutId;
        if ("CIRCLE".equals(shape)) {
            layoutId = R.layout.blueos_photo_widget_circle;
        } else if ("STICKER".equals(shape)) {
            layoutId = R.layout.blueos_photo_widget_sticker;
        } else {
            layoutId = R.layout.blueos_photo_widget_rounded;
        }

        RemoteViews rv = new RemoteViews(context.getPackageName(), layoutId);
        File file = photoFile(context, id);
        if (file != null && file.exists()) {
            Bitmap b = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (b != null) {
                rv.setImageViewBitmap(R.id.blueos_photo, b);
            } else {
                rv.setImageViewResource(R.id.blueos_photo, R.drawable.blueos_arona_sticker);
            }
        } else {
            // Default built-in Arona sticker!
            rv.setImageViewResource(R.id.blueos_photo, R.drawable.blueos_arona_sticker);
        }

        Intent config = new Intent(context, BlueOSPhotoConfigActivity.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        rv.setOnClickPendingIntent(android.R.id.background,
                PendingIntent.getActivity(context, id, config, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        wm.updateAppWidget(id, rv);
    }
}
