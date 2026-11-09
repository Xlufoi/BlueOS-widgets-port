package org.blueos.widgets;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.RemoteViews;

public class BlueOSTogglesWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_TOGGLE_TORCH = "org.blueos.widgets.toggles.TORCH";
    public static final String ACTION_TOGGLE_DND = "org.blueos.widgets.toggles.DND";
    public static final String ACTION_TOGGLE_ROTATE = "org.blueos.widgets.toggles.ROTATE";
    public static final String ACTION_TOGGLE_DARK = "org.blueos.widgets.toggles.DARK";
    public static final String ACTION_TOGGLE_SAVER = "org.blueos.widgets.toggles.SAVER";

    private static boolean sTorchOn = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (action != null) {
            handleAction(context, action);
        }
        push(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        push(context);
    }

    private void handleAction(Context context, String action) {
        Context app = context.getApplicationContext();
        if (ACTION_TOGGLE_TORCH.equals(action)) {
            try {
                CameraManager cm = (CameraManager) app.getSystemService(Context.CAMERA_SERVICE);
                if (cm != null) {
                    for (String id : cm.getCameraIdList()) {
                        CameraCharacteristics cc = cm.getCameraCharacteristics(id);
                        Boolean flash = cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                        if (Boolean.TRUE.equals(flash)) {
                            sTorchOn = !sTorchOn;
                            cm.setTorchMode(id, sTorchOn);
                            break;
                        }
                    }
                }
            } catch (Throwable t) {
                runRoot("cmd flash switch");
            }
        } else if (ACTION_TOGGLE_DND.equals(action)) {
            boolean handled = false;
            try {
                NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.isNotificationPolicyAccessGranted()) {
                    int current = nm.getCurrentInterruptionFilter();
                    int next = (current == NotificationManager.INTERRUPTION_FILTER_ALL)
                            ? NotificationManager.INTERRUPTION_FILTER_PRIORITY
                            : NotificationManager.INTERRUPTION_FILTER_ALL;
                    nm.setInterruptionFilter(next);
                    handled = true;
                }
            } catch (Throwable ignored) {}
            if (!handled) {
                boolean dndOn = false;
                try {
                    NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
                    dndOn = nm != null && nm.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL;
                } catch (Throwable ignored) {}
                boolean rootSuccess = runRoot("cmd notification set_dnd " + (dndOn ? "0" : "1"));
                if (!rootSuccess) {
                    try {
                        Intent it = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        app.startActivity(it);
                    } catch (Throwable ignored) {}
                }
            }
        } else if (ACTION_TOGGLE_ROTATE.equals(action)) {
            boolean handled = false;
            try {
                if (Settings.System.canWrite(app)) {
                    int current = Settings.System.getInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                    Settings.System.putInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, current == 1 ? 0 : 1);
                    handled = true;
                }
            } catch (Throwable ignored) {}
            if (!handled) {
                int current = 0;
                try {
                    current = Settings.System.getInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                } catch (Throwable ignored) {}
                boolean rootSuccess = runRoot("settings put system accelerometer_rotation " + (current == 1 ? 0 : 1));
                if (!rootSuccess) {
                    try {
                        Intent it = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        it.setData(android.net.Uri.parse("package:" + app.getPackageName()));
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        app.startActivity(it);
                    } catch (Throwable ignored) {}
                }
            }
        } else if (ACTION_TOGGLE_DARK.equals(action)) {
            boolean darkOn = (app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
            boolean rootSuccess = runRoot("cmd uimode night " + (darkOn ? "no" : "yes"));
            if (!rootSuccess) {
                try {
                    UiModeManager umm = (UiModeManager) app.getSystemService(Context.UI_MODE_SERVICE);
                    if (umm != null) {
                        umm.setNightMode(darkOn ? UiModeManager.MODE_NIGHT_NO : UiModeManager.MODE_NIGHT_YES);
                    }
                } catch (Throwable ignored) {}
            }
        } else if (ACTION_TOGGLE_SAVER.equals(action)) {
            boolean saverOn = false;
            try {
                PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                saverOn = pm != null && pm.isPowerSaveMode();
            } catch (Throwable ignored) {}
            boolean rootSuccess = runRoot("cmd power set-mode " + (saverOn ? "0" : "1"));
            if (!rootSuccess) {
                try {
                    Intent it = new Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS);
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    app.startActivity(it);
                } catch (Throwable ignored) {}
            }
        }
    }

    private static boolean runRoot(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
            int exit = p.waitFor();
            return exit == 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    protected boolean isTransparent() {
        return false;
    }

    public static void push(Context context) {
        Context app = context.getApplicationContext();
        AppWidgetManager wm = AppWidgetManager.getInstance(app);

        updateProvider(app, wm, BlueOSTogglesWidgetProvider.class, false);
        updateProvider(app, wm, BlueOSTogglesWidgetProvider.Transparent.class, true);
    }

    private static void updateProvider(Context app, AppWidgetManager wm, Class<?> cls, boolean isTransparent) {
        ComponentName provider = new ComponentName(app, cls);
        int[] ids = wm.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return;

        boolean dndOn = false;
        try {
            NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
            dndOn = nm != null && nm.getCurrentInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL;
        } catch (Throwable ignored) {}

        boolean rotateOn = false;
        try {
            rotateOn = Settings.System.getInt(app.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1;
        } catch (Throwable ignored) {}

        boolean darkOn = (app.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        boolean saverOn = false;
        try {
            PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
            saverOn = pm != null && pm.isPowerSaveMode();
        } catch (Throwable ignored) {}

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(app.getPackageName(), R.layout.blueos_toggles_widget);

            applyToggle(rv, R.id.blueos_toggle_flashlight, sTorchOn);
            applyToggle(rv, R.id.blueos_toggle_dnd, dndOn);
            applyToggle(rv, R.id.blueos_toggle_rotate, rotateOn);
            applyToggle(rv, R.id.blueos_toggle_dark, darkOn);
            applyToggle(rv, R.id.blueos_toggle_saver, saverOn);

            rv.setOnClickPendingIntent(R.id.blueos_toggle_flashlight, makeAction(app, ACTION_TOGGLE_TORCH, 21));
            rv.setOnClickPendingIntent(R.id.blueos_toggle_dnd, makeAction(app, ACTION_TOGGLE_DND, 22));
            rv.setOnClickPendingIntent(R.id.blueos_toggle_rotate, makeAction(app, ACTION_TOGGLE_ROTATE, 23));
            rv.setOnClickPendingIntent(R.id.blueos_toggle_dark, makeAction(app, ACTION_TOGGLE_DARK, 24));
            rv.setOnClickPendingIntent(R.id.blueos_toggle_saver, makeAction(app, ACTION_TOGGLE_SAVER, 25));

            WidgetThemeManager.applyBackground(app, rv, isTransparent);
            wm.updateAppWidget(id, rv);
        }
    }

    public static class Transparent extends BlueOSTogglesWidgetProvider {
        @Override
        protected boolean isTransparent() {
            return true;
        }
    }

    private static void applyToggle(RemoteViews rv, int viewId, boolean on) {
        rv.setImageViewResource(viewId, on ? R.drawable.blueos_toggle_on : R.drawable.blueos_toggle_off);
    }

    private static PendingIntent makeAction(Context context, String action, int req) {
        Intent it = new Intent(action).setClass(context, BlueOSTogglesWidgetProvider.class);
        return PendingIntent.getBroadcast(context, req, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
