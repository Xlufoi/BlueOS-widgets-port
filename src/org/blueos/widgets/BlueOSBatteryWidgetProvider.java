package org.blueos.widgets;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.view.View;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.util.Set;

public abstract class BlueOSBatteryWidgetProvider extends AppWidgetProvider {

    private static boolean sRegistered = false;
    private static int sLevel = -1;
    private static boolean sPlugged = false;

    protected abstract int getLayoutId();

    protected boolean isOutlineOnly() {
        return false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        initReceiver(context);
        updateAll(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        initReceiver(context);
        updateAll(context);
    }

    private static final BroadcastReceiver sReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                if (scale <= 0) scale = 100;
                int rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int newLevel = (rawLevel * 100) / scale;
                boolean newPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

                if (newLevel == sLevel && newPlugged == sPlugged) return;
                sLevel = newLevel;
                sPlugged = newPlugged;
                updateAll(context);
            }
        }
    };

    private static void initReceiver(Context context) {
        if (!sRegistered) {
            Context app = context.getApplicationContext();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_POWER_CONNECTED);
            filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            filter.addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED");
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            Intent sticky = app.registerReceiver(sReceiver, filter);
            if (sticky != null) {
                sReceiver.onReceive(app, sticky);
            }
            sRegistered = true;
        }
    }

    @SuppressLint("MissingPermission")
    private static int getHeadsetBattery(Context context) {
        try {
            BluetoothManager bm = context.getSystemService(BluetoothManager.class);
            if (bm != null && bm.getAdapter() != null) {
                Set<BluetoothDevice> bonded = bm.getAdapter().getBondedDevices();
                if (bonded != null) {
                    for (BluetoothDevice dev : bonded) {
                        BluetoothClass bc = dev.getBluetoothClass();
                        if (bc != null && bc.getMajorDeviceClass() == BluetoothClass.Device.Major.AUDIO_VIDEO) {
                            try {
                                java.lang.reflect.Method m = dev.getClass().getMethod("getBatteryLevel");
                                Integer level = (Integer) m.invoke(dev);
                                if (level != null && level >= 0) return level;
                            } catch (Throwable ignored) {}
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    public static void updateAll(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager wm = AppWidgetManager.getInstance(app);

        if (sLevel < 0) {
            BatteryManager bm = (BatteryManager) app.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                sLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            }
            if (sLevel < 0) sLevel = 100;
        }

        // Filled card variants
        updateProvider(app, wm, BlueOSBatteryWidgetProvider.Large.class, R.layout.blueos_battery_widget_large, false);
        updateProvider(app, wm, BlueOSBatteryWidgetProvider.Medium.class, R.layout.blueos_battery_widget_medium, false);
        updateProvider(app, wm, BlueOSBatteryWidgetProvider.Small.class, R.layout.blueos_battery_widget_small, false);

        // Minimalist outline variants
        updateProvider(app, wm, BlueOSBatteryWidgetProvider.LargeOutline.class, R.layout.blueos_battery_widget_large_outline, true);
        updateProvider(app, wm, BlueOSBatteryWidgetProvider.MediumOutline.class, R.layout.blueos_battery_widget_medium_outline, true);
        updateProvider(app, wm, BlueOSBatteryWidgetProvider.SmallOutline.class, R.layout.blueos_battery_widget_small_outline, true);
    }

    private static void updateProvider(Context app, AppWidgetManager wm, Class<?> cls, int layoutId, boolean isOutline) {
        ComponentName provider = new ComponentName(app, cls);
        int[] ids = wm.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return;

        int headset = getHeadsetBattery(app);

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(app.getPackageName(), layoutId);
            rv.setProgressBar(R.id.blueos_battery_ring, 100, sLevel, false);
            rv.setTextViewText(R.id.blueos_battery_level, sLevel + "%");
            rv.setViewVisibility(R.id.blueos_battery_charging, sPlugged ? View.VISIBLE : View.GONE);

            // Headset views if present in layout
            try {
                rv.setViewVisibility(R.id.blueos_headset, headset >= 0 ? View.VISIBLE : View.GONE);
                if (headset >= 0) {
                    rv.setTextViewText(R.id.blueos_headset_level, headset + "%");
                }
            } catch (Throwable ignored) {}

            // Time full in
            try {
                if (sPlugged) {
                    rv.setTextViewText(R.id.blueos_battery_status, app.getString(R.string.blueos_battery_widget_charging));
                } else if (sLevel >= 100) {
                    rv.setTextViewText(R.id.blueos_battery_status, app.getString(R.string.blueos_battery_widget_full));
                }
            } catch (Throwable ignored) {}

            Intent usage = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
            usage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (usage.resolveActivity(app.getPackageManager()) != null) {
                rv.setOnClickPendingIntent(
                        android.R.id.background,
                        PendingIntent.getActivity(app, 0, usage, PendingIntent.FLAG_IMMUTABLE)
                );
            }
            WidgetThemeManager.applyBackground(app, rv, isOutline);
            wm.updateAppWidget(id, rv);
        }
    }

    public static class Small extends BlueOSBatteryWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_battery_widget_small;
        }
    }

    public static class SmallOutline extends BlueOSBatteryWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_battery_widget_small_outline;
        }

        @Override
        protected boolean isOutlineOnly() {
            return true;
        }
    }

    public static class Medium extends BlueOSBatteryWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_battery_widget_medium;
        }
    }

    public static class MediumOutline extends BlueOSBatteryWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_battery_widget_medium_outline;
        }

        @Override
        protected boolean isOutlineOnly() {
            return true;
        }
    }

    public static class Large extends BlueOSBatteryWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_battery_widget_large;
        }
    }

    public static class LargeOutline extends BlueOSBatteryWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_battery_widget_large_outline;
        }

        @Override
        protected boolean isOutlineOnly() {
            return true;
        }
    }
}
