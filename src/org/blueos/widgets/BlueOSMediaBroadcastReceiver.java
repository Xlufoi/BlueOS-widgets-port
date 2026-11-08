package org.blueos.widgets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class BlueOSMediaBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;

        Bundle extras = intent.getExtras();
        if (extras == null) return;

        String track = extras.getString("track");
        if (track == null) track = extras.getString("song");
        if (track == null) track = extras.getString("title");

        String artist = extras.getString("artist");
        if (artist == null) artist = extras.getString("album");

        boolean playing = false;
        if (extras.containsKey("playing")) {
            playing = extras.getBoolean("playing");
        } else if (extras.containsKey("playstate")) {
            Object ps = extras.get("playstate");
            if (ps instanceof Boolean) playing = (Boolean) ps;
            else if (ps instanceof Number) playing = ((Number) ps).intValue() != 0;
        } else if (extras.containsKey("isplaying")) {
            playing = extras.getBoolean("isplaying");
        }

        if (track != null && !track.isEmpty()) {
            BlueOSMediaWidgetProvider.setNotificationMetadata(track, artist != null ? artist : "", null, intent.getPackage());
        }

        BlueOSMediaWidgetProvider.pushAll(context);
        BlueOSEqualizerWidgetProvider.pushAll(context);
    }
}
