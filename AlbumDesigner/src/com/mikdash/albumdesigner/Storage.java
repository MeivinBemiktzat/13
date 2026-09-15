package com.mikdash.albumdesigner;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Persists album projects and their thumbnails to app-private storage. */
public final class Storage {

    private static File dir(Context c) {
        File d = new File(c.getFilesDir(), "projects");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    public static void save(Context c, Model.Project p) {
        p.modified = System.currentTimeMillis();
        File f = new File(dir(c), p.id + ".json");
        try (FileOutputStream o = new FileOutputStream(f)) {
            o.write(p.toJson().getBytes("UTF-8"));
        } catch (Exception ignored) {}
    }

    public static void saveThumb(Context c, String id, Bitmap bmp) {
        File f = new File(dir(c), id + ".jpg");
        try (FileOutputStream o = new FileOutputStream(f)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, 82, o);
        } catch (Exception ignored) {}
    }

    public static File thumbFile(Context c, String id) {
        return new File(dir(c), id + ".jpg");
    }

    public static Model.Project load(Context c, String id) {
        File f = new File(dir(c), id + ".json");
        if (!f.exists()) return null;
        try (FileInputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return Model.Project.fromJson(new String(bo.toByteArray(), "UTF-8"));
        } catch (Exception e) { return null; }
    }

    public static void delete(Context c, String id) {
        new File(dir(c), id + ".json").delete();
        new File(dir(c), id + ".jpg").delete();
    }

    public static List<Model.Project> list(Context c) {
        List<Model.Project> out = new ArrayList<>();
        File[] files = dir(c).listFiles();
        if (files != null) for (File f : files) {
            if (f.getName().endsWith(".json")) {
                Model.Project p = load(c, f.getName().replace(".json", ""));
                if (p != null) out.add(p);
            }
        }
        Collections.sort(out, new Comparator<Model.Project>() {
            public int compare(Model.Project a, Model.Project b) {
                return Long.compare(b.modified, a.modified);
            }
        });
        return out;
    }

    public static String newId() {
        return "alb_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 9999);
    }

    private Storage() {}
}
