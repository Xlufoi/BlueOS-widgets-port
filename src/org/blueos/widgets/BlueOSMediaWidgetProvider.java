package org.blueos.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.KeyEvent;
import android.widget.RemoteViews;

import java.util.List;
import java.util.Locale;

public abstract class BlueOSMediaWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_PLAY_PAUSE = "org.blueos.widgets.media.PLAY_PAUSE";
    public static final String ACTION_PREVIOUS = "org.blueos.widgets.media.PREVIOUS";
    public static final String ACTION_NEXT = "org.blueos.widgets.media.NEXT";
    private static final int PROGRESS_MAX = 1000;
    private static final int PROGRESS_TICK_MILLIS = 1000;
    private static final int ART_SIZE_PX = 256;

    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static Context sAppContext = null;
    private static MediaController sController = null;

    // Persistent metadata: survives pauses, transient updates, and screen offs
    private static String sNotifTitle = null;
    private static String sNotifArtist = null;
    private static Bitmap sNotifArt = null;
    private static String sNotifPackage = null;
    private static long sLastPlaybackTime = 0;

    protected abstract int getLayoutId();

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        init(context);
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_PLAY_PAUSE.equals(action)
                    || ACTION_PREVIOUS.equals(action)
                    || ACTION_NEXT.equals(action)) {
                onAction(context, action);
            }
        }
        pushAll(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager wm, int[] widgetIds) {
        init(context);
        pushAll(context);
    }

    public static void setExternalControllers(List<MediaController> controllers) {
        if (controllers == null || controllers.isEmpty()) return;
        MediaController next = null;
        for (MediaController c : controllers) {
            PlaybackState ps = c.getPlaybackState();
            if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                next = c;
                break;
            }
        }
        if (next == null) next = controllers.get(0);
        setExternalController(next);
    }

    public static void setExternalController(MediaController mc) {
        if (mc == null) return;
        if (sController != null && sController.getSessionToken().equals(mc.getSessionToken())) {
            sController = mc;
            return;
        }
        if (sController != null) {
            try { sController.unregisterCallback(sControllerCallback); } catch (Throwable ignored) {}
        }
        sController = mc;
        try { mc.registerCallback(sControllerCallback, sHandler); } catch (Throwable ignored) {}
    }

    public static void setNotificationMetadata(String title, String artist, Bitmap art, String pkg) {
        if (title != null && !title.isEmpty()) {
            sNotifTitle = title;
        }
        if (artist != null && !artist.isEmpty()) {
            sNotifArtist = artist;
        }
        if (art != null) {
            sNotifArt = art;
        }
        if (pkg != null && !pkg.isEmpty()) {
            sNotifPackage = pkg;
        }
        sLastPlaybackTime = System.currentTimeMillis();
    }

    public static String getTrackTitle() {
        if (sController != null) {
            MediaMetadata md = sController.getMetadata();
            if (md != null) {
                String t = md.getString(MediaMetadata.METADATA_KEY_TITLE);
                if (t != null && !t.isEmpty()) return t;
                if (md.getDescription() != null && md.getDescription().getTitle() != null) {
                    return md.getDescription().getTitle().toString();
                }
            }
        }
        return sNotifTitle;
    }

    public static String getTrackArtist() {
        if (sController != null) {
            MediaMetadata md = sController.getMetadata();
            if (md != null) {
                String a = md.getString(MediaMetadata.METADATA_KEY_ARTIST);
                if (a == null) a = md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
                if (a == null && md.getDescription() != null && md.getDescription().getSubtitle() != null) {
                    a = md.getDescription().getSubtitle().toString();
                }
                if (a != null && !a.isEmpty()) return a;
            }
        }
        return sNotifArtist;
    }

    public static Bitmap getTrackArt() {
        if (sController != null) {
            MediaMetadata md = sController.getMetadata();
            if (md != null) {
                Bitmap b = albumArt(md);
                if (b != null) return b;
            }
        }
        return sNotifArt;
    }

    public static boolean isPlaying(Context context) {
        if (sController != null) {
            PlaybackState ps = sController.getPlaybackState();
            if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) return true;
        }
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am != null && am.isMusicActive()) return true;
        } catch (Throwable ignored) {}
        return false;
    }

    private static final MediaController.Callback sControllerCallback = new MediaController.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackState state) {
            if (sAppContext != null) {
                pushAll(sAppContext);
                BlueOSEqualizerWidgetProvider.pushAll(sAppContext);
            }
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            if (sAppContext != null) {
                pushAll(sAppContext);
                BlueOSEqualizerWidgetProvider.pushAll(sAppContext);
            }
        }

        @Override
        public void onSessionDestroyed() {
            // Keep existing metadata; don't wipe immediately
        }
    };

    private static final Runnable sProgressTick = new Runnable() {
        @Override
        public void run() {
            if (sAppContext != null) {
                pushAll(sAppContext);
            }
        }
    };

    public static void init(Context context) {
        if (sAppContext != null) return;
        sAppContext = context.getApplicationContext();

        try {
            sAppContext.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent it) {
                    pushAll(ctx);
                    BlueOSEqualizerWidgetProvider.pushAll(ctx);
                }
            }, new IntentFilter(Intent.ACTION_SCREEN_ON));
        } catch (Throwable ignored) {}
    }

    public static void onAction(Context context, String action) {
        if (action == null) return;
        if (!ACTION_PLAY_PAUSE.equals(action)
                && !ACTION_PREVIOUS.equals(action)
                && !ACTION_NEXT.equals(action)) {
            return;
        }

        init(context);
        boolean handled = false;

        if (sController != null) {
            try {
                MediaController.TransportControls controls = sController.getTransportControls();
                if (controls != null) {
                    if (ACTION_PLAY_PAUSE.equals(action)) {
                        PlaybackState ps = sController.getPlaybackState();
                        if (ps != null && ps.getState() == PlaybackState.STATE_PLAYING) {
                            controls.pause();
                        } else {
                            controls.play();
                        }
                        handled = true;
                    } else if (ACTION_PREVIOUS.equals(action)) {
                        controls.skipToPrevious();
                        handled = true;
                    } else if (ACTION_NEXT.equals(action)) {
                        controls.skipToNext();
                        handled = true;
                    }
                }
            } catch (Throwable ignored) {}
        }

        // Only dispatch hardware media keys if an explicit user button was pressed AND sController did not handle it
        if (!handled) {
            try {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    int keyCode = 0;
                    if (ACTION_PLAY_PAUSE.equals(action)) {
                        keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;
                    } else if (ACTION_PREVIOUS.equals(action)) {
                        keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
                    } else if (ACTION_NEXT.equals(action)) {
                        keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
                    }

                    if (keyCode != 0) {
                        long now = SystemClock.uptimeMillis();
                        am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0));
                        am.dispatchMediaKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0));
                    }
                }
            } catch (Throwable ignored) {}
        }

        pushAll(context);
        BlueOSEqualizerWidgetProvider.pushAll(context);
        sHandler.postDelayed(() -> {
            pushAll(context);
            BlueOSEqualizerWidgetProvider.pushAll(context);
        }, 350);
    }

    public static void pushAll(Context context) {
        sHandler.removeCallbacks(sProgressTick);
        Context app = context.getApplicationContext();
        AppWidgetManager wm = AppWidgetManager.getInstance(app);

        // Filled card variants
        updateVariant(app, wm, BlueOSMediaWidgetProvider.Small.class, R.layout.blueos_media_widget_small, false);
        updateVariant(app, wm, BlueOSMediaWidgetProvider.Square.class, R.layout.blueos_media_widget_square, false);
        updateVariant(app, wm, BlueOSMediaWidgetProvider.Wide.class, R.layout.blueos_media_widget_wide, false);
        updateVariant(app, wm, BlueOSMediaWidgetProvider.Large.class, R.layout.blueos_media_widget_large, false);

        // Minimalist outline variants
        updateVariant(app, wm, BlueOSMediaWidgetProvider.SmallOutline.class, R.layout.blueos_media_widget_small_outline, true);
        updateVariant(app, wm, BlueOSMediaWidgetProvider.SquareOutline.class, R.layout.blueos_media_widget_square_outline, true);
        updateVariant(app, wm, BlueOSMediaWidgetProvider.WideOutline.class, R.layout.blueos_media_widget_wide_outline, true);
        updateVariant(app, wm, BlueOSMediaWidgetProvider.LargeOutline.class, R.layout.blueos_media_widget_large_outline, true);
    }

    private static void updateVariant(Context app, AppWidgetManager wm, Class<?> cls, int layoutId, boolean isOutline) {
        ComponentName provider = new ComponentName(app, cls);
        int[] ids = wm.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return;

        MediaController current = sController;
        MediaMetadata metadata = current != null ? current.getMetadata() : null;
        PlaybackState state = current != null ? current.getPlaybackState() : null;
        boolean playing = isPlaying(app);

        String title = getTrackTitle();
        String artist = getTrackArtist();
        Bitmap art = getTrackArt();

        long duration = 0;
        long position = 0;
        if (metadata != null) {
            duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        }
        if (state != null) {
            position = currentPosition(state);
        }

        int progress = (duration > 0) ? (int) Math.min(PROGRESS_MAX, (position * PROGRESS_MAX) / duration) : 0;

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(app.getPackageName(), layoutId);

            if (title == null || title.isEmpty()) {
                rv.setTextViewText(R.id.blueos_media_title, app.getString(R.string.blueos_media_widget_idle));
                rv.setTextViewText(R.id.blueos_media_artist, "Schale Music");
            } else {
                rv.setTextViewText(R.id.blueos_media_title, title);
                rv.setTextViewText(R.id.blueos_media_artist, artist != null ? artist : "");
            }

            String elapsedStr = formatTime(position);
            String durationStr = (duration > 0) ? formatTime(duration) : "--:--";
            String timeCombined = elapsedStr + " / " + durationStr;

            try {
                rv.setTextViewText(R.id.blueos_media_elapsed, elapsedStr);
                rv.setTextViewText(R.id.blueos_media_duration, durationStr);
                rv.setTextViewText(R.id.blueos_media_time, timeCombined);
            } catch (Throwable ignored) {}

            if (art != null) {
                float radiusPx = 28f * app.getResources().getDisplayMetrics().density;
                Bitmap roundedArt = getRoundedCornerBitmap(art, radiusPx);
                rv.setImageViewBitmap(R.id.blueos_media_art, roundedArt);
                rv.setViewVisibility(R.id.blueos_media_art, android.view.View.VISIBLE);
                try { rv.setViewVisibility(R.id.blueos_media_placeholder, android.view.View.GONE); } catch (Throwable ignored) {}
                try { rv.setViewVisibility(R.id.blueos_media_scrim, android.view.View.VISIBLE); } catch (Throwable ignored) {}
            } else {
                rv.setViewVisibility(R.id.blueos_media_art, android.view.View.GONE);
                try { rv.setViewVisibility(R.id.blueos_media_placeholder, android.view.View.VISIBLE); } catch (Throwable ignored) {}
                try { rv.setViewVisibility(R.id.blueos_media_scrim, android.view.View.GONE); } catch (Throwable ignored) {}
            }

            try {
                rv.setProgressBar(R.id.blueos_media_progress, PROGRESS_MAX, progress, false);
            } catch (Throwable ignored) {}

            int playIcon = playing ? R.drawable.blueos_ic_pause : R.drawable.blueos_ic_play;
            rv.setImageViewResource(R.id.blueos_media_play_pause, playIcon);

            rv.setOnClickPendingIntent(R.id.blueos_media_play_pause, makeAction(app, ACTION_PLAY_PAUSE, id * 10 + 1));
            rv.setOnClickPendingIntent(R.id.blueos_media_previous, makeAction(app, ACTION_PREVIOUS, id * 10 + 2));
            rv.setOnClickPendingIntent(R.id.blueos_media_next, makeAction(app, ACTION_NEXT, id * 10 + 3));

            // Background tap: open music app or notification settings
            PendingIntent clickIntent = null;
            if (current != null && current.getSessionActivity() != null) {
                clickIntent = current.getSessionActivity();
            } else if (sNotifPackage != null) {
                Intent launch = app.getPackageManager().getLaunchIntentForPackage(sNotifPackage);
                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    clickIntent = PendingIntent.getActivity(app, id * 10 + 4, launch, PendingIntent.FLAG_IMMUTABLE);
                }
            }

            if (clickIntent == null) {
                Intent mainAct = new Intent(app, MainActivity.class);
                mainAct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                clickIntent = PendingIntent.getActivity(app, id * 10 + 5, mainAct, PendingIntent.FLAG_IMMUTABLE);
            }
            rv.setOnClickPendingIntent(android.R.id.background, clickIntent);

            WidgetThemeManager.applyBackground(app, rv, isOutline);
            wm.updateAppWidget(id, rv);
        }

        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        boolean interactive = pm != null && pm.isInteractive();
        if (playing && duration > 0 && interactive) {
            sHandler.postDelayed(sProgressTick, PROGRESS_TICK_MILLIS);
        }
    }

    private static long currentPosition(PlaybackState state) {
        if (state.getState() != PlaybackState.STATE_PLAYING) return state.getPosition();
        long elapsed = SystemClock.elapsedRealtime() - state.getLastPositionUpdateTime();
        return state.getPosition() + (long) (elapsed * state.getPlaybackSpeed());
    }

    private static Bitmap albumArt(MediaMetadata metadata) {
        Bitmap b = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (b == null) b = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (b == null) b = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (b == null) return null;
        if (b.getWidth() <= ART_SIZE_PX && b.getHeight() <= ART_SIZE_PX) return b;
        float scale = (float) ART_SIZE_PX / Math.max(b.getWidth(), b.getHeight());
        return Bitmap.createScaledBitmap(b, Math.max(1, (int) (b.getWidth() * scale)),
                Math.max(1, (int) (b.getHeight() * scale)), true);
    }

    private static String formatTime(long ms) {
        if (ms <= 0) return "0:00";
        long totalSec = ms / 1000;
        long minutes = totalSec / 60;
        long seconds = totalSec % 60;
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds);
    }

    private static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float cornerRadiusPx) {
        if (bitmap == null) return null;
        try {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            Bitmap output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            BitmapShader shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            paint.setShader(shader);
            RectF rect = new RectF(0, 0, w, h);
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint);
            return output;
        } catch (Throwable t) {
            return bitmap;
        }
    }

    public static PendingIntent makeAction(Context context, String action, int requestCode) {
        Intent it = new Intent(context, BlueOSMediaActionReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(context, requestCode, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static class Small extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_small;
        }
    }

    public static class SmallOutline extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_small_outline;
        }
    }

    public static class Square extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_square;
        }
    }

    public static class SquareOutline extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_square_outline;
        }
    }

    public static class Wide extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_wide;
        }
    }

    public static class WideOutline extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_wide_outline;
        }
    }

    public static class Large extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_large;
        }
    }

    public static class LargeOutline extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_large_outline;
        }
    }

    public static class Vinyl extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_vinyl;
        }
    }

    public static class Cassette extends BlueOSMediaWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_media_widget_cassette;
        }
    }
}
