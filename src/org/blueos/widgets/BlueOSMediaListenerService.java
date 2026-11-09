package org.blueos.widgets;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.List;

public class BlueOSMediaListenerService extends NotificationListenerService {

    private static BlueOSMediaListenerService sInstance = null;
    private MediaSessionManager mMediaSessionManager = null;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final MediaSessionManager.OnActiveSessionsChangedListener mSessionsListener =
            new MediaSessionManager.OnActiveSessionsChangedListener() {
                @Override
                public void onActiveSessionsChanged(List<MediaController> controllers) {
                    handleControllers(controllers);
                }
            };

    public static BlueOSMediaListenerService getInstance() {
        return sInstance;
    }

    public static boolean isConnected() {
        return sInstance != null;
    }

    private final android.content.BroadcastReceiver mTickReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BlueOSClockWidgetProvider.updateMoeClocks(context);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            registerReceiver(mTickReceiver, filter);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mTickReceiver);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        sInstance = this;
        try {
            mMediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (mMediaSessionManager != null) {
                ComponentName cn = new ComponentName(this, getClass());
                mMediaSessionManager.addOnActiveSessionsChangedListener(mSessionsListener, cn, mHandler);
                handleControllers(mMediaSessionManager.getActiveSessions(cn));
            }
        } catch (Throwable ignored) {}

        // Scan existing active notifications for media
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification sbn : active) {
                    checkMediaNotification(sbn);
                }
            }
        } catch (Throwable ignored) {}

        updateSessions();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        if (mMediaSessionManager != null) {
            try {
                mMediaSessionManager.removeOnActiveSessionsChangedListener(mSessionsListener);
            } catch (Throwable ignored) {}
        }
        if (sInstance == this) sInstance = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        checkMediaNotification(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        updateSessions();
    }

    private void handleControllers(List<MediaController> controllers) {
        if (controllers != null && !controllers.isEmpty()) {
            BlueOSMediaWidgetProvider.setExternalControllers(controllers);
            for (MediaController mc : controllers) {
                try {
                    mc.registerCallback(new MediaController.Callback() {
                        @Override
                        public void onPlaybackStateChanged(PlaybackState state) {
                            BlueOSMediaWidgetProvider.pushAll(BlueOSMediaListenerService.this);
                            BlueOSEqualizerWidgetProvider.pushAll(BlueOSMediaListenerService.this);
                        }

                        @Override
                        public void onMetadataChanged(MediaMetadata metadata) {
                            BlueOSMediaWidgetProvider.pushAll(BlueOSMediaListenerService.this);
                            BlueOSEqualizerWidgetProvider.pushAll(BlueOSMediaListenerService.this);
                        }
                    }, mHandler);
                } catch (Throwable ignored) {}
            }
        }
        BlueOSMediaWidgetProvider.pushAll(this);
        BlueOSEqualizerWidgetProvider.pushAll(this);
    }

    public void updateSessions() {
        try {
            if (mMediaSessionManager != null) {
                ComponentName cn = new ComponentName(this, getClass());
                handleControllers(mMediaSessionManager.getActiveSessions(cn));
                return;
            }
        } catch (Throwable ignored) {}
        BlueOSMediaWidgetProvider.pushAll(this);
        BlueOSEqualizerWidgetProvider.pushAll(this);
    }

    private void checkMediaNotification(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;
        Notification n = sbn.getNotification();
        Bundle extras = n.extras;
        if (extras == null) return;

        String pkg = sbn.getPackageName();

        // 1. Extract MediaSession Token if available
        try {
            MediaSession.Token token = null;
            if (Build.VERSION.SDK_INT >= 33) {
                token = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token.class);
            } else {
                token = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
            }
            if (token != null) {
                MediaController mc = new MediaController(this, token);
                BlueOSMediaWidgetProvider.setExternalController(mc);
            }
        } catch (Throwable ignored) {}

        // 2. ALWAYS extract notification title, artist, and large icon
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence artist = extras.getCharSequence(Notification.EXTRA_TEXT);
        if (artist == null) artist = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        if (title != null && title.length() > 0) {
            Bitmap largeIcon = null;
            try {
                Icon icon = n.getLargeIcon();
                if (icon != null) {
                    Drawable d = icon.loadDrawable(this);
                    if (d instanceof BitmapDrawable) {
                        largeIcon = ((BitmapDrawable) d).getBitmap();
                    }
                }
            } catch (Throwable ignored) {}

            boolean isMedia = extras.containsKey(Notification.EXTRA_MEDIA_SESSION)
                    || Notification.CATEGORY_TRANSPORT.equals(n.category)
                    || pkg.toLowerCase().contains("music")
                    || pkg.toLowerCase().contains("audio")
                    || pkg.toLowerCase().contains("spotify")
                    || pkg.toLowerCase().contains("yandex")
                    || pkg.toLowerCase().contains("youtube");

            if (!isMedia && n.actions != null) {
                for (Notification.Action a : n.actions) {
                    if (a.title != null) {
                        String at = a.title.toString().toLowerCase();
                        if (at.contains("play") || at.contains("pause") || at.contains("next")
                                || at.contains("воспр") || at.contains("пауз") || at.contains("след")) {
                            isMedia = true;
                            break;
                        }
                    }
                }
            }

            if (isMedia) {
                BlueOSMediaWidgetProvider.setNotificationMetadata(
                        title.toString(),
                        artist != null ? artist.toString() : "",
                        largeIcon,
                        pkg
                );
            }
        }

        BlueOSMediaWidgetProvider.pushAll(this);
        BlueOSEqualizerWidgetProvider.pushAll(this);
    }
}
