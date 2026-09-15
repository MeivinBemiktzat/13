package com.mikdash.albumdesigner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.List;

/** Home screen: create albums, pick a format, and reopen saved projects. */
public class MainActivity extends Activity {

    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF5F0FA);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        // header
        TextView title = new TextView(this);
        title.setText("עיצוב אלבומים");
        title.setTextColor(0xFF4A148C);
        title.setTextSize(30);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("סטודיו מקצועי לאלבומים דיגיטליים · אופליין לחלוטין · חינם");
        sub.setTextColor(0xFF7E57C2);
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 18));
        root.addView(sub);

        TextView create = Ui.pillButton(this, "＋  אלבום חדש", 0xFFEC407A, 0xFFFFFFFF,
                new View.OnClickListener() { public void onClick(View v) { chooseFormat(); } });
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        create.setTextSize(18);
        create.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        root.addView(create, cp);

        TextView saved = new TextView(this);
        saved.setText("האלבומים שלי");
        saved.setTextColor(0xFF4A148C);
        saved.setTextSize(19);
        saved.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 8));
        root.addView(saved);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        listContainer.removeAllViews();
        List<Model.Project> projects = Storage.list(this);
        if (projects.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("עדיין אין אלבומים.\nלחצו על \"אלבום חדש\" כדי להתחיל ליצור! 🎨");
            empty.setTextColor(0xFF9E9E9E);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, Ui.dp(this, 40), 0, 0);
            listContainer.addView(empty);
            return;
        }
        for (final Model.Project p : projects) listContainer.addView(projectCard(p));
    }

    private View projectCard(final Model.Project p) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(Ui.roundBg(0xFFFFFFFF, 16, this));
        card.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        card.setLayoutParams(lp);

        ImageView thumb = new ImageView(this);
        thumb.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 72), Ui.dp(this, 72)));
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setBackground(Ui.roundBg(0xFFECECEC, 10, this));
        thumb.setClipToOutline(true);
        File tf = Storage.thumbFile(this, p.id);
        if (tf.exists()) thumb.setImageBitmap(BitmapFactory.decodeFile(tf.getAbsolutePath()));
        else thumb.setImageResource(android.R.drawable.ic_menu_gallery);
        card.addView(thumb);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ip.setMargins(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        info.setLayoutParams(ip);
        TextView name = new TextView(this);
        name.setText(p.name);
        name.setTextColor(0xFF212121);
        name.setTextSize(17);
        info.addView(name);
        TextView meta = new TextView(this);
        meta.setText(p.pages.size() + " עמודים · " + Model.FORMATS[p.format].name);
        meta.setTextColor(0xFF9E9E9E);
        meta.setTextSize(12);
        info.addView(meta);
        card.addView(info);

        TextView menu = new TextView(this);
        menu.setText("⋮");
        menu.setTextSize(24);
        menu.setTextColor(0xFF757575);
        menu.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        menu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { projectMenu(p); }
        });
        card.addView(menu);

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { open(p.id); }
        });
        return card;
    }

    private void projectMenu(final Model.Project p) {
        new AlertDialog.Builder(this)
                .setTitle(p.name)
                .setItems(new String[]{"פתיחה", "שכפול", "שינוי שם", "מחיקה"},
                        new android.content.DialogInterface.OnClickListener() {
                            public void onClick(android.content.DialogInterface d, int w) {
                                switch (w) {
                                    case 0: open(p.id); break;
                                    case 1: duplicate(p); break;
                                    case 2: rename(p); break;
                                    case 3: confirmDelete(p); break;
                                }
                            }
                        }).show();
    }

    private void duplicate(Model.Project p) {
        Model.Project full = Storage.load(this, p.id);
        if (full == null) return;
        full.id = Storage.newId();
        full.name = p.name + " (עותק)";
        Storage.save(this, full);
        File tf = Storage.thumbFile(this, p.id);
        if (tf.exists()) {
            try {
                java.nio.file.Files.copy(tf.toPath(),
                        Storage.thumbFile(this, full.id).toPath());
            } catch (Exception ignored) {}
        }
        refresh();
    }

    private void rename(final Model.Project p) {
        final EditText et = new EditText(this);
        et.setText(p.name);
        new AlertDialog.Builder(this).setTitle("שינוי שם").setView(et)
                .setPositiveButton("שמירה", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        Model.Project full = Storage.load(MainActivity.this, p.id);
                        if (full != null) { full.name = et.getText().toString(); Storage.save(MainActivity.this, full); refresh(); }
                    }
                }).setNegativeButton("ביטול", null).show();
    }

    private void confirmDelete(final Model.Project p) {
        new AlertDialog.Builder(this).setTitle("מחיקת אלבום")
                .setMessage("למחוק את \"" + p.name + "\"? הפעולה אינה הפיכה.")
                .setPositiveButton("מחק", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        Storage.delete(MainActivity.this, p.id); refresh();
                    }
                }).setNegativeButton("ביטול", null).show();
    }

    private void chooseFormat() {
        String[] names = new String[Model.FORMATS.length];
        for (int i = 0; i < names.length; i++) names[i] = Model.FORMATS[i].name;
        new AlertDialog.Builder(this).setTitle("בחרו פורמט לאלבום")
                .setItems(names, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) { createNew(w); }
                }).show();
    }

    private void createNew(int format) {
        Model.Project p = new Model.Project();
        p.id = Storage.newId();
        p.format = format;
        p.name = "אלבום " + (Storage.list(this).size() + 1);
        p.pages.add(new Model.Page());
        Storage.save(this, p);
        open(p.id);
    }

    private void open(String id) {
        Intent i = new Intent(this, EditorActivity.class);
        i.putExtra("id", id);
        startActivity(i);
    }
}
