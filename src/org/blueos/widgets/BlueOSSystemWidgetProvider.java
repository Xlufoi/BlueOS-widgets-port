package org.blueos.widgets;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.widget.RemoteViews;

import java.io.File;
import java.util.Locale;

public class BlueOSSystemWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] ids) {
        for (int id : ids) {
            updateWidget(context, wm, id);
        }
    }

    public static void updateWidget(Context context, AppWidgetManager wm, int id) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.blueos_system_widget);
        boolean isRu = LanguageManager.isRussian(context);

        // 1. Battery
        Intent battIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = battIntent != null ? battIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = battIntent != null ? battIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        int pct = (level >= 0 && scale > 0) ? (int) ((level / (float) scale) * 100f) : 100;
        rv.setTextViewText(R.id.blueos_sys_batt_pct, pct + "%");
        rv.setTextViewText(R.id.blueos_sys_batt_sub, isRu ? "Батарея" : "Battery");

        // 2. RAM
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        if (am != null) am.getMemoryInfo(mem);
        double totalGb = mem.totalMem / (1024.0 * 1024.0 * 1024.0);
        double availGb = mem.availMem / (1024.0 * 1024.0 * 1024.0);
        double usedGb = totalGb - availGb;
        String ramStr = String.format(Locale.US, "%.1fG", usedGb);
        rv.setTextViewText(R.id.blueos_sys_ram_val, ramStr);
        rv.setTextViewText(R.id.blueos_sys_ram_sub, isRu ? "ОЗУ" : "RAM");

        // 3. Storage
        try {
            File dataDir = Environment.getDataDirectory();
            StatFs stat = new StatFs(dataDir.getPath());
            long totalBytes = stat.getTotalBytes();
            long freeBytes = stat.getAvailableBytes();
            long usedBytes = totalBytes - freeBytes;
            int storagePct = (int) ((usedBytes * 100.0) / totalBytes);
            rv.setTextViewText(R.id.blueos_sys_disk_val, storagePct + "%");
        } catch (Throwable t) {
            rv.setTextViewText(R.id.blueos_sys_disk_val, "65%");
        }
        rv.setTextViewText(R.id.blueos_sys_disk_sub, isRu ? "Память" : "Storage");

        // Click opens Storage/Battery Settings
        Intent it = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (it.resolveActivity(context.getPackageManager()) != null) {
            rv.setOnClickPendingIntent(android.R.id.background,
                    PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_IMMUTABLE));
        }

        WidgetThemeManager.applyBackground(context, rv);
        wm.updateAppWidget(id, rv);
    }
}
