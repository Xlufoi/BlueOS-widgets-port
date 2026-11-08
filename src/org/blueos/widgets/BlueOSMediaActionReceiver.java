package org.blueos.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BlueOSMediaActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action != null) {
            BlueOSMediaWidgetProvider.onAction(context, action);
        }
    }
}
