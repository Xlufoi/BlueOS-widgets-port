package org.blueos.widgets;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

public class BlueOSPhotoProvider extends ContentProvider {

    private static final String AUTHORITY = "org.blueos.widgets.photos";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new SecurityException("Read-only provider");
        String name = uri.getLastPathSegment();
        if (name == null || getContext() == null) throw new FileNotFoundException(uri.toString());

        File f = new File(BlueOSPhotoWidgetProvider.photosDir(getContext()), name);
        if (!f.exists()) throw new FileNotFoundException(uri.toString());
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        String seg = uri.getLastPathSegment();
        if (seg != null && seg.endsWith(".png")) return "image/png";
        if (seg != null && seg.endsWith(".jpg")) return "image/jpeg";
        return null;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
