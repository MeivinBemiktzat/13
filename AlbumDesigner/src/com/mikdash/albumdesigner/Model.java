package com.mikdash.albumdesigner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model for an album project. Everything is stored in a canonical page
 * coordinate space (see {@link Format}); the editor maps that onto the screen.
 * Fully self-contained and JSON-serializable so projects persist offline.
 */
public final class Model {

    /* ----------------------------- Element ----------------------------- */

    public static final int KIND_PHOTO = 0;
    public static final int KIND_TEXT = 1;
    public static final int KIND_SHAPE = 2;
    public static final int KIND_STICKER = 3;

    public static final int SHAPE_RECT = 0;
    public static final int SHAPE_ROUND = 1;
    public static final int SHAPE_CIRCLE = 2;
    public static final int SHAPE_TRIANGLE = 3;
    public static final int SHAPE_HEART = 4;
    public static final int SHAPE_STAR = 5;
    public static final int SHAPE_LINE = 6;

    public static final int ALIGN_LEFT = 0;
    public static final int ALIGN_CENTER = 1;
    public static final int ALIGN_RIGHT = 2;

    public static final class El {
        public int kind;
        public float x, y, w, h;      // top-left + size, page coords
        public float rotation = 0f;   // degrees
        public int alpha = 255;

        // photo
        public String uri;            // content:// or file path
        public float photoScale = 1f; // extra zoom of the image inside frame
        public float photoDx = 0f, photoDy = 0f; // pan of image inside frame (fraction)
        public float corner = 0f;     // corner radius (page px)
        public float borderW = 0f;
        public int borderColor = 0xFFFFFFFF;
        public int filter = 0;        // photo filter, see Renderer.FILTER_*

        // text
        public String text = "";
        public int textColor = 0xFF222222;
        public float textSize = 60f;
        public int font = 0;          // index into Assets fonts
        public boolean bold = false, italic = false, underline = false;
        public int align = ALIGN_CENTER;
        public int shadow = 0;        // 0 none, 1 soft
        public int bgBox = 0;         // background pill color, 0 = none
        public float letterSpacing = 0f;

        // shape
        public int shapeType = SHAPE_RECT;
        public int fillColor = 0xFFEC407A;
        public int strokeColor = 0x00000000;
        public float strokeW = 0f;

        // sticker
        public String emoji = "★";

        public El copy() {
            El e = new El();
            e.kind = kind; e.x = x + 24; e.y = y + 24; e.w = w; e.h = h;
            e.rotation = rotation; e.alpha = alpha;
            e.uri = uri; e.photoScale = photoScale; e.photoDx = photoDx; e.photoDy = photoDy;
            e.corner = corner; e.borderW = borderW; e.borderColor = borderColor; e.filter = filter;
            e.text = text; e.textColor = textColor; e.textSize = textSize; e.font = font;
            e.bold = bold; e.italic = italic; e.underline = underline; e.align = align;
            e.shadow = shadow; e.bgBox = bgBox; e.letterSpacing = letterSpacing;
            e.shapeType = shapeType; e.fillColor = fillColor; e.strokeColor = strokeColor; e.strokeW = strokeW;
            e.emoji = emoji;
            return e;
        }

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("k", kind); o.put("x", x); o.put("y", y); o.put("w", w); o.put("h", h);
            o.put("r", rotation); o.put("a", alpha);
            if (uri != null) o.put("uri", uri);
            o.put("ps", photoScale); o.put("pdx", photoDx); o.put("pdy", photoDy);
            o.put("cr", corner); o.put("bw", borderW); o.put("bc", borderColor); o.put("flt", filter);
            o.put("t", text); o.put("tc", textColor); o.put("ts", textSize); o.put("fn", font);
            o.put("b", bold); o.put("i", italic); o.put("u", underline); o.put("al", align);
            o.put("sh", shadow); o.put("bx", bgBox); o.put("ls", letterSpacing);
            o.put("st", shapeType); o.put("fc", fillColor); o.put("sc", strokeColor); o.put("sw", strokeW);
            o.put("em", emoji);
            return o;
        }

