package org.blueos.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.view.View;
import android.widget.RemoteViews;

import java.util.Random;

public abstract class BlueOSEqualizerWidgetProvider extends AppWidgetProvider {

    protected abstract int getLayoutId();
    protected abstract int getStyle();

    public static final int STYLE_BARS = 0;
    public static final int STYLE_RING = 1;
    public static final int STYLE_WAVE = 2;
    public static final int STYLE_FULL = 3;

    private static final Handler sHandler = new Handler(Looper.getMainLooper());
    private static Context sAppContext = null;
    private static boolean sIsAnimating = false;
    private static int sAnimStep = 0;
    private static float sAmp = 0f;
    private static final int FRAME_DELAY_MS = 33; // ~30 FPS

    private static Bitmap sBitmapBars = null;
    private static Canvas sCanvasBars = null;
    private static Bitmap sBitmapSquare = null;
    private static Canvas sCanvasSquare = null;
    private static final Paint sPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Path sPath1 = new Path();
    private static final Path sPath2 = new Path();

    private static final Runnable sAnimTick = new Runnable() {
        @Override
        public void run() {
            if (sAppContext == null) {
                sIsAnimating = false;
                return;
            }
            PowerManager pm = (PowerManager) sAppContext.getSystemService(Context.POWER_SERVICE);
            boolean interactive = pm == null || pm.isInteractive();
            boolean playing = BlueOSMediaWidgetProvider.isPlaying(sAppContext);

            if (playing && interactive) {
                sAnimStep++;
                sAmp = Math.min(1.0f, sAmp + (1.0f - sAmp) * 0.14f + 0.03f); // Butter-smooth attack ease-in
                boolean anyActive = pushPartial(sAppContext, sAmp);
                if (sAnimStep % 4 == 0) {
                    BlueOSAronaPetWidgetProvider.tickMusic(sAppContext, sAnimStep);
                    BlueOSDuoPetWidgetProvider.tickMusic(sAppContext, sAnimStep);
                }
                if (anyActive) {
                    sHandler.postDelayed(this, FRAME_DELAY_MS);
                } else {
                    sIsAnimating = false;
                }
            } else if (sAmp > 0.02f && interactive) {
                // Smooth organic decay / fade-out
                sAnimStep++;
                sAmp *= 0.85f;
                if (sAmp < 0.02f) sAmp = 0f;
                boolean anyActive = pushPartial(sAppContext, sAmp);
                if (sAnimStep % 4 == 0) {
                    BlueOSAronaPetWidgetProvider.tickMusic(sAppContext, 0);
                }
                if (anyActive && sAmp > 0f) {
                    sHandler.postDelayed(this, FRAME_DELAY_MS);
                } else {
                    sIsAnimating = false;
                    pushPartial(sAppContext, 0f);
                }
            } else {
                sAmp = 0f;
                sIsAnimating = false;
                pushPartial(sAppContext, 0f);
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        init(context);
        pushAll(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        init(context);
        pushAll(context);
    }

    private static void init(Context context) {
        if (sAppContext == null) {
            sAppContext = context.getApplicationContext();
        }
        BlueOSMediaWidgetProvider.init(context);
    }

    public static boolean pushAll(Context context) {
        boolean playing = BlueOSMediaWidgetProvider.isPlaying(context);
        return pushAll(context, playing ? (sAmp > 0.1f ? sAmp : 1.0f) : sAmp);
    }

    public static boolean pushAll(Context context, float amp) {
        init(context);
        Context app = context.getApplicationContext();
        AppWidgetManager wm = AppWidgetManager.getInstance(app);

        boolean playing = BlueOSMediaWidgetProvider.isPlaying(app);

        // Transparent variants
        boolean b1 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.Bars.class, R.layout.blueos_eq_widget_bars, STYLE_BARS, playing, amp, true);
        boolean b2 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.Ring.class, R.layout.blueos_eq_widget_ring, STYLE_RING, playing, amp, true);
        boolean b3 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.Wave.class, R.layout.blueos_eq_widget_wave, STYLE_WAVE, playing, amp, true);
        boolean b4 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.FullTransparent.class, R.layout.blueos_eq_widget_full_trans, STYLE_FULL, playing, amp, true);

