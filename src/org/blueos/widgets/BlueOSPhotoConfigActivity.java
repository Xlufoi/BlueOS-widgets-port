package org.blueos.widgets;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class BlueOSPhotoConfigActivity extends Activity {

    private static final int REQ_PICK = 1001;
    private int mWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Bitmap mBitmap = null;
    private String mShape = "ROUNDED";

    private ImageView mPreview;
    private ImageView mPreviewRing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        mWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setContentView(R.layout.blueos_photo_config);
        mPreview = findViewById(R.id.blueos_photo_preview);
        mPreviewRing = findViewById(R.id.blueos_photo_preview_ring);

        RadioGroup rg = findViewById(R.id.blueos_photo_shape);
        rg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.blueos_photo_shape_circle) {
                mShape = "CIRCLE";
            } else if (checkedId == R.id.blueos_photo_shape_sticker) {
                mShape = "STICKER";
            } else {
                mShape = "ROUNDED";
            }
            updatePreview();
        });

        Button btnChange = findViewById(R.id.blueos_photo_change);
        btnChange.setOnClickListener(v -> pickImage());

        Button btnDone = findViewById(R.id.blueos_photo_done);
        btnDone.setOnClickListener(v -> saveAndFinish());

        File existing = BlueOSPhotoWidgetProvider.photoFile(this, mWidgetId);
        if (existing != null && existing.exists()) {
            mBitmap = BitmapFactory.decodeFile(existing.getAbsolutePath());
        }
        if (mBitmap == null) {
            // default arona bitmap
            mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.blueos_arona_sticker);
        }
        updatePreview();
    }

    private void pickImage() {
        Intent it = new Intent(Intent.ACTION_GET_CONTENT);
        it.setType("image/*");
        startActivityForResult(it, REQ_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri uri = data.getData();
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap b = BitmapFactory.decodeStream(is);
                if (is != null) is.close();
                if (b != null) {
                    mBitmap = scaleDown(b, 800);
                    updatePreview();
                }
            } catch (Throwable t) {
                Toast.makeText(this, R.string.blueos_photo_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updatePreview() {
        if (mBitmap == null) return;
        if ("CIRCLE".equals(mShape)) {
            mPreview.setImageBitmap(toCircle(mBitmap));
            mPreviewRing.setVisibility(View.VISIBLE);
        } else {
            mPreview.setImageBitmap(mBitmap);
            mPreviewRing.setVisibility(View.GONE);
        }
    }

    private void saveAndFinish() {
        if (mBitmap != null) {
            try {
                Bitmap out = "CIRCLE".equals(mShape) ? toCircle(mBitmap) : mBitmap;
                File dir = BlueOSPhotoWidgetProvider.photosDir(this);
                File file = new File(dir, UUID.randomUUID().toString() + ".png");
                FileOutputStream fos = new FileOutputStream(file);
                out.compress(Bitmap.CompressFormat.PNG, 95, fos);
                fos.close();

                File old = BlueOSPhotoWidgetProvider.photoFile(this, mWidgetId);
                if (old != null && old.exists()) old.delete();

                BlueOSPhotoWidgetProvider.prefs(this).edit()
                        .putString(BlueOSPhotoWidgetProvider.keyFile(mWidgetId), file.getName())
                        .putString(BlueOSPhotoWidgetProvider.keyShape(mWidgetId), mShape)
                        .apply();
            } catch (Throwable ignored) {}
        }

        BlueOSPhotoWidgetProvider.update(this, AppWidgetManager.getInstance(this), mWidgetId);
        setResult(RESULT_OK, new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId));
        finish();
    }

    private static Bitmap scaleDown(Bitmap b, int max) {
        int w = b.getWidth();
        int h = b.getHeight();
        if (w <= max && h <= max) return b;
        float ratio = (float) max / Math.max(w, h);
        return Bitmap.createScaledBitmap(b, Math.max(1, (int) (w * ratio)), Math.max(1, (int) (h * ratio)), true);
    }

    private static Bitmap toCircle(Bitmap b) {
        int size = Math.min(b.getWidth(), b.getHeight());
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawCircle(size / 2f, size / 2f, size / 2f, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        int left = (b.getWidth() - size) / 2;
        int top = (b.getHeight() - size) / 2;
        c.drawBitmap(b, new Rect(left, top, left + size, top + size), new Rect(0, 0, size, size), p);
        return out;
    }
}