        static El fromJson(JSONObject o) {
            El e = new El();
            e.kind = o.optInt("k"); e.x = (float) o.optDouble("x"); e.y = (float) o.optDouble("y");
            e.w = (float) o.optDouble("w"); e.h = (float) o.optDouble("h");
            e.rotation = (float) o.optDouble("r"); e.alpha = o.optInt("a", 255);
            e.uri = o.has("uri") ? o.optString("uri") : null;
            e.photoScale = (float) o.optDouble("ps", 1); e.photoDx = (float) o.optDouble("pdx", 0);
            e.photoDy = (float) o.optDouble("pdy", 0);
            e.corner = (float) o.optDouble("cr", 0); e.borderW = (float) o.optDouble("bw", 0);
            e.borderColor = o.optInt("bc", 0xFFFFFFFF); e.filter = o.optInt("flt", 0);
            e.text = o.optString("t", ""); e.textColor = o.optInt("tc", 0xFF222222);
            e.textSize = (float) o.optDouble("ts", 60); e.font = o.optInt("fn", 0);
            e.bold = o.optBoolean("b"); e.italic = o.optBoolean("i"); e.underline = o.optBoolean("u");
            e.align = o.optInt("al", ALIGN_CENTER); e.shadow = o.optInt("sh", 0);
            e.bgBox = o.optInt("bx", 0); e.letterSpacing = (float) o.optDouble("ls", 0);
            e.shapeType = o.optInt("st", 0); e.fillColor = o.optInt("fc", 0xFFEC407A);
            e.strokeColor = o.optInt("sc", 0); e.strokeW = (float) o.optDouble("sw", 0);
            e.emoji = o.optString("em", "★");
            return e;
        }
    }

    /* ------------------------------ Page ------------------------------- */

    public static final int BG_SOLID = 0;
    public static final int BG_GRADIENT = 1;
    public static final int BG_PHOTO = 2;

    public static final class Page {
        public int bgType = BG_SOLID;
        public int bgColor = 0xFFFFFFFF;
        public int bgColor2 = 0xFFEDE7F6; // gradient end
        public int gradientAngle = 45;
        public String bgUri;              // for BG_PHOTO
        public final List<El> els = new ArrayList<>();

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("bt", bgType); o.put("bc", bgColor); o.put("bc2", bgColor2);
            o.put("ga", gradientAngle);
            if (bgUri != null) o.put("buri", bgUri);
            JSONArray arr = new JSONArray();
            for (El e : els) arr.put(e.toJson());
            o.put("els", arr);
            return o;
        }

        public Page duplicate() {
            try { return fromJson(toJson()); } catch (JSONException e) { return new Page(); }
        }

        static Page fromJson(JSONObject o) {
            Page p = new Page();
            p.bgType = o.optInt("bt"); p.bgColor = o.optInt("bc", 0xFFFFFFFF);
            p.bgColor2 = o.optInt("bc2", 0xFFEDE7F6); p.gradientAngle = o.optInt("ga", 45);
            p.bgUri = o.has("buri") ? o.optString("buri") : null;
            JSONArray arr = o.optJSONArray("els");
            if (arr != null) for (int i = 0; i < arr.length(); i++)
                p.els.add(El.fromJson(arr.optJSONObject(i)));
            return p;
        }
    }

    /* ----------------------------- Format ------------------------------ */

    public static final class Format {
        public final String name;
        public final int w, h;
        public Format(String n, int w, int h) { this.name = n; this.w = w; this.h = h; }
    }

    public static final Format[] FORMATS = {
            new Format("ריבוע 1:1", 1000, 1000),
            new Format("קלאסי 4:3", 1000, 750),
            new Format("פורטרט 3:4", 1000, 1333),
            new Format("אלבום כפול 2:1", 1500, 750),
            new Format("A4 לאורך", 1000, 1414),
            new Format("סטורי 9:16", 1000, 1778),
            new Format("פנורמה 16:9", 1600, 900),
    };

    /* ---------------------------- Project ------------------------------ */

    public static final class Project {
        public String id;
        public String name = "אלבום חדש";
        public int format = 0;
        public long modified;
        public final List<Page> pages = new ArrayList<>();

        public int pw() { return FORMATS[Math.max(0, Math.min(FORMATS.length - 1, format))].w; }
        public int ph() { return FORMATS[Math.max(0, Math.min(FORMATS.length - 1, format))].h; }

        public String toJson() {
            try {
                JSONObject o = new JSONObject();
                o.put("id", id); o.put("name", name); o.put("format", format);
                o.put("modified", modified);
                JSONArray arr = new JSONArray();
                for (Page p : pages) arr.put(p.toJson());
                o.put("pages", arr);
                return o.toString();
            } catch (JSONException e) { return "{}"; }
        }

        public static Project fromJson(String s) {
            try {
                JSONObject o = new JSONObject(s);
                Project p = new Project();
                p.id = o.optString("id");
                p.name = o.optString("name", "אלבום");
                p.format = o.optInt("format");
                p.modified = o.optLong("modified");
                JSONArray arr = o.optJSONArray("pages");
                if (arr != null) for (int i = 0; i < arr.length(); i++)
                    p.pages.add(Page.fromJson(arr.optJSONObject(i)));
                if (p.pages.isEmpty()) p.pages.add(new Page());
                return p;
            } catch (JSONException e) { return null; }
        }
    }

    private Model() {}
}
