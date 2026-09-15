package com.mikdash.albumdesigner;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;

/**
 * Minimal, dependency-free provider that shares exported files from the app's
 * export cache via content:// URIs (so PDFs can be shared on API 24+ without
 * the support-library FileProvider).
 */
public class AlbumFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.mikdash.albumdesigner.fileprovider";

    public static Uri uriFor(File f) {
        return Uri.parse("content://" + AUTHORITY + "/" + f.getName());
    }

    private File resolve(Uri uri) {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("..") || name.contains("/")) return null;
        return new File(Exporter.exportDir(getContext()), name);
    }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        File f = resolve(uri);
        if (f == null || !f.exists()) return null;
        try { return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY); }
        catch (Exception e) { return null; }
    }

    @Override
    public Cursor query(Uri uri, String[] proj, String sel, String[] args, String sort) {
        File f = resolve(uri);
        if (f == null) return null;
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }

    @Override
    public String getType(Uri uri) {
        String n = uri.getLastPathSegment();
        if (n != null && n.endsWith(".pdf")) return "application/pdf";
        if (n != null && (n.endsWith(".jpg") || n.endsWith(".jpeg"))) return "image/jpeg";
        return "application/octet-stream";
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
