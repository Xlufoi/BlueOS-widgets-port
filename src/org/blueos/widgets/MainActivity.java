package org.blueos.widgets;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {

    private TextView mStatusNotif;
    private TextView mStatusPlayer;
    private Button mBtnNotif;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BlueOSMediaWidgetProvider.init(this);

        float dp = getResources().getDisplayMetrics().density;

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF090E17);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (22 * dp), (int) (32 * dp), (int) (22 * dp), (int) (32 * dp));
        scroll.addView(root);
        setContentView(scroll);

        // Header
        TextView header = new TextView(this);
        header.setText("✦ KIVOTOS BLUEOS ✦");
        header.setTextSize(12f);
        header.setLetterSpacing(0.15f);
        header.setTextColor(0xFF00E5FF);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(header);

        TextView title = new TextView(this);
        title.setText("Виджеты BlueOS");
        title.setTextSize(24f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, (int) (4 * dp), 0, (int) (2 * dp));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Настройка служб, темы и доступа для HyperOS");
        subtitle.setTextSize(13f);
        subtitle.setTextColor(0xFF94A3B8);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, (int) (24 * dp));
        root.addView(subtitle);

        // Card 0: Widget Theme & Style
        LinearLayout cardTheme = createCard(dp);
        TextView cardThemeTitle = cardHeader("ТЕМА И ОФОРМЛЕНИЕ ВИДЖЕТОВ", dp);
        cardTheme.addView(cardThemeTitle);

        TextView descTheme = new TextView(this);
        descTheme.setText("Выберите стиль фона и прозрачность для всех виджетов:");
        descTheme.setTextSize(12f);
        descTheme.setTextColor(0xFFCBD5E1);
        descTheme.setPadding(0, (int) (4 * dp), 0, (int) (10 * dp));
        cardTheme.addView(descTheme);

        LinearLayout rowThemes = new LinearLayout(this);
        rowThemes.setOrientation(LinearLayout.HORIZONTAL);
        rowThemes.setPadding(0, 0, 0, (int) (12 * dp));

        Button btnBlack = new Button(this);
        Button btnAosp = new Button(this);
        Button btnMonet = new Button(this);
        Button btnTrans = new Button(this);

        final Runnable updateThemeButtons = () -> {
            int cur = WidgetThemeManager.getTheme(this);
            boolean trans = WidgetThemeManager.isTranslucent(this);

            styleSegment(btnBlack, "Pure Black", cur == WidgetThemeManager.THEME_PURE_BLACK, dp);
            styleSegment(btnAosp, "AOSP Dark", cur == WidgetThemeManager.THEME_AOSP_DARK, dp);
            styleSegment(btnMonet, "Monet (Обои)", cur == WidgetThemeManager.THEME_MONET, dp);

            btnTrans.setText(trans ? "✦ Эффект стекла: ВКЛЮЧЕН (70%)" : "Эффект стекла: ВЫКЛЮЧЕН (Сплошной)");
            styleTranslucentBtn(btnTrans, trans, dp);
        };

        btnBlack.setOnClickListener(v -> {
            WidgetThemeManager.setTheme(this, WidgetThemeManager.THEME_PURE_BLACK);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, "Тема: Pure Black (Черный AMOLED)", Toast.LENGTH_SHORT).show();
        });
        btnAosp.setOnClickListener(v -> {
            WidgetThemeManager.setTheme(this, WidgetThemeManager.THEME_AOSP_DARK);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, "Тема: AOSP Dark (Тёмно-серый)", Toast.LENGTH_SHORT).show();
        });
        btnMonet.setOnClickListener(v -> {
            WidgetThemeManager.setTheme(this, WidgetThemeManager.THEME_MONET);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, "Тема: Monet (Цвета текущих обоев)", Toast.LENGTH_SHORT).show();
        });

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        btnLp.setMarginEnd((int) (6 * dp));
        rowThemes.addView(btnBlack, btnLp);

        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        btnLp2.setMarginEnd((int) (6 * dp));
        rowThemes.addView(btnAosp, btnLp2);

        LinearLayout.LayoutParams btnLp3 = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        rowThemes.addView(btnMonet, btnLp3);

        cardTheme.addView(rowThemes);

        btnTrans.setOnClickListener(v -> {
            boolean cur = WidgetThemeManager.isTranslucent(this);
            WidgetThemeManager.setTranslucent(this, !cur);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, !cur ? "Прозрачность карточки включена" : "Прозрачность карточки выключена", Toast.LENGTH_SHORT).show();
        });
        cardTheme.addView(btnTrans);

        TextView hintGlass = new TextView(this);
        hintGlass.setText("При эффекте стекла прозрачной становится только карточка-подложка, а текст, цифры и обложки остаются 100% четкими.");
        hintGlass.setTextSize(11f);
        hintGlass.setTextColor(0xFF64748B);
        hintGlass.setPadding(0, (int) (8 * dp), 0, 0);
        cardTheme.addView(hintGlass);

        updateThemeButtons.run();
        root.addView(cardTheme);

        // Card Language: Language Selector
        LinearLayout cardLang = createCard(dp);
        TextView cardLangTitle = cardHeader("ЯЗЫК ВИДЖЕТОВ И ПРИЛОЖЕНИЯ / LANGUAGE", dp);
        cardLang.addView(cardLangTitle);

        LinearLayout rowLang = new LinearLayout(this);
        rowLang.setOrientation(LinearLayout.HORIZONTAL);
        rowLang.setPadding(0, (int) (8 * dp), 0, (int) (8 * dp));

        Button btnLangRu = new Button(this);
        Button btnLangEn = new Button(this);

        final Runnable updateLangButtons = () -> {
            String curLang = LanguageManager.getLanguage(this);
            styleSegment(btnLangRu, "🇷🇺 Русский", "ru".equals(curLang), dp);
            styleSegment(btnLangEn, "🇬🇧 English", "en".equals(curLang), dp);
        };

        btnLangRu.setOnClickListener(v -> {
            LanguageManager.setLanguage(this, "ru");
            updateLangButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, "Язык виджетов: Русский", Toast.LENGTH_SHORT).show();
            recreate();
        });

        btnLangEn.setOnClickListener(v -> {
            LanguageManager.setLanguage(this, "en");
            updateLangButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, "Widget language: English", Toast.LENGTH_SHORT).show();
            recreate();
        });

        LinearLayout.LayoutParams langLp1 = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        langLp1.setMarginEnd((int) (6 * dp));
        rowLang.addView(btnLangRu, langLp1);

        LinearLayout.LayoutParams langLp2 = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        rowLang.addView(btnLangEn, langLp2);

        cardLang.addView(rowLang);
        updateLangButtons.run();
        root.addView(cardLang);

        // Card 1: Notification Access Status
        LinearLayout cardNotif = createCard(dp);
        TextView cardNotifTitle = cardHeader("ДОСТУП К УВЕДОМЛЕНИЯМ И ПЛЕЕРУ", dp);
        cardNotif.addView(cardNotifTitle);

        mStatusNotif = new TextView(this);
        mStatusNotif.setTextSize(14f);
        mStatusNotif.setPadding(0, (int) (6 * dp), 0, (int) (12 * dp));
        cardNotif.addView(mStatusNotif);

        mBtnNotif = createButton("Открыть настройки доступа", 0xFF0284C7, 0xFF38BDF8, dp, () -> {
            try {
                Intent it = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(it);
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть настройки", Toast.LENGTH_SHORT).show();
            }
        });
        cardNotif.addView(mBtnNotif);

        Button btnRootNotif = createButton("⚡ Выдать доступ к уведомлениям через ROOT (1 клик)", 0xFF047857, 0xFF10B981, dp, () -> {
            new Thread(() -> {
                boolean ok = RootHelper.grantNotificationAccess(getPackageName());
                try {
                    android.service.notification.NotificationListenerService.requestRebind(
                            new ComponentName(this, BlueOSMediaListenerService.class));
                } catch (Throwable ignored) {}
                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, "✓ Root: доступ к уведомлениям успешно выдан!", Toast.LENGTH_LONG).show();
                        updateStatus();
                    } else {
                        Toast.makeText(this, "Не удалось выполнить команду через su (APatch)", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
        cardNotif.addView(btnRootNotif);

        TextView hintNotif = new TextView(this);
        hintNotif.setText("Для HyperOS / Android 14+:\nЕсли переключатель недоступен («Ограниченные настройки»), откройте «О приложении» -> 3 точки вверху -> «Разрешить ограниченные настройки».");
        hintNotif.setTextSize(11f);
        hintNotif.setTextColor(0xFF64748B);
        hintNotif.setPadding(0, (int) (10 * dp), 0, 0);
        cardNotif.addView(hintNotif);

        root.addView(cardNotif);

        // Card 2: HyperOS Battery & Autostart
        LinearLayout cardBattery = createCard(dp);
        TextView cardBatteryTitle = cardHeader("ФОНОВАЯ РАБОТА В HYPEROS", dp);
        cardBattery.addView(cardBatteryTitle);

        TextView descBattery = new TextView(this);
        descBattery.setText("Чтобы HyperOS не усыпляла виджеты плеера и эквалайзера в фоне, отключите для приложения контроль активности.");
        descBattery.setTextSize(13f);
        descBattery.setTextColor(0xFFCBD5E1);
        descBattery.setPadding(0, (int) (6 * dp), 0, (int) (12 * dp));
        cardBattery.addView(descBattery);

        Button btnAppInfo = createButton("О приложении (Автозапуск / Батарея)", 0xFF1E293B, 0xFF334155, dp, () -> {
            try {
                Intent it = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                it.setData(Uri.parse("package:" + getPackageName()));
                startActivity(it);
            } catch (Exception e) {
                Toast.makeText(this, "Не удалось открыть параметры", Toast.LENGTH_SHORT).show();
            }
        });
        cardBattery.addView(btnAppInfo);

        root.addView(cardBattery);

        // Card 3: Active Media Monitor
        LinearLayout cardMonitor = createCard(dp);
        TextView cardMonitorTitle = cardHeader("МОНИТОР МУЗЫКИ И ВИДЖЕТОВ", dp);
        cardMonitor.addView(cardMonitorTitle);

        mStatusPlayer = new TextView(this);
        mStatusPlayer.setTextSize(13f);
        mStatusPlayer.setTextColor(0xFF94A3B8);
        mStatusPlayer.setPadding(0, (int) (6 * dp), 0, (int) (12 * dp));
        cardMonitor.addView(mStatusPlayer);

        Button btnRefresh = createButton("Обновить все виджеты", 0xFF0284C7, 0xFF38BDF8, dp, () -> {
            BlueOSMediaWidgetProvider.pushAll(this);
            BlueOSEqualizerWidgetProvider.pushAll(this);
            updateStatus();
            Toast.makeText(this, "Виджеты обновлены!", Toast.LENGTH_SHORT).show();
        });
        cardMonitor.addView(btnRefresh);

        root.addView(cardMonitor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            android.service.notification.NotificationListenerService.requestRebind(
                    new ComponentName(this, BlueOSMediaListenerService.class));
        } catch (Throwable ignored) {}
        updateStatus();
    }

    private void updateStatus() {
        boolean connected = BlueOSMediaListenerService.isConnected();
        if (connected) {
            mStatusNotif.setText("● Доступ к уведомлениям: АКТИВЕН (метаданные считываются)");
            mStatusNotif.setTextColor(0xFF4ADE80);
            mBtnNotif.setText("Настройки доступа (включено)");
        } else {
            mStatusNotif.setText("▲ Доступ к уведомлениям: ВЫКЛЮЧЕН\n(Включите для показа названия трека и обложки)");
            mStatusNotif.setTextColor(0xFFFBBF24);
            mBtnNotif.setText("Включить доступ к уведомлениям");
        }

        String title = BlueOSMediaWidgetProvider.getTrackTitle();
        String artist = BlueOSMediaWidgetProvider.getTrackArtist();
        boolean playing = BlueOSMediaWidgetProvider.isPlaying(this);

        StringBuilder sb = new StringBuilder();
        sb.append("Состояние: ").append(playing ? "Воспроизведение ▶" : "Пауза ⏸").append("\n");
        sb.append("Трек: ").append(title != null ? title : "Не определен").append("\n");
        sb.append("Исполнитель: ").append(artist != null ? artist : "—");
        mStatusPlayer.setText(sb.toString());
    }

    private LinearLayout createCard(float dp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (16 * dp), (int) (16 * dp), (int) (16 * dp), (int) (16 * dp));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = (int) (16 * dp);
        card.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFF131D2D);
        gd.setStroke((int) (1.5f * dp), 0xFF1E324D);
        gd.setCornerRadius(16 * dp);
        card.setBackground(gd);

        return card;
    }

    private TextView cardHeader(String text, float dp) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11f);
        tv.setLetterSpacing(0.08f);
        tv.setTextColor(0xFF00E5FF);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        return tv;
    }

    private Button createButton(String text, int bg, int stroke, float dp, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13f);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bg);
        gd.setStroke((int) (1.5f * dp), stroke);
        gd.setCornerRadius(10 * dp);
        b.setBackground(gd);

        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void styleSegment(Button b, String text, boolean active, float dp) {
        b.setText(text);
        b.setTextSize(12f);
        b.setAllCaps(false);
        b.setTextColor(active ? Color.WHITE : 0xFF94A3B8);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(active ? 0xFF0284C7 : 0xFF131D2D);
        gd.setStroke((int) (1.5f * dp), active ? 0xFF38BDF8 : 0xFF1E293B);
        gd.setCornerRadius(8 * dp);
        b.setBackground(gd);
    }

    private void styleTranslucentBtn(Button b, boolean active, float dp) {
        b.setTextSize(12f);
        b.setAllCaps(false);
        b.setTextColor(active ? Color.WHITE : 0xFF94A3B8);
        b.setPadding(0, (int) (10 * dp), 0, (int) (10 * dp));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(active ? 0xFF0284C7 : 0xFF1E293B);
        gd.setStroke((int) (1.5f * dp), active ? 0xFF00E5FF : 0xFF334155);
        gd.setCornerRadius(10 * dp);
        b.setBackground(gd);
    }

    public static void pushAllWidgets(Context context) {
        try {
            Context ctx = LanguageManager.applyLocale(context);
            BlueOSMediaWidgetProvider.pushAll(ctx);
            BlueOSEqualizerWidgetProvider.pushAll(ctx);

            android.appwidget.AppWidgetManager wm = android.appwidget.AppWidgetManager.getInstance(ctx);

            int[] c1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Card.class));
            if (c1 != null && c1.length > 0) new BlueOSClockWidgetProvider.Card().onUpdate(ctx, wm, c1);
            int[] c2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.CardWide.class));
            if (c2 != null && c2.length > 0) new BlueOSClockWidgetProvider.CardWide().onUpdate(ctx, wm, c2);
            int[] c3 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Tile.class));
            if (c3 != null && c3.length > 0) new BlueOSClockWidgetProvider.Tile().onUpdate(ctx, wm, c3);
            int[] c5 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Halo.class));
            if (c5 != null && c5.length > 0) new BlueOSClockWidgetProvider.Halo().onUpdate(ctx, wm, c5);
            int[] c6 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Moe.class));
            if (c6 != null && c6.length > 0) new BlueOSClockWidgetProvider.Moe().onUpdate(ctx, wm, c6);

            int[] b1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSBatteryWidgetProvider.Small.class));
            if (b1 != null && b1.length > 0) new BlueOSBatteryWidgetProvider.Small().onUpdate(ctx, wm, b1);
            int[] b2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSBatteryWidgetProvider.Medium.class));
            if (b2 != null && b2.length > 0) new BlueOSBatteryWidgetProvider.Medium().onUpdate(ctx, wm, b2);
            int[] b3 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSBatteryWidgetProvider.Large.class));
            if (b3 != null && b3.length > 0) new BlueOSBatteryWidgetProvider.Large().onUpdate(ctx, wm, b3);

            int[] d1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSDateWidgetProvider.class));
            if (d1 != null && d1.length > 0) new BlueOSDateWidgetProvider().onUpdate(ctx, wm, d1);

            int[] d2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSCalendarMonthWidgetProvider.class));
            if (d2 != null && d2.length > 0) new BlueOSCalendarMonthWidgetProvider().onUpdate(ctx, wm, d2);

            int[] cd1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSCountdownWidgetProvider.class));
            if (cd1 != null && cd1.length > 0) new BlueOSCountdownWidgetProvider().onUpdate(ctx, wm, cd1);

            int[] p1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSAronaPetWidgetProvider.class));
            if (p1 != null && p1.length > 0) new BlueOSAronaPetWidgetProvider().onUpdate(ctx, wm, p1);

            int[] p2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSPlanaPetWidgetProvider.class));
            if (p2 != null && p2.length > 0) new BlueOSPlanaPetWidgetProvider().onUpdate(ctx, wm, p2);

            int[] p3 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSDuoPetWidgetProvider.class));
            if (p3 != null && p3.length > 0) new BlueOSDuoPetWidgetProvider().onUpdate(ctx, wm, p3);

            int[] s1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSSystemWidgetProvider.class));
            if (s1 != null && s1.length > 0) new BlueOSSystemWidgetProvider().onUpdate(ctx, wm, s1);
        } catch (Throwable ignored) {}
    }
}
