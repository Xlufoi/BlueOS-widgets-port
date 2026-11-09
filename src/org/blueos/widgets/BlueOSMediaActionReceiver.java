package org.blueos.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BlueOSMediaActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (BlueOSMediaWidgetProvider.ACTION_PLAY_PAUSE.equals(action)
                || BlueOSMediaWidgetProvider.ACTION_PREVIOUS.equals(action)
                || BlueOSMediaWidgetProvider.ACTION_NEXT.equals(action)) {
            BlueOSMediaWidgetProvider.onAction(context, action);
        }
    }
}
