package org.blueos.widgets;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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

        if (!LanguageManager.isOnboardingCompleted(this)) {
            showLanguageOnboardingDialog(dp);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF090E17);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding((int) (22 * dp), (int) (32 * dp), (int) (22 * dp), (int) (32 * dp));
        scroll.addView(root);
        setContentView(scroll);

        boolean isRu = LanguageManager.isRussian(this);

        // Header
        TextView header = new TextView(this);
        header.setText("✦ KIVOTOS BLUEOS ✦");
        header.setTextSize(12f);
        header.setLetterSpacing(0.15f);
        header.setTextColor(0xFF00E5FF);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(header);

        TextView title = new TextView(this);
        title.setText(isRu ? "Виджеты BlueOS" : "BlueOS Widgets");
        title.setTextSize(24f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, (int) (4 * dp), 0, (int) (2 * dp));
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText(isRu
                ? "Настройка оформления, тем и служб для HyperOS"
                : "Themes, styling and system service setup for HyperOS");
        subtitle.setTextSize(13f);
        subtitle.setTextColor(0xFF94A3B8);
        subtitle.setGravity(Gravity.CENTER_HORIZONTAL);
        subtitle.setPadding(0, 0, 0, (int) (24 * dp));
        root.addView(subtitle);

        // ==========================================
        // Card 0: Widget Theme & Translucency
        // ==========================================
        LinearLayout cardTheme = createCard(dp);
        TextView cardThemeTitle = cardHeader(isRu ? "ТЕМА И ОФОРМЛЕНИЕ ВИДЖЕТОВ" : "WIDGET THEME & STYLE", dp);
        cardTheme.addView(cardThemeTitle);

        TextView descTheme = new TextView(this);
        descTheme.setText(isRu
                ? "Цветовая тема и прозрачность для виджетов с заполнением:"
                : "Background plate theme and opacity for filled widgets:");
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
        Button btnNavy = new Button(this);
        Button btnTrans = new Button(this);

        TextView tvAlphaLabel = new TextView(this);
        tvAlphaLabel.setTextSize(12f);
        tvAlphaLabel.setTextColor(0xFF94A3B8);
        tvAlphaLabel.setPadding(0, (int) (6 * dp), 0, (int) (2 * dp));

        SeekBar sbAlpha = new SeekBar(this);
        sbAlpha.setMax(100);

        final Runnable updateThemeButtons = () -> {
            int cur = WidgetThemeManager.getTheme(this);
            int alphaVal = WidgetThemeManager.getBgAlpha(this);
            int pct = (int) Math.round((alphaVal / 255.0) * 100);

            styleSegment(btnBlack, "Pure Black", cur == WidgetThemeManager.THEME_PURE_BLACK, dp);
            styleSegment(btnAosp, "AOSP Dark", cur == WidgetThemeManager.THEME_AOSP_DARK, dp);
            styleSegment(btnMonet, "Monet", cur == WidgetThemeManager.THEME_MONET, dp);
            styleSegment(btnNavy, "Navy Slate", cur == WidgetThemeManager.THEME_NAVY_SLATE, dp);

            btnTrans.setText(isRu
                    ? "✦ Быстрый пресет «Стекло (70%)»: " + (alphaVal < 240 ? "ВКЛ" : "ВЫКЛ")
                    : "✦ Quick Glass Preset (70%): " + (alphaVal < 240 ? "ON" : "OFF"));
            styleTranslucentBtn(btnTrans, alphaVal < 240, dp);

            tvAlphaLabel.setText((isRu ? "Непрозрачность подложки: " : "Plate Opacity: ") + pct + "%");
            if (sbAlpha.getProgress() != pct) {
                sbAlpha.setProgress(pct);
            }
        };

        btnBlack.setOnClickListener(v -> {
            WidgetThemeManager.setTheme(this, WidgetThemeManager.THEME_PURE_BLACK);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, isRu ? "Тема: Pure Black (Черный AMOLED)" : "Theme: Pure Black", Toast.LENGTH_SHORT).show();
        });
        btnAosp.setOnClickListener(v -> {
            WidgetThemeManager.setTheme(this, WidgetThemeManager.THEME_AOSP_DARK);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, isRu ? "Тема: AOSP Dark (Тёмно-серый)" : "Theme: AOSP Dark", Toast.LENGTH_SHORT).show();
        });
        btnMonet.setOnClickListener(v -> {
            WidgetThemeManager.setTheme(this, WidgetThemeManager.THEME_MONET);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, isRu ? "Тема: Monet (Цвета текущих обоев)" : "Theme: Monet (Wallpaper)", Toast.LENGTH_SHORT).show();
        });
        btnNavy.setOnClickListener(v -> {
            WidgetThemeManager.setTheme(this, WidgetThemeManager.THEME_NAVY_SLATE);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, isRu ? "Тема: Navy Slate (Тёмно-синий)" : "Theme: Navy Slate", Toast.LENGTH_SHORT).show();
        });

        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        btnLp.setMarginEnd((int) (4 * dp));
        rowThemes.addView(btnBlack, btnLp);

        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        btnLp2.setMarginEnd((int) (4 * dp));
        rowThemes.addView(btnAosp, btnLp2);

        LinearLayout.LayoutParams btnLp3 = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        btnLp3.setMarginEnd((int) (4 * dp));
        rowThemes.addView(btnMonet, btnLp3);

        LinearLayout.LayoutParams btnLp4 = new LinearLayout.LayoutParams(0, (int) (40 * dp), 1f);
        rowThemes.addView(btnNavy, btnLp4);

        cardTheme.addView(rowThemes);

        cardTheme.addView(tvAlphaLabel);
        sbAlpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    int alpha = (int) (progress * 2.55f);
                    WidgetThemeManager.setBgAlpha(MainActivity.this, alpha);
                    updateThemeButtons.run();
                    pushAllWidgets(MainActivity.this);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        cardTheme.addView(sbAlpha);

        btnTrans.setOnClickListener(v -> {
            boolean cur = WidgetThemeManager.isTranslucent(this);
            WidgetThemeManager.setTranslucent(this, !cur);
            updateThemeButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, isRu
                    ? (!cur ? "Эффект стекла включен (70% прозрачности)" : "Сплошная карточка (100% непрозрачность)")
                    : (!cur ? "Glass effect enabled (70% opacity)" : "Solid card enabled (100% opacity)"),
                    Toast.LENGTH_SHORT).show();
        });
        cardTheme.addView(btnTrans);

        TextView hintGlass = new TextView(this);
        hintGlass.setText(isRu
                ? "Настройки темы и прозрачности применяются к виджетам с карточной подложкой. Минималистичные виджеты стиля «Контур» (чистый контур без заливки) и прозрачные виджеты (Нимб, Кивотос, Эквалайзер, Сводка) выбираются отдельно в меню виджетов рабочего стола."
                : "Theme and opacity settings apply to filled card widgets. Minimalist outline widgets (clean contour without fill) and transparent widgets (Halo, Kivotos, Equalizer, Daily Summary) can be placed separately from your launcher's widget menu.");
        hintGlass.setTextSize(11f);
        hintGlass.setTextColor(0xFF64748B);
        hintGlass.setPadding(0, (int) (8 * dp), 0, 0);
        cardTheme.addView(hintGlass);

        updateThemeButtons.run();
        root.addView(cardTheme);

        // ==========================================
        // Card Language: Language Selector
        // ==========================================
        LinearLayout cardLang = createCard(dp);
        TextView cardLangTitle = cardHeader(isRu ? "ЯЗЫК ВИДЖЕТОВ И ПРИЛОЖЕНИЯ / LANGUAGE" : "LANGUAGE & LOCALIZATION", dp);
        cardLang.addView(cardLangTitle);

        TextView descLang = new TextView(this);
        descLang.setText(isRu
                ? "Явное переключение языка приложения и надписей виджетов:"
                : "Explicit widget and app interface language:");
        descLang.setTextSize(12f);
        descLang.setTextColor(0xFFCBD5E1);
        descLang.setPadding(0, (int) (4 * dp), 0, (int) (8 * dp));
        cardLang.addView(descLang);

        LinearLayout rowLang = new LinearLayout(this);
        rowLang.setOrientation(LinearLayout.HORIZONTAL);
        rowLang.setPadding(0, 0, 0, (int) (4 * dp));

        Button btnLangRu = new Button(this);
        Button btnLangEn = new Button(this);

        final Runnable updateLangButtons = () -> {
            String curLang = LanguageManager.getLanguage(this);
            styleSegment(btnLangRu, "🇷🇺 Русский", "ru".equals(curLang), dp);
            styleSegment(btnLangEn, "🇬🇧 English", "en".equals(curLang), dp);
        };

        btnLangRu.setOnClickListener(v -> {
            LanguageManager.setLanguage(this, "ru");
            LanguageManager.setOnboardingCompleted(this, true);
            updateLangButtons.run();
            pushAllWidgets(this);
            Toast.makeText(this, "Язык виджетов: Русский", Toast.LENGTH_SHORT).show();
            recreate();
        });

        btnLangEn.setOnClickListener(v -> {
            LanguageManager.setLanguage(this, "en");
            LanguageManager.setOnboardingCompleted(this, true);
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

        // ==========================================
        // Card 1: Notification Access Status
        // ==========================================
        LinearLayout cardNotif = createCard(dp);
        TextView cardNotifTitle = cardHeader(isRu ? "ДОСТУП К УВЕДОМЛЕНИЯМ И ПЛЕЕРУ" : "NOTIFICATION & MEDIA ACCESS", dp);
        cardNotif.addView(cardNotifTitle);

        mStatusNotif = new TextView(this);
        mStatusNotif.setTextSize(14f);
        mStatusNotif.setPadding(0, (int) (6 * dp), 0, (int) (12 * dp));
        cardNotif.addView(mStatusNotif);

        mBtnNotif = createButton(isRu ? "Открыть настройки доступа" : "Open Access Settings", 0xFF0284C7, 0xFF38BDF8, dp, () -> {
            try {
                Intent it = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                startActivity(it);
            } catch (Exception e) {
                Toast.makeText(this, isRu ? "Не удалось открыть настройки" : "Failed to open settings", Toast.LENGTH_SHORT).show();
            }
        });
        cardNotif.addView(mBtnNotif);

        Button btnRootNotif = createButton(isRu ? "⚡ Выдать доступ к уведомлениям через ROOT (1 клик)" : "⚡ Grant Notification Access via ROOT", 0xFF047857, 0xFF10B981, dp, () -> {
            new Thread(() -> {
                boolean ok = RootHelper.grantNotificationAccess(getPackageName());
                try {
                    android.service.notification.NotificationListenerService.requestRebind(
                            new ComponentName(this, BlueOSMediaListenerService.class));
                } catch (Throwable ignored) {}
                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, isRu ? "✓ Root: доступ к уведомлениям успешно выдан!" : "✓ Root: Access granted!", Toast.LENGTH_LONG).show();
                        updateStatus();
                    } else {
                        Toast.makeText(this, isRu ? "Не удалось выполнить команду через su (APatch)" : "su command failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
        cardNotif.addView(btnRootNotif);

        TextView hintNotif = new TextView(this);
        hintNotif.setText(isRu
                ? "Для HyperOS / Android 14+:\nЕсли переключатель недоступен («Ограниченные настройки»), откройте «О приложении» -> 3 точки вверху -> «Разрешить ограниченные настройки»."
                : "For HyperOS / Android 14+:\nIf toggle is greyed out ('Restricted setting'), open App Info -> 3 dots in top right -> 'Allow restricted settings'.");
        hintNotif.setTextSize(11f);
        hintNotif.setTextColor(0xFF64748B);
        hintNotif.setPadding(0, (int) (10 * dp), 0, 0);
        cardNotif.addView(hintNotif);

        root.addView(cardNotif);

        // ==========================================
        // Card 2: HyperOS Battery & Autostart
        // ==========================================
        LinearLayout cardBattery = createCard(dp);
        TextView cardBatteryTitle = cardHeader(isRu ? "ФОНОВАЯ РАБОТА В HYPEROS" : "BACKGROUND WORK IN HYPEROS", dp);
        cardBattery.addView(cardBatteryTitle);

        TextView descBattery = new TextView(this);
        descBattery.setText(isRu
                ? "Чтобы HyperOS не усыпляла виджеты плеера и эквалайзера в фоне, отключите для приложения контроль активности."
                : "To prevent HyperOS from killing player widgets in background, disable battery restrictions.");
        descBattery.setTextSize(13f);
        descBattery.setTextColor(0xFFCBD5E1);
        descBattery.setPadding(0, (int) (6 * dp), 0, (int) (12 * dp));
        cardBattery.addView(descBattery);

        Button btnBattery = createButton(isRu ? "Открыть «О приложении» (Контроль активности -> Нет ограничений)" : "Open App Info (Battery -> No restrictions)", 0xFF1E293B, 0xFF38BDF8, dp, () -> {
            try {
                Intent it = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                it.setData(Uri.parse("package:" + getPackageName()));
                startActivity(it);
            } catch (Exception e) {
                Toast.makeText(this, isRu ? "Не удалось открыть настройки приложения" : "Failed to open settings", Toast.LENGTH_SHORT).show();
            }
        });
        cardBattery.addView(btnBattery);

        Button btnAutostart = createButton(isRu ? "Открыть менеджер автозапуска (Включить автозапуск)" : "Open Autostart Manager", 0xFF1E293B, 0xFF38BDF8, dp, () -> {
            boolean opened = false;
            String[][] intents = {
                    {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                    {"com.miui.securitycenter", "com.miui.powercenter.PowerSettings"}
            };
            for (String[] act : intents) {
                try {
                    Intent it = new Intent();
                    it.setComponent(new ComponentName(act[0], act[1]));
                    startActivity(it);
                    opened = true;
                    break;
                } catch (Exception ignored) {}
            }
            if (!opened) {
                Toast.makeText(this, isRu ? "Не удалось открыть меню автозапуска напрямую" : "Could not open autostart menu directly", Toast.LENGTH_SHORT).show();
            }
        });
        cardBattery.addView(btnAutostart);

        Button btnRootBattery = createButton(isRu ? "⚡ Отключить контроль батареи через ROOT (1 клик)" : "⚡ Whitelist Battery via ROOT (1 click)", 0xFF047857, 0xFF10B981, dp, () -> {
            new Thread(() -> {
                boolean ok = RootHelper.whitelistBattery(getPackageName());
                runOnUiThread(() -> {
                    if (ok) {
                        Toast.makeText(this, isRu ? "✓ Root: контроль активности отключен (без ограничений)!" : "✓ Root: Battery restrictions removed!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, isRu ? "Не удалось выполнить команду через su (APatch)" : "su command failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
        cardBattery.addView(btnRootBattery);

        root.addView(cardBattery);

        // ==========================================
        // Card 3: Media Diagnostics
        // ==========================================
        LinearLayout cardDiag = createCard(dp);
        TextView cardDiagTitle = cardHeader(isRu ? "ДИАГНОСТИКА МЕДИА-СЕССИИ" : "MEDIA SESSION DIAGNOSTICS", dp);
        cardDiag.addView(cardDiagTitle);

        mStatusPlayer = new TextView(this);
        mStatusPlayer.setTextSize(13f);
        mStatusPlayer.setTextColor(0xFF94A3B8);
        mStatusPlayer.setPadding(0, (int) (6 * dp), 0, (int) (12 * dp));
        cardDiag.addView(mStatusPlayer);

        Button btnRefresh = createButton(isRu ? "Обновить все виджеты сейчас" : "Force Refresh All Widgets", 0xFF1E293B, 0xFF38BDF8, dp, () -> {
            pushAllWidgets(this);
            updateStatus();
            Toast.makeText(this, isRu ? "Все виджеты обновлены" : "All widgets refreshed", Toast.LENGTH_SHORT).show();
        });
        cardDiag.addView(btnRefresh);

        root.addView(cardDiag);
    }

    private void showLanguageOnboardingDialog(float dp) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        LinearLayout dRoot = new LinearLayout(this);
        dRoot.setOrientation(LinearLayout.VERTICAL);
        dRoot.setPadding((int) (24 * dp), (int) (24 * dp), (int) (24 * dp), (int) (24 * dp));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF0F172A);
        bg.setStroke((int) (1.5f * dp), 0xFF38BDF8);
        bg.setCornerRadius(20 * dp);
        dRoot.setBackground(bg);

        TextView tag = new TextView(this);
        tag.setText("✦ KIVOTOS BLUEOS ✦");
        tag.setTextSize(11f);
        tag.setLetterSpacing(0.15f);
        tag.setTextColor(0xFF00E5FF);
        tag.setGravity(Gravity.CENTER_HORIZONTAL);
        dRoot.addView(tag);

        TextView title = new TextView(this);
        title.setText("Choose Language / Выберите язык");
        title.setTextSize(18f);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, (int) (6 * dp), 0, (int) (4 * dp));
        dRoot.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Please select your interface and widget language:\nВыберите язык отображения интерфейса и виджетов:");
        sub.setTextSize(12f);
        sub.setTextColor(0xFF94A3B8);
        sub.setGravity(Gravity.CENTER_HORIZONTAL);
        sub.setPadding(0, 0, 0, (int) (18 * dp));
        dRoot.addView(sub);

        Button btnRu = createButton("🇷🇺 Русский", 0xFF0284C7, 0xFF38BDF8, dp, () -> {
            LanguageManager.setLanguage(this, "ru");
            LanguageManager.setOnboardingCompleted(this, true);
            dialog.dismiss();
            pushAllWidgets(this);
            recreate();
        });
        dRoot.addView(btnRu);

        Button btnEn = createButton("🇬🇧 English", 0xFF1E293B, 0xFF38BDF8, dp, () -> {
            LanguageManager.setLanguage(this, "en");
            LanguageManager.setOnboardingCompleted(this, true);
            dialog.dismiss();
            pushAllWidgets(this);
            recreate();
        });
        dRoot.addView(btnEn);

        dialog.setContentView(dRoot);
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        boolean isRu = LanguageManager.isRussian(this);
        boolean connected = BlueOSMediaListenerService.isConnected();
        if (connected) {
            mStatusNotif.setText(isRu
                    ? "✓ Служба активна (подключена к Android MediaSession)"
                    : "✓ Service Active (connected to Android MediaSession)");
            mStatusNotif.setTextColor(0xFF34D399);
            mBtnNotif.setVisibility(Button.GONE);
        } else {
            mStatusNotif.setText(isRu
                    ? "✕ Служба НЕ активна (виджеты не видят плеер)"
                    : "✕ Service NOT active (widgets cannot see music player)");
            mStatusNotif.setTextColor(0xFFF87171);
            mBtnNotif.setVisibility(Button.VISIBLE);
        }

        String title = BlueOSMediaWidgetProvider.getTrackTitle();
        String artist = BlueOSMediaWidgetProvider.getTrackArtist();
        boolean playing = BlueOSMediaWidgetProvider.isPlaying(this);

        if (title != null && !title.isEmpty()) {
            mStatusPlayer.setText((isRu ? "Текущий трек: " : "Now playing: ")
                    + title + (artist != null ? " — " + artist : "")
                    + " [" + (playing ? (isRu ? "ИГРАЕТ" : "PLAYING") : (isRu ? "ПАУЗА" : "PAUSED")) + "]");
            mStatusPlayer.setTextColor(0xFFE2E8F0);
        } else {
            mStatusPlayer.setText(isRu
                    ? "Трек не играет. Запустите музыку в Spotify, VK, Яндекс Музыке или YouTube Music."
                    : "No track playing. Start playing music in Spotify, YouTube Music or local player.");
            mStatusPlayer.setTextColor(0xFF64748B);
        }
    }

    private LinearLayout createCard(float dp) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding((int) (18 * dp), (int) (18 * dp), (int) (18 * dp), (int) (18 * dp));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(0xFF101725);
        gd.setStroke((int) (1.2f * dp), 0xFF1E293B);
        gd.setCornerRadius(16 * dp);
        card.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int) (14 * dp));
        card.setLayoutParams(lp);

        return card;
    }

    private TextView cardHeader(String text, float dp) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(11f);
        tv.setLetterSpacing(0.12f);
        tv.setTextColor(0xFF00E5FF);
        tv.setTypeface(tv.getTypeface(), android.graphics.Typeface.BOLD);
        tv.setPadding(0, 0, 0, (int) (6 * dp));
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

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int) (8 * dp));
        b.setLayoutParams(lp);

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
            BlueOSTogglesWidgetProvider.push(ctx);
            BlueOSSummaryWidgetProvider.push(ctx);
            BlueOSBatteryWidgetProvider.updateAll(ctx);
            BlueOSDateWidgetProvider.push(ctx);
            BlueOSSystemWidgetProvider.push(ctx);

            android.appwidget.AppWidgetManager wm = android.appwidget.AppWidgetManager.getInstance(ctx);

            int[] c1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Card.class));
            if (c1 != null && c1.length > 0) new BlueOSClockWidgetProvider.Card().onUpdate(ctx, wm, c1);
            int[] c1o = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.CardOutline.class));
            if (c1o != null && c1o.length > 0) new BlueOSClockWidgetProvider.CardOutline().onUpdate(ctx, wm, c1o);

            int[] c2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.CardWide.class));
            if (c2 != null && c2.length > 0) new BlueOSClockWidgetProvider.CardWide().onUpdate(ctx, wm, c2);
            int[] c2o = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.CardWideOutline.class));
            if (c2o != null && c2o.length > 0) new BlueOSClockWidgetProvider.CardWideOutline().onUpdate(ctx, wm, c2o);

            int[] c3 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Tile.class));
            if (c3 != null && c3.length > 0) new BlueOSClockWidgetProvider.Tile().onUpdate(ctx, wm, c3);
            int[] c3t = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.TileTransparent.class));
            if (c3t != null && c3t.length > 0) new BlueOSClockWidgetProvider.TileTransparent().onUpdate(ctx, wm, c3t);

            int[] c5 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Halo.class));
            if (c5 != null && c5.length > 0) new BlueOSClockWidgetProvider.Halo().onUpdate(ctx, wm, c5);
            int[] c5c = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.HaloCard.class));
            if (c5c != null && c5c.length > 0) new BlueOSClockWidgetProvider.HaloCard().onUpdate(ctx, wm, c5c);

            int[] c6 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Moe.class));
            if (c6 != null && c6.length > 0) new BlueOSClockWidgetProvider.Moe().onUpdate(ctx, wm, c6);
            int[] c6c = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.MoeCard.class));
            if (c6c != null && c6c.length > 0) new BlueOSClockWidgetProvider.MoeCard().onUpdate(ctx, wm, c6c);
            BlueOSClockWidgetProvider.updateMoeClocks(ctx);

            int[] c7 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.Alarm.class));
            if (c7 != null && c7.length > 0) new BlueOSClockWidgetProvider.Alarm().onUpdate(ctx, wm, c7);
            int[] c7t = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSClockWidgetProvider.AlarmTransparent.class));
            if (c7t != null && c7t.length > 0) new BlueOSClockWidgetProvider.AlarmTransparent().onUpdate(ctx, wm, c7t);

            int[] d2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSCalendarMonthWidgetProvider.class));
            if (d2 != null && d2.length > 0) new BlueOSCalendarMonthWidgetProvider().onUpdate(ctx, wm, d2);
            int[] d2t = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSCalendarMonthWidgetProvider.Transparent.class));
            if (d2t != null && d2t.length > 0) new BlueOSCalendarMonthWidgetProvider.Transparent().onUpdate(ctx, wm, d2t);

            int[] cd1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSCountdownWidgetProvider.class));
            if (cd1 != null && cd1.length > 0) new BlueOSCountdownWidgetProvider().onUpdate(ctx, wm, cd1);
            int[] cd2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSCountdownWidgetProvider.Outline.class));
            if (cd2 != null && cd2.length > 0) new BlueOSCountdownWidgetProvider.Outline().onUpdate(ctx, wm, cd2);

            int[] p1 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSAronaPetWidgetProvider.class));
            if (p1 != null && p1.length > 0) new BlueOSAronaPetWidgetProvider().onUpdate(ctx, wm, p1);
            int[] p1t = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSAronaPetWidgetProvider.Transparent.class));
            if (p1t != null && p1t.length > 0) new BlueOSAronaPetWidgetProvider.Transparent().onUpdate(ctx, wm, p1t);

            int[] p2 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSPlanaPetWidgetProvider.class));
            if (p2 != null && p2.length > 0) new BlueOSPlanaPetWidgetProvider().onUpdate(ctx, wm, p2);
            int[] p2t = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSPlanaPetWidgetProvider.Transparent.class));
            if (p2t != null && p2t.length > 0) new BlueOSPlanaPetWidgetProvider.Transparent().onUpdate(ctx, wm, p2t);

            int[] p3 = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSDuoPetWidgetProvider.class));
            if (p3 != null && p3.length > 0) new BlueOSDuoPetWidgetProvider().onUpdate(ctx, wm, p3);
            int[] p3t = wm.getAppWidgetIds(new ComponentName(ctx, BlueOSDuoPetWidgetProvider.Transparent.class));
            if (p3t != null && p3t.length > 0) new BlueOSDuoPetWidgetProvider.Transparent().onUpdate(ctx, wm, p3t);
        } catch (Throwable ignored) {}
    }
}
