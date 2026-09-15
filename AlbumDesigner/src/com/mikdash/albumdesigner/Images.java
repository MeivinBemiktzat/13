package com.mikdash.albumdesigner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;

import java.io.InputStream;

/** Loads and caches downsampled bitmaps from content URIs or file paths. */
public final class Images implements Renderer.ImageProvider {

    private final Context ctx;
    private final LruCache<String, Bitmap> cache;
    private final int maxDim;

    public Images(Context c, int maxDim) {
        this.ctx = c.getApplicationContext();
        this.maxDim = maxDim;
        int mem = (int) (Runtime.getRuntime().maxMemory() / 1024);
        cache = new LruCache<String, Bitmap>(mem / 6) {
            protected int sizeOf(String k, Bitmap b) { return b.getByteCount() / 1024; }
        };
    }

    public Bitmap get(String uri) {
        if (uri == null) return null;
        Bitmap b = cache.get(uri);
        if (b != null) return b;
        b = decode(uri, maxDim);
        if (b != null) cache.put(uri, b);
        return b;
    }

    public void clear() { cache.evictAll(); }

    private InputStream open(String uri) throws Exception {
        if (uri.startsWith("content://")) return ctx.getContentResolver().openInputStream(Uri.parse(uri));
        if (uri.startsWith("file://")) return ctx.getContentResolver().openInputStream(Uri.parse(uri));
        return new java.io.FileInputStream(uri);
    }

    /** Full-resolution (or capped) decode for export. */
    public Bitmap decode(String uri, int cap) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream is = open(uri)) { BitmapFactory.decodeStream(is, null, o); }
            int sample = 1;
            int big = Math.max(o.outWidth, o.outHeight);
            while (big / sample > cap) sample *= 2;
            BitmapFactory.Options d = new BitmapFactory.Options();
            d.inSampleSize = sample;
            d.inPreferredConfig = Bitmap.Config.ARGB_8888;
            try (InputStream is = open(uri)) { return BitmapFactory.decodeStream(is, null, d); }
        } catch (Throwable t) {
            return null;
        }
    }
}
