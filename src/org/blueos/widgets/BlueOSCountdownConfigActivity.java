package org.blueos.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Switch;

import java.time.LocalDate;
import java.time.ZoneId;

public class BlueOSCountdownConfigActivity extends Activity {

    private int mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        mWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }
        setContentView(R.layout.blueos_countdown_config);

        final EditText title = findViewById(R.id.blueos_countdown_title_input);
        final DatePicker date = findViewById(R.id.blueos_countdown_date);
        final Switch yearly = findViewById(R.id.blueos_countdown_yearly);

        final SharedPreferences prefs = BlueOSCountdownWidgetProvider.prefs(this);
        title.setText(prefs.getString(BlueOSCountdownWidgetProvider.keyTitle(mWidgetId), ""));
        final long epochDay = prefs.getLong(BlueOSCountdownWidgetProvider.keyDate(mWidgetId), Long.MIN_VALUE);
        final LocalDate initial = epochDay == Long.MIN_VALUE
                ? LocalDate.now(ZoneId.systemDefault())
                : LocalDate.ofEpochDay(epochDay);
        date.init(initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth(), null);
        yearly.setChecked(prefs.getBoolean(BlueOSCountdownWidgetProvider.keyYearly(mWidgetId), false));

        findViewById(R.id.blueos_countdown_done).setOnClickListener(v -> {
            final LocalDate picked = LocalDate.of(date.getYear(), date.getMonth() + 1,
                    date.getDayOfMonth());
            prefs.edit()
                    .putString(BlueOSCountdownWidgetProvider.keyTitle(mWidgetId),
                            title.getText().toString().trim())
                    .putLong(BlueOSCountdownWidgetProvider.keyDate(mWidgetId), picked.toEpochDay())
                    .putBoolean(BlueOSCountdownWidgetProvider.keyYearly(mWidgetId),
                            yearly.isChecked())
                    .apply();

            final AppWidgetManager wm = AppWidgetManager.getInstance(this);
            BlueOSCountdownWidgetProvider.update(this, wm, mWidgetId);
            setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId));
            finish();
        });
    }
}
