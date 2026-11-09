package org.blueos.widgets;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class RootHelper {
    private static final String TAG = "BlueOSRootHelper";

    private static final String[] SU_PATHS = {
            "su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/adb/ap/bin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/magisk/su"
    };

    public static String findSuBinary() {
        for (String path : SU_PATHS) {
            try {
                Process p = new ProcessBuilder(path, "-v").start();
                boolean exited = p.waitFor(500, TimeUnit.MILLISECONDS);
                if (exited && p.exitValue() == 0) {
                    return path;
                }
            } catch (Throwable ignored) {}
        }
        return "su";
    }

    public static boolean isRootAvailable() {
        String su = findSuBinary();
        try {
            Process p = new ProcessBuilder(su, "-c", "id").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String output = reader.readLine();
            p.waitFor(3, TimeUnit.SECONDS);
            return output != null && output.contains("uid=0");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean grantNotificationAccess(String packageName) {
        String su = findSuBinary();
        String service = packageName + "/" + packageName + ".BlueOSMediaListenerService";
        String[] cmds = {
                "cmd notification allow_listener " + service,
                "settings put secure enabled_notification_listeners " + service,
                "appops set " + packageName + " GET_USAGE_STATS allow",
                "appops set " + packageName + " SYSTEM_ALERT_WINDOW allow",
                "appops set " + packageName + " 10020 allow",
                "appops set " + packageName + " 10021 allow"
        };

        try {
            Process p = new ProcessBuilder(su).start();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String cmd : cmds) {
                os.writeBytes(cmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor(5, TimeUnit.SECONDS);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to grant notification access via root: " + t.getMessage());
            return false;
        }
    }

    public static boolean whitelistBattery(String packageName) {
        String su = findSuBinary();
        String[] cmds = {
                "dumpsys deviceidle whitelist +" + packageName,
                "cmd appops set " + packageName + " RUN_IN_BACKGROUND allow",
                "cmd appops set " + packageName + " RUN_ANY_IN_BACKGROUND allow"
        };

        try {
            Process p = new ProcessBuilder(su).start();
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            for (String cmd : cmds) {
                os.writeBytes(cmd + "\n");
            }
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor(5, TimeUnit.SECONDS);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to whitelist battery via root: " + t.getMessage());
            return false;
        }
    }
}
