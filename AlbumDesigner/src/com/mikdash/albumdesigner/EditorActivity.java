package com.mikdash.albumdesigner;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/** The full album editor: tools, templates, per-element properties and export. */
public class EditorActivity extends Activity implements EditorView.Listener {

    private Model.Project project;
    private int pageIndex = 0;
    private EditorView editor;
    private TextView pageLabel, titleLabel;
    private LinearLayout selectionBar;
    private final Deque<String> undo = new ArrayDeque<>();
    private String lastSnapshot;
    private Model.El pendingPhotoTarget; // element awaiting an image pick
    private static final int REQ_PICK = 101, REQ_BG = 102, REQ_PERM = 103;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String id = getIntent().getStringExtra("id");
        project = Storage.load(this, id);
        if (project == null) { finish(); return; }
        if (project.pages.isEmpty()) project.pages.add(new Model.Page());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFEDE7F6);

        root.addView(buildTopBar());

        editor = new EditorView(this);
        editor.setListener(this);
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        editor.setLayoutParams(elp);
        root.addView(editor);

        selectionBar = buildSelectionBar();
        selectionBar.setVisibility(View.GONE);
        root.addView(selectionBar);

        root.addView(buildToolbar());

        setContentView(root);
        editor.bind(project, project.pages.get(pageIndex));
        updatePageLabel();
    }

    /* ---------------------------- top bar ------------------------------ */

    private View buildTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(0xFF4A148C);
        int p = Ui.dp(this, 8);
        bar.setPadding(p, p, p, p);

        TextView back = navBtn("‹", new View.OnClickListener() {
            public void onClick(View v) { saveNow(); finish(); }
        });
        bar.addView(back);

        titleLabel = new TextView(this);
        titleLabel.setText(project.name);
        titleLabel.setTextColor(0xFFFFFFFF);
        titleLabel.setTextSize(16);
        titleLabel.setSingleLine(true);
        titleLabel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { renameProject(); }
        });
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMargins(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        bar.addView(titleLabel, tlp);

        bar.addView(navBtn("↶", new View.OnClickListener() {
            public void onClick(View v) { doUndo(); }
        }));
        bar.addView(navBtn("‹P", new View.OnClickListener() {
            public void onClick(View v) { gotoPage(pageIndex - 1); }
        }));
        pageLabel = new TextView(this);
        pageLabel.setTextColor(0xFFFFFFFF);
        pageLabel.setTextSize(13);
        pageLabel.setGravity(Gravity.CENTER);
        pageLabel.setPadding(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        pageLabel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pageMenu(); }
        });
        bar.addView(pageLabel);
        bar.addView(navBtn("P›", new View.OnClickListener() {
            public void onClick(View v) { gotoPage(pageIndex + 1); }
        }));
        bar.addView(navBtn("⤵", new View.OnClickListener() {
            public void onClick(View v) { exportMenu(); }
        }));
        return bar;
    }

    private TextView navBtn(String glyph, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(glyph);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(19);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        t.setOnClickListener(l);
        return t;
    }

    /* ---------------------------- toolbar ------------------------------ */

    private View buildToolbar() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setBackgroundColor(0xFFFFFFFF);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int p = Ui.dp(this, 4);
        row.setPadding(p, p, p, p);

        row.addView(tool("🖼", "תמונה", new Runnable() { public void run() { addPhoto(); } }));
        row.addView(tool("🅰", "טקסט", new Runnable() { public void run() { addText(); } }));
        row.addView(tool("🌸", "איורים", new Runnable() { public void run() { clipartDialog(); } }));
        row.addView(tool("⭐", "אמוג'י", new Runnable() { public void run() { stickerDialog(); } }));
        row.addView(tool("◼", "צורה", new Runnable() { public void run() { shapeDialog(); } }));
        row.addView(tool("🎨", "רקע", new Runnable() { public void run() { backgroundDialog(); } }));
        row.addView(tool("✨", "תבניות", new Runnable() { public void run() { themeDialog(); } }));
        row.addView(tool("▦", "פריסות", new Runnable() { public void run() { layoutDialog(); } }));
        row.addView(tool("📄", "עמודים", new Runnable() { public void run() { pageMenu(); } }));
        hs.addView(row);
        return hs;
    }

    private View tool(String glyph, String label, final Runnable r) {
        TextView t = Ui.iconButton(this, glyph, label, new View.OnClickListener() {
            public void onClick(View v) { r.run(); }
        });
        return t;
    }

    /* ------------------------- selection bar --------------------------- */

    private LinearLayout buildSelectionBar() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 4);
        row.setPadding(p, p, p, p);
        row.addView(tool("✎", "עריכה", new Runnable() { public void run() { editSelected(); } }));
        row.addView(tool("⧉", "שכפול", new Runnable() { public void run() { editor.duplicateSelected(); } }));
        row.addView(tool("↔", "מרכז אופקי", new Runnable() { public void run() { editor.centerH(); } }));
        row.addView(tool("↕", "מרכז אנכי", new Runnable() { public void run() { editor.centerV(); } }));
        row.addView(tool("◹", "שקיפות", new Runnable() { public void run() { alphaDialog(); } }));
        row.addView(tool("⤒", "קדימה", new Runnable() { public void run() { editor.beginChange(); editor.bringToFront(); } }));
        row.addView(tool("⤓", "אחורה", new Runnable() { public void run() { editor.beginChange(); editor.sendToBack(); } }));
        row.addView(tool("🗑", "מחיקה", new Runnable() { public void run() { editor.beginChange(); editor.deleteSelected(); } }));
        LinearLayout wrap = new LinearLayout(this);
        wrap.setBackgroundColor(0xFFF3E5F5);
        hs.addView(row);
        wrap.addView(hs, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return wrap;
    }

    /* --------------------------- listener ------------------------------ */

    @Override public void onSelectionChanged(Model.El sel) {
        selectionBar.setVisibility(sel == null ? View.GONE : View.VISIBLE);
    }

    @Override public void onEdited() { /* repaint handled by view; save on pause */ }

    @Override public void onBeginChange() { pushUndo(); }

    private void pushUndo() {
        String snap = project.toJson();
        if (snap.equals(lastSnapshot)) return;
        lastSnapshot = snap;
        undo.push(snap);
        while (undo.size() > 40) undo.removeLast();
    }

    private void doUndo() {
        if (undo.isEmpty()) { Toast.makeText(this, "אין מה לבטל", Toast.LENGTH_SHORT).show(); return; }
        String snap = undo.pop();
        Model.Project restored = Model.Project.fromJson(snap);
        if (restored == null) return;
        project = restored;
        lastSnapshot = null;
        if (pageIndex >= project.pages.size()) pageIndex = project.pages.size() - 1;
        editor.bind(project, project.pages.get(pageIndex));
        updatePageLabel();
    }

    /* ----------------------------- pages ------------------------------- */

    private void gotoPage(int i) {
        if (i < 0 || i >= project.pages.size()) return;
        pageIndex = i;
        editor.bind(project, project.pages.get(pageIndex));
        updatePageLabel();
    }

    private void updatePageLabel() {
        pageLabel.setText((pageIndex + 1) + "/" + project.pages.size());
    }

    private void pageMenu() {
        new AlertDialog.Builder(this).setTitle("עמודים")
                .setItems(new String[]{"➕ עמוד חדש", "⧉ שכפול עמוד", "🗑 מחיקת עמוד"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                pushUndo();
                                if (w == 0) { project.pages.add(new Model.Page()); pageIndex = project.pages.size() - 1; }
                                else if (w == 1) {
                                    Model.Page copy = project.pages.get(pageIndex).duplicate();
                                    project.pages.add(pageIndex + 1, copy); pageIndex++;
                                } else {
                                    if (project.pages.size() <= 1) { Toast.makeText(EditorActivity.this, "חייב להישאר עמוד אחד", Toast.LENGTH_SHORT).show(); return; }
                                    project.pages.remove(pageIndex);
                                    if (pageIndex >= project.pages.size()) pageIndex = project.pages.size() - 1;
                                }
                                editor.bind(project, project.pages.get(pageIndex));
                                updatePageLabel();
                            }
                        }).show();
    }

    /* ------------------------- add elements ---------------------------- */

    private void addPhoto() {
        Model.El sel = editor.getSelected();
        if (sel != null && sel.kind == Model.KIND_PHOTO) {
            pushUndo(); pendingPhotoTarget = sel; launchPicker(REQ_PICK); return;
        }
        pushUndo();
        Model.El e = new Model.El();
        e.kind = Model.KIND_PHOTO;
        int pw = project.pw(), ph = project.ph();
        e.w = pw * 0.6f; e.h = ph * 0.45f; e.x = (pw - e.w) / 2; e.y = (ph - e.h) / 2;
        e.corner = Math.min(e.w, e.h) * 0.04f;
        editor.addElement(e);
        pendingPhotoTarget = e;
        launchPicker(REQ_PICK);
    }

    private void launchPicker(int req) {
        Intent i;
        if (Build.VERSION.SDK_INT >= 19) {
            i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            i = new Intent(Intent.ACTION_GET_CONTENT);
        }
        i.setType("image/*");
        try { startActivityForResult(i, req); }
        catch (Exception e) { Toast.makeText(this, "לא נמצאה אפליקציית גלריה", Toast.LENGTH_SHORT).show(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (Build.VERSION.SDK_INT >= 19) {
            try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
            catch (Exception ignored) {}
        }
        String u = uri.toString();
        if (req == REQ_PICK && pendingPhotoTarget != null) {
            pendingPhotoTarget.uri = u;
            pendingPhotoTarget.photoScale = 1f; pendingPhotoTarget.photoDx = 0; pendingPhotoTarget.photoDy = 0;
            pendingPhotoTarget = null;
            editor.edited();
        } else if (req == REQ_BG) {
            Model.Page pg = editor.getPage();
            pg.bgType = Model.BG_PHOTO; pg.bgUri = u;
            editor.edited();
        }
    }

    private void addText() {
        pushUndo();
        final Model.El e = new Model.El();
        e.kind = Model.KIND_TEXT; e.text = "הקלד כאן";
        int pw = project.pw();
        e.w = pw * 0.8f; e.h = pw * 0.18f; e.x = pw * 0.1f;
        e.y = project.ph() * 0.4f; e.textSize = pw * 0.08f;
        editor.addElement(e);
        editText(e, true);
    }

    private void stickerDialog() {
        gridPicker("בחרו מדבקה", Palette.STICKERS, new IntConsumer() {
            public void accept(int i) {
                pushUndo();
                Model.El e = new Model.El();
                e.kind = Model.KIND_STICKER; e.emoji = Palette.STICKERS[i];
                int pw = project.pw();
                e.w = pw * 0.22f; e.h = pw * 0.22f; e.x = (pw - e.w) / 2; e.y = project.ph() * 0.4f;
                editor.addElement(e);
            }
        });
    }

    private void shapeDialog() {
        final String[] names = {"מלבן", "מלבן מעוגל", "עיגול", "משולש", "לב", "כוכב", "קו"};
        final int[] types = {Model.SHAPE_RECT, Model.SHAPE_ROUND, Model.SHAPE_CIRCLE,
                Model.SHAPE_TRIANGLE, Model.SHAPE_HEART, Model.SHAPE_STAR, Model.SHAPE_LINE};
        new AlertDialog.Builder(this).setTitle("בחרו צורה")
                .setItems(names, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        pushUndo();
                        Model.El e = new Model.El();
                        e.kind = Model.KIND_SHAPE; e.shapeType = types[w];
                        int pw = project.pw();
                        e.w = pw * 0.4f; e.h = w == 6 ? pw * 0.03f : pw * 0.4f;
                        e.x = (pw - e.w) / 2; e.y = project.ph() * 0.4f;
                        e.fillColor = 0xFFEC407A;
                        editor.addElement(e);
                    }
                }).show();
    }

    /* --------------------------- clipart ------------------------------- */

    // A small canvas-drawn preview tile for clipart / patterns / frames.
    private class Tile extends View {
        int mode; // 0 clip, 1 pattern, 2 frame
        int idx; int c1, c2;
        final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        Tile(Context ctx, int mode, int idx, int c1, int c2) {
            super(ctx); this.mode = mode; this.idx = idx; this.c1 = c1; this.c2 = c2;
        }
        protected void onDraw(Canvas cv) {
            int w = getWidth(), h = getHeight();
            float pad = w * 0.12f;
            if (mode == 0) {
                Clipart.draw(cv, idx, pad, pad, w - 2 * pad, h - 2 * pad, pt, c1, c2);
            } else if (mode == 1) {
                pt.setColor(0xFFFFFFFF); pt.setStyle(Paint.Style.FILL);
                cv.drawRect(0, 0, w, h, pt);
                cv.save(); cv.clipRect(0, 0, w, h);
                Patterns.draw(cv, idx, w, h, c1); cv.restore();
            } else {
                pt.setColor(0xFFBBDEFB); pt.setStyle(Paint.Style.FILL);
                float mi = Frames.matInset(idx) * Math.min(w, h) + w * 0.16f;
                RectF r = new RectF(mi, mi, w - mi, h - mi);
                cv.drawRect(r, pt);
                Frames.draw(cv, idx, r, w * 0.04f, c1, pt);
            }
        }
    }

    private View tile(int mode, int idx, int c1, int c2, int sizeDp, View.OnClickListener l) {
        Tile t = new Tile(this, mode, idx, c1, c2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, sizeDp), Ui.dp(this, sizeDp));
        lp.setMargins(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        t.setLayoutParams(lp);
        t.setBackground(Ui.roundBg(0xFFFAF6FF, 10, this));
        t.setOnClickListener(l);
        return t;
    }

    private int clipC1 = 0xFFEC407A, clipC2 = 0xFFFFFFFF;

    private void clipartDialog() {
        LinearLayout box = vbox();
        // colour chooser for new clipart
        addSectionTitle(box, "צבע ראשי");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() { public void accept(int c) { clipC1 = c; } }));
        addSectionTitle(box, "צבע משני");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() { public void accept(int c) { clipC2 = c; } }));
        for (Object[] cat : Clipart.CATEGORIES) {
            addSectionTitle(box, (String) cat[0]);
            int from = (Integer) cat[1], to = (Integer) cat[2];
            HorizontalScrollView hs = new HorizontalScrollView(this);
            LinearLayout rowl = new LinearLayout(this);
            for (int i = from; i < to; i++) {
                final int id = i;
                rowl.addView(tile(0, i, clipC1, clipC2, 62, new View.OnClickListener() {
                    public void onClick(View v) { addClip(id); dismissTop(); }
                }));
            }
            hs.addView(rowl);
            box.addView(hs);
        }
        showSheet("איורים וקטוריים", box);
    }

    private void addClip(int id) {
        pushUndo();
        Model.El e = new Model.El();
        e.kind = Model.KIND_CLIP; e.clipId = id;
        e.fillColor = clipC1; e.clipColor2 = clipC2;
        int pw = project.pw();
        e.w = pw * 0.28f; e.h = pw * 0.28f; e.x = (pw - e.w) / 2; e.y = project.ph() * 0.4f;
        editor.addElement(e);
    }

    private void editClip(final Model.El e) {
        pushUndo();
        LinearLayout box = vbox();
        addSectionTitle(box, "החלף איור");
        for (Object[] cat : Clipart.CATEGORIES) {
            int from = (Integer) cat[1], to = (Integer) cat[2];
            HorizontalScrollView hs = new HorizontalScrollView(this);
            LinearLayout rowl = new LinearLayout(this);
            for (int i = from; i < to; i++) {
                final int id = i;
                rowl.addView(tile(0, i, e.fillColor, e.clipColor2, 54, new View.OnClickListener() {
                    public void onClick(View v) { e.clipId = id; editor.edited(); }
                }));
            }
            hs.addView(rowl); box.addView(hs);
        }
        addSectionTitle(box, "צבע ראשי");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() { public void accept(int c) { e.fillColor = c; editor.edited(); } }));
        addSectionTitle(box, "צבע משני");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() { public void accept(int c) { e.clipColor2 = c; editor.edited(); } }));
        addSectionTitle(box, "אפקטים");
        LinearLayout cfx = new LinearLayout(this);
        cfx.addView(toggle("צל", e.dropShadow, new BoolConsumer() { public void accept(boolean b) { e.dropShadow = b; editor.edited(); } }));
        box.addView(cfx);
        showSheet("עריכת איור", box);
    }

    /* ------------------------ background / theme ----------------------- */

    private void backgroundDialog() {
        final Model.Page pg = editor.getPage();
        LinearLayout box = vbox();
        addSectionTitle(box, "צבע רקע");
        box.addView(swatchRow(Palette.PAGE_BG, new IntConsumer() {
            public void accept(int c) { pushUndo(); pg.bgType = Model.BG_SOLID; pg.bgColor = c; editor.edited(); }
        }));
        addSectionTitle(box, "גרדיאנט");
        LinearLayout gr = new LinearLayout(this);
        gr.setOrientation(LinearLayout.HORIZONTAL);
        HorizontalScrollView ghs = new HorizontalScrollView(this);
        for (int i = 0; i < Palette.GRADIENTS.length; i++) {
            final int[] g = Palette.GRADIENTS[i];
            View v = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46));
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            v.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{g[0], g[1]});
            d.setCornerRadius(Ui.dp(this, 8));
            v.setBackground(d);
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) { pushUndo(); pg.bgType = Model.BG_GRADIENT; pg.bgColor = g[0]; pg.bgColor2 = g[1]; pg.gradientAngle = g[2]; editor.edited(); }
            });
            gr.addView(v);
        }
        ghs.addView(gr);
        box.addView(ghs);
        addSectionTitle(box, "דפוסים");
        HorizontalScrollView phs = new HorizontalScrollView(this);
        LinearLayout prow = new LinearLayout(this);
        for (int i = 0; i < Patterns.NAMES.length; i++) {
            final int pi = i;
            prow.addView(tile(1, i, 0xFFB39DDB, 0, 54, new View.OnClickListener() {
                public void onClick(View v) {
                    pushUndo(); pg.bgType = Model.BG_PATTERN; pg.patternId = pi;
                    if (pg.bgColor == 0xFF000000) pg.bgColor = 0xFFFFFFFF;
                    editor.edited();
                }
            }));
        }
        phs.addView(prow); box.addView(phs);
        addSectionTitle(box, "צבע הדפוס");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() {
            public void accept(int c) { if (pg.bgType == Model.BG_PATTERN) { pushUndo(); pg.bgColor2 = c; editor.edited(); } }
        }));
        addSectionTitle(box, "רקע מעגלי (זוהר)");
        LinearLayout radial = new LinearLayout(this);
        for (int i = 0; i < Palette.GRADIENTS.length; i += 2) {
            final int[] gg = Palette.GRADIENTS[i];
            View v = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 46), Ui.dp(this, 46));
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0); v.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
            d.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            d.setGradientType(android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT);
            d.setColors(new int[]{gg[1], gg[0]}); d.setGradientRadius(Ui.dp(this, 30)); d.setCornerRadius(Ui.dp(this, 8));
            v.setBackground(d);
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) { pushUndo(); pg.bgType = Model.BG_RADIAL; pg.bgColor = gg[1]; pg.bgColor2 = gg[0]; editor.edited(); }
            });
            radial.addView(v);
        }
        HorizontalScrollView rhs = new HorizontalScrollView(this); rhs.addView(radial); box.addView(rhs);
        addSectionTitle(box, "אפקט תאורה");
        LinearLayout ov = new LinearLayout(this);
        ov.addView(Ui.pillButton(this, "ללא", 0xFFE1BEE7, 0xFF4A148C, new View.OnClickListener() {
            public void onClick(View v) { pushUndo(); pg.overlay = 0; editor.edited(); } }));
        ov.addView(sp());
        ov.addView(Ui.pillButton(this, "וינייטה", 0xFFE1BEE7, 0xFF4A148C, new View.OnClickListener() {
            public void onClick(View v) { pushUndo(); pg.overlay = 1; editor.edited(); } }));
        ov.addView(sp());
        ov.addView(Ui.pillButton(this, "זוהר עליון", 0xFFE1BEE7, 0xFF4A148C, new View.OnClickListener() {
            public void onClick(View v) { pushUndo(); pg.overlay = 2; editor.edited(); } }));
        box.addView(ov);
        addSectionTitle(box, "רקע מתמונה");
        box.addView(Ui.pillButton(this, "בחר תמונת רקע", 0xFF7B1FA2, 0xFFFFFFFF, new View.OnClickListener() {
            public void onClick(View v) { pushUndo(); launchPicker(REQ_BG); dismissTop(); }
        }));
        showSheet("רקע העמוד", box);
    }

    private interface BmpProvider { Bitmap get(int i); }

    private Bitmap renderPagePreview(Model.Page pg, int longEdge) {
        int pw = project.pw(), ph = project.ph();
        float s = (float) longEdge / Math.max(pw, ph);
        int w = Math.max(1, Math.round(pw * s)), h = Math.max(1, Math.round(ph * s));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.drawColor(0xFFFFFFFF);
        cv.scale((float) w / pw, (float) h / ph);
        new Renderer().drawPage(cv, pg, pw, ph, null);
        return bmp;
    }

    private void showPreviewGrid(String title, final int count, final int cols,
                                 final BmpProvider provider, final String[] labels, final IntConsumer onPick) {
        final android.widget.GridView grid = new android.widget.GridView(this);
        grid.setNumColumns(cols);
        grid.setVerticalSpacing(Ui.dp(this, 8));
        grid.setHorizontalSpacing(Ui.dp(this, 8));
        int pad = Ui.dp(this, 10);
        grid.setPadding(pad, pad, pad, pad);
        final Bitmap[] cache = new Bitmap[count];
        grid.setAdapter(new android.widget.BaseAdapter() {
            public int getCount() { return count; }
            public Object getItem(int i) { return i; }
            public long getItemId(int i) { return i; }
            public View getView(final int i, View cv, ViewGroup parent) {
                LinearLayout cell = new LinearLayout(EditorActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                ImageView iv = new ImageView(EditorActivity.this);
                iv.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(EditorActivity.this, 150)));
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                iv.setBackground(Ui.roundBg(0xFFF3EEF9, 8, EditorActivity.this));
                if (cache[i] == null) try { cache[i] = provider.get(i); } catch (Throwable ignored) {}
                iv.setImageBitmap(cache[i]);
                cell.addView(iv);
                if (labels != null) {
                    TextView t = new TextView(EditorActivity.this);
                    t.setText(labels[i]); t.setTextSize(11); t.setGravity(Gravity.CENTER);
                    t.setTextColor(0xFF555555); t.setPadding(0, Ui.dp(EditorActivity.this, 3), 0, 0);
                    cell.addView(t);
                }
                return cell;
            }
        });
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(title).setView(grid)
                .setNegativeButton("סגור", null).create();
        grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> pr, View v, int pos, long id) {
                onPick.accept(pos); dlg.dismiss();
            }
        });
        dlg.show();
    }

    private void themeDialog() {
        showPreviewGrid("תבניות עיצוב מוכנות", Templates.THEME_NAMES.length, 2,
                new BmpProvider() { public Bitmap get(int i) { return renderPagePreview(Templates.theme(i, project.pw(), project.ph()), 320); } },
                Templates.THEME_NAMES,
                new IntConsumer() { public void accept(int i) {
                    pushUndo();
                    Model.Page pg = Templates.theme(i, project.pw(), project.ph());
                    project.pages.set(pageIndex, pg);
                    editor.bind(project, pg);
                    Toast.makeText(EditorActivity.this, "הוחלה תבנית · הוסיפו תמונות במסגרות", Toast.LENGTH_SHORT).show();
                } });
    }

    private void layoutDialog() {
        showPreviewGrid("פריסת תמונות", Templates.LAYOUT_NAMES.length, 3,
                new BmpProvider() { public Bitmap get(int i) {
                    Model.Page pg = new Model.Page(); pg.bgColor = 0xFFF0F0F0;
                    pg.els.addAll(Templates.layout(i, project.pw(), project.ph()));
                    return renderPagePreview(pg, 300);
                } },
                Templates.LAYOUT_NAMES,
                new IntConsumer() { public void accept(int w) {
                    pushUndo();
                    Model.Page pg = editor.getPage();
                    for (int i = pg.els.size() - 1; i >= 0; i--) {
                        Model.El e = pg.els.get(i);
                        if (e.kind == Model.KIND_PHOTO && e.uri == null) pg.els.remove(i);
                    }
                    pg.els.addAll(0, Templates.layout(w, project.pw(), project.ph()));
                    editor.bind(project, pg);
                } });
    }

    /* --------------------- per-element editing ------------------------- */

    private void editSelected() {
        Model.El e = editor.getSelected();
        if (e == null) return;
        switch (e.kind) {
            case Model.KIND_TEXT: editText(e, false); break;
            case Model.KIND_PHOTO: editPhoto(e); break;
            case Model.KIND_SHAPE: editShape(e); break;
            case Model.KIND_STICKER: editSticker(e); break;
            case Model.KIND_CLIP: editClip(e); break;
        }
    }

    private void editText(final Model.El e, boolean isNew) {
        pushUndo();
        LinearLayout box = vbox();
        final EditText et = new EditText(this);
        et.setText(e.text);
        et.setHint("טקסט");
        box.addView(et);
        addSectionTitle(box, "צבע");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() {
            public void accept(int c) { e.textColor = c; editor.edited(); }
        }));
        addSectionTitle(box, "גופן");
        LinearLayout fonts = new LinearLayout(this);
        for (int i = 0; i < Palette.FONT_NAMES.length; i++) {
            final int fi = i;
            TextView t = Ui.pillButton(this, Palette.FONT_NAMES[i], 0xFFE1BEE7, 0xFF4A148C, new View.OnClickListener() {
                public void onClick(View v) { e.font = fi; editor.edited(); }
            });
            t.setTypeface(Palette.font(i, false, false));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            fonts.addView(t, lp);
        }
        HorizontalScrollView fhs = new HorizontalScrollView(this); fhs.addView(fonts);
        box.addView(fhs);
        addSectionTitle(box, "גודל");
        box.addView(slider(20, 260, (int) e.textSize, new IntConsumer() {
            public void accept(int v) { e.textSize = v; editor.edited(); }
        }));
        LinearLayout style = new LinearLayout(this);
        style.addView(toggle("B", e.bold, new BoolConsumer() { public void accept(boolean b) { e.bold = b; editor.edited(); } }));
        style.addView(toggle("I", e.italic, new BoolConsumer() { public void accept(boolean b) { e.italic = b; editor.edited(); } }));
        style.addView(toggle("U", e.underline, new BoolConsumer() { public void accept(boolean b) { e.underline = b; editor.edited(); } }));
        style.addView(toggle("צל", e.shadow == 1, new BoolConsumer() { public void accept(boolean b) { e.shadow = b ? 1 : 0; editor.edited(); } }));
        box.addView(style);
        addSectionTitle(box, "יישור");
        LinearLayout al = new LinearLayout(this);
        al.addView(Ui.pillButton(this, "ימין", 0xFFE1BEE7, 0xFF4A148C, alignClick(e, Model.ALIGN_RIGHT)));
        al.addView(sp());
        al.addView(Ui.pillButton(this, "מרכז", 0xFFE1BEE7, 0xFF4A148C, alignClick(e, Model.ALIGN_CENTER)));
        al.addView(sp());
        al.addView(Ui.pillButton(this, "שמאל", 0xFFE1BEE7, 0xFF4A148C, alignClick(e, Model.ALIGN_LEFT)));
        box.addView(al);

        addSectionTitle(box, "מילוי גרדיאנט");
        LinearLayout grad = new LinearLayout(this);
        grad.addView(toggle("גרדיאנט", e.textGrad, new BoolConsumer() { public void accept(boolean b) { e.textGrad = b; editor.edited(); } }));
        box.addView(grad);
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() { public void accept(int c) { e.textColor2 = c; e.textGrad = true; editor.edited(); } }));
        addSectionTitle(box, "קו מתאר (עובי)");
        box.addView(slider(0, 40, (int) e.outlineW, new IntConsumer() { public void accept(int v) { e.outlineW = v; editor.edited(); } }));
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() { public void accept(int c) { e.outlineColor = c; if (e.outlineW == 0) e.outlineW = 8; editor.edited(); } }));
        addSectionTitle(box, "עיקול הטקסט (קשת)");
        box.addView(slider(-200, 200, (int) e.curve, new IntConsumer() { public void accept(int v) { e.curve = v; editor.edited(); } }));
        addSectionTitle(box, "אפקטים");
        LinearLayout fx = new LinearLayout(this);
        fx.addView(toggle("צל", e.dropShadow, new BoolConsumer() { public void accept(boolean b) { e.dropShadow = b; editor.edited(); } }));
        box.addView(fx);

        new AlertDialog.Builder(this).setTitle("עריכת טקסט · Word-Art")
                .setView(wrapScroll(box))
                .setPositiveButton("אישור", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { e.text = et.getText().toString(); editor.edited(); }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface d) { e.text = et.getText().toString(); editor.edited(); }
                }).show();
    }

    private View.OnClickListener alignClick(final Model.El e, final int a) {
        return new View.OnClickListener() { public void onClick(View v) { e.align = a; editor.edited(); } };
    }

    private void editPhoto(final Model.El e) {
        pushUndo();
        LinearLayout box = vbox();
        box.addView(Ui.pillButton(this, "החלף תמונה", 0xFF7B1FA2, 0xFFFFFFFF, new View.OnClickListener() {
            public void onClick(View v) { pendingPhotoTarget = e; launchPicker(REQ_PICK); dismissTop(); }
        }));
        addSectionTitle(box, "פילטר");
        LinearLayout filters = new LinearLayout(this);
        for (int i = 0; i < Renderer.FILTER_NAMES.length; i++) {
            final int fi = i;
            TextView t = Ui.pillButton(this, Renderer.FILTER_NAMES[i], 0xFFE1BEE7, 0xFF4A148C,
                    new View.OnClickListener() { public void onClick(View v) { e.filter = fi; editor.edited(); } });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            filters.addView(t, lp);
        }
        HorizontalScrollView fhs = new HorizontalScrollView(this); fhs.addView(filters);
        box.addView(fhs);
        addSectionTitle(box, "מסגרת דקורטיבית");
        HorizontalScrollView frs = new HorizontalScrollView(this);
        LinearLayout frames = new LinearLayout(this);
        for (int i = 0; i < Frames.NAMES.length; i++) {
            final int fi = i;
            int fc = e.borderColor == 0xFFFFFFFF ? 0xFF555555 : e.borderColor;
            LinearLayout cell = new LinearLayout(this); cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.addView(tile(2, i, fc, 0, 58, new View.OnClickListener() {
                public void onClick(View v) {
                    e.frameStyle = fi;
                    if (e.borderColor == 0xFFFFFFFF && fi != 6 && fi != 7 && fi != 11) e.borderColor = 0xFF555555;
                    editor.edited();
                }
            }));
            TextView lbl = new TextView(this); lbl.setText(Frames.NAMES[i]); lbl.setTextSize(10);
            lbl.setGravity(Gravity.CENTER); lbl.setTextColor(0xFF666666);
            cell.addView(lbl);
            frames.addView(cell);
        }
        frs.addView(frames); box.addView(frs);
        addSectionTitle(box, "עיגול פינות");
        box.addView(slider(0, (int) (Math.min(e.w, e.h) / 2), (int) e.corner, new IntConsumer() {
            public void accept(int v) { e.corner = v; editor.edited(); }
        }));
        addSectionTitle(box, "עובי מסגרת");
        box.addView(slider(0, 80, (int) e.borderW, new IntConsumer() {
            public void accept(int v) { e.borderW = v; editor.edited(); }
        }));
        addSectionTitle(box, "צבע מסגרת");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() {
            public void accept(int c) { e.borderColor = c; editor.edited(); }
        }));
        addSectionTitle(box, "זום תמונה");
        box.addView(slider(100, 300, (int) (e.photoScale * 100), new IntConsumer() {
            public void accept(int v) { e.photoScale = v / 100f; editor.edited(); }
        }));
        addSectionTitle(box, "בהירות");
        box.addView(slider(-100, 100, (int) e.bright, new IntConsumer() { public void accept(int v) { e.bright = v; editor.edited(); } }));
        addSectionTitle(box, "ניגודיות");
        box.addView(slider(40, 220, (int) (e.contrast * 100), new IntConsumer() { public void accept(int v) { e.contrast = v / 100f; editor.edited(); } }));
        addSectionTitle(box, "רוויה");
        box.addView(slider(0, 200, (int) (e.saturation * 100), new IntConsumer() { public void accept(int v) { e.saturation = v / 100f; editor.edited(); } }));
        addSectionTitle(box, "אפקטים");
        LinearLayout pfx = new LinearLayout(this);
        pfx.addView(toggle("צל מוטל", e.dropShadow, new BoolConsumer() { public void accept(boolean b) { e.dropShadow = b; editor.edited(); } }));
        box.addView(pfx);
        showSheet("עריכת תמונה", box);
    }

    private void editShape(final Model.El e) {
        pushUndo();
        LinearLayout box = vbox();
        addSectionTitle(box, "צבע מילוי");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() {
            public void accept(int c) { e.fillColor = c; editor.edited(); }
        }));
        addSectionTitle(box, "צבע מסגרת");
        box.addView(swatchRow(Palette.COLORS, new IntConsumer() {
            public void accept(int c) { e.strokeColor = c; if (e.strokeW == 0) e.strokeW = 8; editor.edited(); }
        }));
        addSectionTitle(box, "עובי מסגרת");
        box.addView(slider(0, 60, (int) e.strokeW, new IntConsumer() {
            public void accept(int v) { e.strokeW = v; editor.edited(); }
        }));
        addSectionTitle(box, "אפקטים");
        LinearLayout sfx = new LinearLayout(this);
        sfx.addView(toggle("צל", e.dropShadow, new BoolConsumer() { public void accept(boolean b) { e.dropShadow = b; editor.edited(); } }));
        box.addView(sfx);
        showSheet("עריכת צורה", box);
    }

    private void editSticker(final Model.El e) {
        gridPicker("החלף מדבקה", Palette.STICKERS, new IntConsumer() {
            public void accept(int i) { pushUndo(); e.emoji = Palette.STICKERS[i]; editor.edited(); }
        });
    }

    private void alphaDialog() {
        final Model.El e = editor.getSelected();
        if (e == null) return;
        pushUndo();
        LinearLayout box = vbox();
        addSectionTitle(box, "שקיפות");
        box.addView(slider(20, 255, e.alpha, new IntConsumer() {
            public void accept(int v) { e.alpha = v; editor.edited(); }
        }));
        showSheet("שקיפות", box);
    }

    /* ---------------------------- export ------------------------------- */

    private void exportMenu() {
        saveNow();
        new AlertDialog.Builder(this).setTitle("ייצוא ושיתוף")
                .setItems(new String[]{"💾 שמור תמונות לגלריה", "📄 ייצוא PDF", "📤 שתף PDF"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                if (w == 0) exportImages();
                                else if (w == 1) exportPdf(false);
                                else exportPdf(true);
                            }
                        }).show();
    }

    private void exportImages() {
        if (Build.VERSION.SDK_INT < 29 &&
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERM);
            return;
        }
        final ProgressDialog pd = ProgressDialog.show(this, "", "מייצא תמונות…", true);
        new AsyncTask<Void, Void, String>() {
            protected String doInBackground(Void... v) {
                try { Exporter.saveAllJpeg(EditorActivity.this, project, editor.getImages()); return null; }
                catch (Throwable t) { return t.getMessage(); }
            }
            protected void onPostExecute(String err) {
                pd.dismiss();
                Toast.makeText(EditorActivity.this, err == null
                        ? "נשמר בגלריה › Pictures/AlbumDesigner" : "שגיאה: " + err, Toast.LENGTH_LONG).show();
            }
        }.execute();
    }

    private void exportPdf(final boolean share) {
        final ProgressDialog pd = ProgressDialog.show(this, "", "יוצר PDF…", true);
        new AsyncTask<Void, Void, Object>() {
            protected Object doInBackground(Void... v) {
                try { return Exporter.exportPdf(EditorActivity.this, project, editor.getImages()); }
                catch (Throwable t) { return t; }
            }
            protected void onPostExecute(Object o) {
                pd.dismiss();
                if (o instanceof File) {
                    File f = (File) o;
                    if (share) {
                        Uri u = AlbumFileProvider.uriFor(f);
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("application/pdf");
                        i.putExtra(Intent.EXTRA_STREAM, u);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(Intent.createChooser(i, "שיתוף אלבום"));
                    } else {
                        Toast.makeText(EditorActivity.this, "PDF נשמר: " + f.getName(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(EditorActivity.this, "שגיאה ביצירת PDF", Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] res) {
        if (req == REQ_PERM && res.length > 0 && res[0] == android.content.pm.PackageManager.PERMISSION_GRANTED)
            exportImages();
        else if (req == REQ_PERM)
            Toast.makeText(this, "נדרשת הרשאה כדי לשמור לגלריה", Toast.LENGTH_LONG).show();
    }

    /* --------------------------- helpers ------------------------------- */

    private void renameProject() {
        final EditText et = new EditText(this);
        et.setText(project.name);
        new AlertDialog.Builder(this).setTitle("שם האלבום").setView(et)
                .setPositiveButton("שמור", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        project.name = et.getText().toString();
                        titleLabel.setText(project.name);
                    }
                }).setNegativeButton("ביטול", null).show();
    }

    private interface IntConsumer { void accept(int v); }
    private interface BoolConsumer { void accept(boolean b); }

    private LinearLayout vbox() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 12);
        l.setPadding(p, p, p, p);
        return l;
    }

    private View sp() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 6), 1));
        return v;
    }

    private android.widget.ScrollView wrapScroll(View v) {
        android.widget.ScrollView s = new android.widget.ScrollView(this);
        s.addView(v);
        return s;
    }

    private void addSectionTitle(LinearLayout box, String t) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(0xFF7B1FA2);
        tv.setTextSize(13);
        tv.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        box.addView(tv);
    }

    private View swatchRow(int[] colors, final IntConsumer cb) {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        LinearLayout row = new LinearLayout(this);
        for (final int c : colors) {
            row.addView(Ui.swatch(this, c, 38, new View.OnClickListener() {
                public void onClick(View v) { cb.accept(c); }
            }));
        }
        hs.addView(row);
        return hs;
    }

    private View slider(int min, int max, int val, final IntConsumer cb) {
        final SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgress(Math.max(0, Math.min(max - min, val - min)));
        final int fmin = min;
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { cb.accept(p + fmin); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        return sb;
    }

    private View toggle(String label, boolean on, final BoolConsumer cb) {
        final boolean[] state = {on};
        final TextView t = new TextView(this);
        t.setText(label);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(16);
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        Runnable paint = new Runnable() {
            public void run() {
                t.setBackground(Ui.roundBg(state[0] ? 0xFF7B1FA2 : 0xFFE1BEE7, 20, EditorActivity.this));
                t.setTextColor(state[0] ? 0xFFFFFFFF : 0xFF4A148C);
            }
        };
        paint.run();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        t.setLayoutParams(lp);
        t.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { state[0] = !state[0]; paint.run(); cb.accept(state[0]); }
        });
        return t;
    }

    private void gridPicker(String title, final String[] items, final IntConsumer cb) {
        android.widget.GridView grid = new android.widget.GridView(this);
        grid.setNumColumns(6);
        grid.setAdapter(new android.widget.BaseAdapter() {
            public int getCount() { return items.length; }
            public Object getItem(int i) { return items[i]; }
            public long getItemId(int i) { return i; }
            public View getView(int i, View cv, ViewGroup parent) {
                TextView t = new TextView(EditorActivity.this);
                t.setText(items[i]);
                t.setTextSize(28);
                t.setGravity(Gravity.CENTER);
                t.setPadding(0, Ui.dp(EditorActivity.this, 10), 0, Ui.dp(EditorActivity.this, 10));
                return t;
            }
        });
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle(title).setView(grid).create();
        grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            public void onItemClick(android.widget.AdapterView<?> p, View v, int pos, long id) {
                cb.accept(pos); dlg.dismiss();
            }
        });
        dlg.show();
        topDialog = dlg;
    }

    private AlertDialog topDialog;

    private void showSheet(String title, View content) {
        topDialog = new AlertDialog.Builder(this).setTitle(title)
                .setView(wrapScroll(content))
                .setPositiveButton("סגור", null).create();
        topDialog.show();
    }

    private void showSheetNoScroll(String title, View content) {
        topDialog = new AlertDialog.Builder(this).setTitle(title).setView(content)
                .setPositiveButton("סגור", null).create();
        topDialog.show();
    }

    private void dismissTop() { if (topDialog != null) topDialog.dismiss(); }

    private void saveNow() {
        Storage.save(this, project);
        try {
            Bitmap thumb = Exporter.thumb(project, project.pages.get(0), editor.getImages(), 300);
            Storage.saveThumb(this, project.id, thumb);
        } catch (Throwable ignored) {}
    }

    @Override protected void onPause() { super.onPause(); if (project != null) saveNow(); }
}