        // Card variants
        boolean c1 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.BarsCard.class, R.layout.blueos_eq_widget_bars_card, STYLE_BARS, playing, amp, false);
        boolean c2 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.RingCard.class, R.layout.blueos_eq_widget_ring_card, STYLE_RING, playing, amp, false);
        boolean c3 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.WaveCard.class, R.layout.blueos_eq_widget_wave_card, STYLE_WAVE, playing, amp, false);
        boolean c4 = updateStyle(app, wm, BlueOSEqualizerWidgetProvider.Full.class, R.layout.blueos_eq_widget_full, STYLE_FULL, playing, amp, false);

        boolean hasAnyWidgets = b1 || b2 || b3 || b4 || c1 || c2 || c3 || c4;

        if ((playing || amp > 0f) && hasAnyWidgets && !sIsAnimating) {
            sIsAnimating = true;
            sHandler.post(sAnimTick);
        }

        return hasAnyWidgets;
    }

    public static boolean pushPartial(Context context, float amp) {
        init(context);
        Context app = context.getApplicationContext();
        AppWidgetManager wm = AppWidgetManager.getInstance(app);

        boolean playing = BlueOSMediaWidgetProvider.isPlaying(app);

        boolean b1 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.Bars.class, R.layout.blueos_eq_widget_bars, STYLE_BARS, playing, amp);
        boolean b2 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.Ring.class, R.layout.blueos_eq_widget_ring, STYLE_RING, playing, amp);
        boolean b3 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.Wave.class, R.layout.blueos_eq_widget_wave, STYLE_WAVE, playing, amp);
        boolean b4 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.FullTransparent.class, R.layout.blueos_eq_widget_full_trans, STYLE_FULL, playing, amp);

        boolean c1 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.BarsCard.class, R.layout.blueos_eq_widget_bars_card, STYLE_BARS, playing, amp);
        boolean c2 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.RingCard.class, R.layout.blueos_eq_widget_ring_card, STYLE_RING, playing, amp);
        boolean c3 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.WaveCard.class, R.layout.blueos_eq_widget_wave_card, STYLE_WAVE, playing, amp);
        boolean c4 = updatePartial(app, wm, BlueOSEqualizerWidgetProvider.Full.class, R.layout.blueos_eq_widget_full, STYLE_FULL, playing, amp);

        return b1 || b2 || b3 || b4 || c1 || c2 || c3 || c4;
    }

    private static boolean updatePartial(Context app, AppWidgetManager wm, Class<?> cls, int layoutId, int style, boolean playing, float amp) {
        ComponentName provider = new ComponentName(app, cls);
        int[] ids = wm.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return false;

        Bitmap eqBitmap = drawVisualizer(app, style, playing, sAnimStep, amp);
        if (eqBitmap == null) return false;

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(app.getPackageName(), layoutId);
            rv.setImageViewBitmap(R.id.blueos_eq_view, eqBitmap);
            wm.partiallyUpdateAppWidget(id, rv);
        }
        return true;
    }

    private static boolean updateStyle(Context app, AppWidgetManager wm, Class<?> cls, int layoutId, int style, boolean playing, float amp, boolean isTransparent) {
        ComponentName provider = new ComponentName(app, cls);
        int[] ids = wm.getAppWidgetIds(provider);
        if (ids == null || ids.length == 0) return false;

        String title = BlueOSMediaWidgetProvider.getTrackTitle();
        String artist = BlueOSMediaWidgetProvider.getTrackArtist();
        Bitmap art = BlueOSMediaWidgetProvider.getTrackArt();

        Bitmap eqBitmap = drawVisualizer(app, style, playing, sAnimStep, amp);

        for (int id : ids) {
            RemoteViews rv = new RemoteViews(app.getPackageName(), layoutId);

            if (eqBitmap != null) {
                rv.setImageViewBitmap(R.id.blueos_eq_view, eqBitmap);
            }

            try {
                if (title != null && !title.isEmpty()) {
                    rv.setTextViewText(R.id.blueos_media_title, title);
                } else {
                    rv.setTextViewText(R.id.blueos_media_title, app.getString(R.string.blueos_media_widget_idle));
                }
            } catch (Throwable ignored) {}

            try {
                if (artist != null && !artist.isEmpty()) {
                    rv.setTextViewText(R.id.blueos_media_artist, artist);
                } else {
                    rv.setTextViewText(R.id.blueos_media_artist, "Schale Sound");
                }
            } catch (Throwable ignored) {}

            try {
                if (art != null) {
                    rv.setImageViewBitmap(R.id.blueos_media_art, art);
                    rv.setViewVisibility(R.id.blueos_media_art, View.VISIBLE);
                    rv.setViewVisibility(R.id.blueos_media_placeholder, View.GONE);
                } else {
                    rv.setViewVisibility(R.id.blueos_media_art, View.GONE);
                    rv.setViewVisibility(R.id.blueos_media_placeholder, View.VISIBLE);
                }
            } catch (Throwable ignored) {}

            try {
                int playIcon = playing ? R.drawable.blueos_ic_pause : R.drawable.blueos_ic_play;
                rv.setImageViewResource(R.id.blueos_media_play_pause, playIcon);
                rv.setOnClickPendingIntent(R.id.blueos_media_play_pause,
                        BlueOSMediaWidgetProvider.makeAction(app, BlueOSMediaWidgetProvider.ACTION_PLAY_PAUSE, id * 10 + 6));
            } catch (Throwable ignored) {}

            if (style != STYLE_FULL) {
                rv.setOnClickPendingIntent(android.R.id.background,
                        BlueOSMediaWidgetProvider.makeAction(app, BlueOSMediaWidgetProvider.ACTION_PLAY_PAUSE, id * 10 + 7));
            }

            WidgetThemeManager.applyBackground(app, rv, isTransparent);
            wm.updateAppWidget(id, rv);
        }

        return true;
    }

    private static Bitmap drawVisualizer(Context context, int style, boolean playing, int step, float amp) {
        int width = 160;
        int height = (style == STYLE_BARS || style == STYLE_WAVE) ? 70 : 100;
        Bitmap b;
        Canvas c;
        if (height == 70) {
            if (sBitmapBars == null) {
                sBitmapBars = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                sCanvasBars = new Canvas(sBitmapBars);
            }
            b = sBitmapBars;
            c = sCanvasBars;
        } else {
            if (sBitmapSquare == null) {
                sBitmapSquare = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                sCanvasSquare = new Canvas(sBitmapSquare);
            }
            b = sBitmapSquare;
            c = sCanvasSquare;
        }
        c.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);

        Paint paint = sPaint;
        paint.reset();
        paint.setAntiAlias(true);
        int primary = 0xFF53B2FF; // Kivotos Blue
        int accent = 0xFF29D8FF;  // Halo Cyan

        if (style == STYLE_BARS || style == STYLE_FULL) {
            paint.setStyle(Paint.Style.FILL);
            int barCount = 16;
            float barW = 5f;
            float gap = 4f;
            float totalW = barCount * barW + (barCount - 1) * gap;
            float startX = (width - totalW) / 2f;
            float t = step * 0.16f;

            for (int i = 0; i < barCount; i++) {
                float h;
                if (amp > 0f) {
                    float bass = (i < 6) ? (float) Math.pow(Math.max(0, Math.sin(t * 1.5f)), 2.0) * (1.0f - i * 0.14f) * 0.7f : 0f;
                    float wave = (float) (0.45f + 0.35f * Math.sin(t * 1.0f + i * 0.38f) + 0.15f * Math.sin(t * 2.1f - i * 0.28f));
                    float norm = Math.min(1.0f, Math.max(0.06f, wave * 0.6f + bass)) * amp;
                    h = 5f + norm * (height - 9f);
                } else {
                    h = 5f;
                }

                float x = startX + i * (barW + gap);
                float y = height - h - 3f;

                paint.setShader(new LinearGradient(x, y, x, height - 3f, accent, primary, Shader.TileMode.CLAMP));
                c.drawRoundRect(x, y, x + barW, height - 3f, barW / 2f, barW / 2f, paint);
            }
        } else if (style == STYLE_WAVE) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            float t = step * 0.16f;

            paint.setStrokeWidth(3.5f);
            paint.setColor(primary);
            Path path1 = sPath1;
            path1.reset();
            path1.moveTo(4f, height / 2f);
            float stepW = (width - 8f) / 16f;
            for (int i = 0; i <= 16; i++) {
                float x = 4f + i * stepW;
                float waveAmp = amp > 0f ? (float) (Math.sin(t * 1.2f + i * 0.42f) * (height / 2.7f)) * amp : 0f;
                path1.lineTo(x, height / 2f + waveAmp);
            }
            c.drawPath(path1, paint);

            paint.setStrokeWidth(2f);
            paint.setColor(accent);
            Path path2 = sPath2;
            path2.reset();
            path2.moveTo(4f, height / 2f);
            for (int i = 0; i <= 16; i++) {
                float x = 4f + i * stepW;
                float waveAmp = amp > 0f ? (float) (Math.cos(t * 1.0f + i * 0.36f + 1.2f) * (height / 3.2f)) * amp : 0f;
                path2.lineTo(x, height / 2f + waveAmp);
            }
            c.drawPath(path2, paint);

        } else if (style == STYLE_RING) {
            float cx = width / 2f;
            float cy = height / 2f;
            float radius = 26f;

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f);
            paint.setColor(primary);
            c.drawCircle(cx, cy, radius, paint);

            paint.setStrokeWidth(1.5f);
            paint.setColor(accent);
            c.drawCircle(cx, cy, radius - 4f, paint);

            paint.setStrokeWidth(2.2f);
            paint.setStrokeCap(Paint.Cap.ROUND);
            int spokes = 20;
            float t = step * 0.14f;
            for (int i = 0; i < spokes; i++) {
                double angle = (2 * Math.PI / spokes) * i + (step * 0.02);
                float pulse = 0.15f + (amp > 0f ? (float) (0.42f * Math.sin(t * 1.6f + i * 0.5f) + 0.15f * Math.cos(t * 0.8f)) * amp : 0f);
                float spokeLen = 2.5f + Math.max(0f, pulse) * 16f;
                float x1 = (float) (cx + Math.cos(angle) * (radius + 2f));
                float y1 = (float) (cy + Math.sin(angle) * (radius + 2f));
                float x2 = (float) (cx + Math.cos(angle) * (radius + 2f + spokeLen));
                float y2 = (float) (cy + Math.sin(angle) * (radius + 2f + spokeLen));

                paint.setColor(i % 2 == 0 ? primary : accent);
                c.drawLine(x1, y1, x2, y2, paint);
            }
        }

        return b;
    }

    public static class Bars extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_bars;
        }

        @Override
        protected int getStyle() {
            return STYLE_BARS;
        }
    }

    public static class BarsCard extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_bars_card;
        }

        @Override
        protected int getStyle() {
            return STYLE_BARS;
        }
    }

    public static class Ring extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_ring;
        }

        @Override
        protected int getStyle() {
            return STYLE_RING;
        }
    }

    public static class RingCard extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_ring_card;
        }

        @Override
        protected int getStyle() {
            return STYLE_RING;
        }
    }

    public static class Wave extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_wave;
        }

        @Override
        protected int getStyle() {
            return STYLE_WAVE;
        }
    }

    public static class WaveCard extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_wave_card;
        }

        @Override
        protected int getStyle() {
            return STYLE_WAVE;
        }
    }

    public static class Full extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_full;
        }

        @Override
        protected int getStyle() {
            return STYLE_FULL;
        }
    }

    public static class FullTransparent extends BlueOSEqualizerWidgetProvider {
        @Override
        protected int getLayoutId() {
            return R.layout.blueos_eq_widget_full_trans;
        }

        @Override
        protected int getStyle() {
            return STYLE_FULL;
        }
    }
}
