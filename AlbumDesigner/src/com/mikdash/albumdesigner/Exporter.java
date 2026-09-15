package com.mikdash.albumdesigner;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/** Renders pages to full-resolution images/PDF and saves them offline to the device. */
public final class Exporter {

    private static final Renderer R = new Renderer();

    /** Renders one page to a bitmap at the given long-edge target resolution. */
    public static Bitmap renderPage(Model.Project p, Model.Page page, Images images, int longEdge) {
        int pw = p.pw(), ph = p.ph();
        float s = (float) longEdge / Math.max(pw, ph);
        int w = Math.round(pw * s), h = Math.round(ph * s);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.scale((float) w / pw, (float) h / ph);
        R.drawPage(c, page, pw, ph, new Renderer.ImageProvider() {
            public Bitmap get(String uri) { return images.decode(uri, 2400); }
        });
        return bmp;
    }

    public static Bitmap thumb(Model.Project p, Model.Page page, Images images, int longEdge) {
        int pw = p.pw(), ph = p.ph();
        float s = (float) longEdge / Math.max(pw, ph);
        int w = Math.round(pw * s), h = Math.round(ph * s);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.scale((float) w / pw, (float) h / ph);
        R.drawPage(c, page, pw, ph, images);
        return bmp;
    }

    /** Saves every page as a JPEG into the gallery (Pictures/AlbumDesigner). Returns first Uri. */
    public static Uri saveAllJpeg(Context ctx, Model.Project p, Images images) throws Exception {
        Uri first = null;
        for (int i = 0; i < p.pages.size(); i++) {
            Bitmap bmp = renderPage(p, p.pages.get(i), images, 2400);
            Uri u = saveImageToGallery(ctx, bmp, safe(p.name) + "_" + (i + 1) + ".jpg");
            if (first == null) first = u;
            bmp.recycle();
        }
        return first;
    }

    private static Uri saveImageToGallery(Context ctx, Bitmap bmp, String name) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Images.Media.DISPLAY_NAME, name);
            v.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            v.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AlbumDesigner");
            Uri uri = ctx.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
            }
            return uri;
        } else {
            File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "AlbumDesigner");
            if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, name);
            try (FileOutputStream os = new FileOutputStream(f)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, os);
            }
            return Uri.fromFile(f);
        }
    }

    /** Exports the whole album as a single PDF into app cache; returns a shareable file. */
    public static File exportPdf(Context ctx, Model.Project p, Images images) throws Exception {
        PdfDocument doc = new PdfDocument();
        int pw = p.pw(), ph = p.ph();
        for (int i = 0; i < p.pages.size(); i++) {
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pw, ph, i + 1).create();
            PdfDocument.Page pdfPage = doc.startPage(info);
            R.drawPage(pdfPage.getCanvas(), p.pages.get(i), pw, ph, new Renderer.ImageProvider() {
                public Bitmap get(String uri) { return images.decode(uri, 2000); }
            });
            doc.finishPage(pdfPage);
        }
        File out = new File(exportDir(ctx), safe(p.name) + ".pdf");
        try (FileOutputStream os = new FileOutputStream(out)) { doc.writeTo(os); }
        doc.close();
        return out;
    }

    public static File exportDir(Context ctx) {
        File d = new File(ctx.getCacheDir(), "export");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    private static String safe(String s) {
        if (s == null) return "album";
        String r = s.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return r.isEmpty() ? "album" : r;
    }

    private Exporter() {}
}
