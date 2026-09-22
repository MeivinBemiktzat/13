package com.prisma.match3;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.prisma.match3.engine.Board;
import com.prisma.match3.engine.GameListener;
import com.prisma.match3.engine.GemType;
import com.prisma.match3.engine.Level;
import com.prisma.match3.engine.LevelFactory;
import com.prisma.match3.engine.Objective;
import com.prisma.match3.engine.Special;
import com.prisma.match3.render.GameRenderer;
import com.prisma.match3.render.GameView;
import com.prisma.match3.save.Achievement;
import com.prisma.match3.save.Progress;
import com.prisma.match3.ui.IconView;
import com.prisma.match3.ui.Sfx;
import com.prisma.match3.ui.Ui;

import java.util.List;

/**
 * Single-activity host for Prisma. Owns the persistent GL surface (a live 3D
 * backdrop that also renders the board in-game) and swaps lightweight,
 * code-built screens over it: main menu, level map, gameplay HUD, achievements
 * and win/lose dialogs. Implements the engine's {@link GameListener} to drive
 * particles, sound and HUD refreshes.
 */
public class MainActivity extends Activity implements GameListener {

    private enum Screen { MENU, MAP, GAME, ACHIEVEMENTS }

    private FrameLayout root, overlay;
    private GameView glView;
    private GameRenderer renderer;
    private Progress progress;
    private Sfx sfx;

    private Board board;
    private int currentLevel = 1;
    private volatile int clearedThisLevel;

    // HUD references
    private TextView hudScore, hudMoves, hudLevel, comboText, coinText;
    private LinearLayout objectiveRow;

    // ------------------------------------------------------------ lifecycle
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        progress = new Progress(this);
        sfx = new Sfx();
        sfx.setEnabled(progress.soundOn());

        renderer = new GameRenderer();
        glView = new GameView(this, renderer);
        glView.setTapListener(new GameView.TapListener() {
            @Override public void onTap(int r, int c) { onBoardTap(r, c); }
        });

        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG0);
        root.addView(glView, new FrameLayout.LayoutParams(-1, -1));

        overlay = new FrameLayout(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
        showMenu();
    }

    @Override protected void onResume() { super.onResume(); glView.onResume(); }
    @Override protected void onPause() { super.onPause(); glView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); if (sfx != null) sfx.release(); }

    @Override public void onBackPressed() {
        if (overlayScreen == Screen.GAME) { confirmQuitToMap(); }
        else if (overlayScreen == Screen.MAP || overlayScreen == Screen.ACHIEVEMENTS) { showMenu(); }
        else super.onBackPressed();
    }

    private Screen overlayScreen = Screen.MENU;

    private void setScreen(Screen s, View content) {
        overlayScreen = s;
        overlay.removeAllViews();
        overlay.addView(content, new FrameLayout.LayoutParams(-1, -1));
    }

    // ------------------------------------------------------------ menu
    private void showMenu() {
        renderer.setBoard(null);
        glView.setBoard(null);
        board = null;

        LinearLayout col = vertical();
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 24);
        col.setPadding(pad, Ui.dp(this, 48), pad, pad);

        col.addView(header());

        View spacerTop = new View(this);
        col.addView(spacerTop, lp(-1, 0, 1f));

        TextView title = Ui.label(this, "PRISMA", 64, Ui.TEXT, true);
        title.setLetterSpacing(0.08f);
        title.setShadowLayer(24, 0, 0, Ui.ACCENT);
        col.addView(title);

        TextView sub = Ui.label(this, "3D MATCH ODYSSEY", 15, Ui.ACCENT, true);
        sub.setLetterSpacing(0.35f);
        col.addView(sub, marginTop(6));

        col.addView(spacer(28));

        int cont = progress.unlockedLevel();
        col.addView(bigButton(IconView.Glyph.PLAY,
                cont > 1 ? "CONTINUE  ·  LEVEL " + cont : "PLAY",
                Ui.ACCENT, Ui.ACCENT2, new Runnable() {
            @Override public void run() { sfx.click(); startLevel(progress.unlockedLevel()); }
        }));
        col.addView(spacer(14));
        col.addView(menuButton(IconView.Glyph.GEM, "LEVEL MAP", new Runnable() {
            @Override public void run() { sfx.click(); showMap(); }
        }));
        col.addView(spacer(12));
        col.addView(menuButton(IconView.Glyph.TROPHY, "ACHIEVEMENTS", new Runnable() {
            @Override public void run() { sfx.click(); showAchievements(); }
        }));

        View spacerBot = new View(this);
        col.addView(spacerBot, lp(-1, 0, 1.4f));

        TextView foot = Ui.label(this, "Plays fully offline", 12, Ui.TEXT_DIM, false);
        col.addView(foot);

        setScreen(Screen.MENU, col);
    }

    /** Top status strip: coins, total stars, sound toggle. */
    private View header() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(Ui.roundRect(Ui.PANEL, 22, this));
        int p = Ui.dp(this, 12);
        bar.setPadding(p, Ui.dp(this, 8), p, Ui.dp(this, 8));

        bar.addView(icon(IconView.Glyph.COIN, Ui.GOLD, 22));
        coinText = Ui.label(this, " " + progress.coins() + "   ", 16, Ui.TEXT, true);
        bar.addView(coinText);
        bar.addView(icon(IconView.Glyph.STAR, Ui.GOLD, 22));
        bar.addView(Ui.label(this, " " + progress.totalStars(), 16, Ui.TEXT, true));

        bar.addView(new View(this), lp(0, -1, 1f));

        final IconView snd = new IconView(this,
                progress.soundOn() ? IconView.Glyph.SOUND_ON : IconView.Glyph.SOUND_OFF, Ui.TEXT);
        int is = Ui.dp(this, 30);
        snd.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean on = !progress.soundOn();
                progress.setSound(on);
                sfx.setEnabled(on);
                snd.setGlyph(on ? IconView.Glyph.SOUND_ON : IconView.Glyph.SOUND_OFF);
                sfx.click();
            }
        });
        bar.addView(snd, new LinearLayout.LayoutParams(is, is));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        bar.setLayoutParams(lp);
        return bar;
    }

    // ------------------------------------------------------------ level map
    private void showMap() {
        LinearLayout col = vertical();
        int pad = Ui.dp(this, 18);
        col.setPadding(pad, Ui.dp(this, 20), pad, pad);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(iconButton(IconView.Glyph.BACK, new Runnable() {
            @Override public void run() { sfx.click(); showMenu(); }
        }));
        top.addView(Ui.label(this, "  LEVEL MAP", 22, Ui.TEXT, true));
        col.addView(top);
        col.addView(spacer(12));

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout list = vertical();
        int perRow = 5;
        int unlocked = progress.unlockedLevel();
        LinearLayout rowl = null;
        for (int i = 1; i <= LevelFactory.TOTAL; i++) {
            if ((i - 1) % perRow == 0) {
                rowl = new LinearLayout(this);
                rowl.setOrientation(LinearLayout.HORIZONTAL);
                list.addView(rowl, marginTop(8));
            }
            rowl.addView(levelCell(i, i <= unlocked), cellLp());
        }
        sv.addView(list);
        col.addView(sv);
        setScreen(Screen.MAP, col);
    }

    private View levelCell(final int id, boolean unlocked) {
        FrameLayout cell = new FrameLayout(this);
        int c0 = unlocked ? Ui.PANEL2 : Ui.darken(Ui.PANEL2, 0.6f);
        cell.setBackground(Ui.stroked(c0, unlocked ? Ui.ACCENT : Ui.alpha(Ui.TEXT_DIM, 60), 16, 2, this));

        LinearLayout inner = vertical();
        inner.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 6);
        inner.setPadding(p, p, p, p);

        if (unlocked) {
            inner.addView(Ui.label(this, String.valueOf(id), 20, Ui.TEXT, true));
            LinearLayout stars = new LinearLayout(this);
            stars.setGravity(Gravity.CENTER);
            int got = progress.starsFor(id);
            for (int s = 0; s < 3; s++) {
                stars.addView(icon(s < got ? IconView.Glyph.STAR : IconView.Glyph.STAR_OUTLINE,
                        s < got ? Ui.GOLD : Ui.alpha(Ui.TEXT_DIM, 120), 12));
            }
            inner.addView(stars, marginTop(2));
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { sfx.click(); startLevel(id); }
            });
        } else {
            inner.addView(icon(IconView.Glyph.LOCK, Ui.alpha(Ui.TEXT_DIM, 150), 26));
        }
        cell.addView(inner, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        return cell;
    }

    private LinearLayout.LayoutParams cellLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 64), 1f);
        int m = Ui.dp(this, 4);
        lp.setMargins(m, 0, m, 0);
        return lp;
    }

    // ------------------------------------------------------------ gameplay
    private void startLevel(int id) {
        currentLevel = id;
        clearedThisLevel = 0;
        Level lv = LevelFactory.get(id);
        for (Objective o : lv.objectives) o.progress = 0;
        board = new Board(lv, this);
        renderer.setBoard(board);
        glView.setBoard(board);
        setScreen(Screen.GAME, buildHud(lv));
        updateHud();
    }

    private View buildHud(Level lv) {
        LinearLayout col = vertical();
        int pad = Ui.dp(this, 14);
        col.setPadding(pad, Ui.dp(this, 16), pad, pad);

        // top bar
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(Ui.roundRect(Ui.PANEL, 18, this));
        int bp = Ui.dp(this, 10);
        bar.setPadding(bp, bp, bp, bp);

        bar.addView(iconButton(IconView.Glyph.PAUSE, new Runnable() {
            @Override public void run() { sfx.click(); confirmQuitToMap(); }
        }));

        LinearLayout mid = vertical();
        mid.setGravity(Gravity.CENTER);
        hudLevel = Ui.label(this, "LEVEL " + lv.id, 13, Ui.ACCENT, true);
        hudScore = Ui.label(this, "0", 26, Ui.TEXT, true);
        mid.addView(hudLevel);
        mid.addView(hudScore);
        bar.addView(mid, lp(0, -2, 1f));

        LinearLayout movesBox = vertical();
        movesBox.setGravity(Gravity.CENTER);
        hudMoves = Ui.label(this, String.valueOf(lv.moves), 26, Ui.GOLD, true);
        movesBox.addView(hudMoves);
        movesBox.addView(Ui.label(this, "MOVES", 11, Ui.TEXT_DIM, true));
        bar.addView(movesBox);

        col.addView(bar);

        // objectives
        objectiveRow = new LinearLayout(this);
        objectiveRow.setOrientation(LinearLayout.HORIZONTAL);
        objectiveRow.setGravity(Gravity.CENTER);
        objectiveRow.setBackground(Ui.roundRect(Ui.alpha(Ui.PANEL, 180), 14, this));
        objectiveRow.setPadding(bp, Ui.dp(this, 6), bp, Ui.dp(this, 6));
        col.addView(objectiveRow, marginTop(8));

        // combo popup (overlaps board area)
        FrameLayout midempty = new FrameLayout(this);
        comboText = Ui.label(this, "", 34, Ui.GOLD, true);
        comboText.setShadowLayer(18, 0, 0, Ui.ACCENT2);
        comboText.setAlpha(0f);
        midempty.addView(comboText, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        col.addView(midempty, lp(-1, 0, 1f));

        // power-up bar
        LinearLayout power = new LinearLayout(this);
        power.setOrientation(LinearLayout.HORIZONTAL);
        power.setGravity(Gravity.CENTER);
        power.addView(powerButton(IconView.Glyph.HAMMER, "30", new Runnable() {
            @Override public void run() { usePowerHammer(); }
        }));
        power.addView(powerButton(IconView.Glyph.SHUFFLE, "20", new Runnable() {
            @Override public void run() { usePowerShuffle(); }
        }));
        power.addView(powerButton(IconView.Glyph.BOLT, "40", new Runnable() {
            @Override public void run() { usePowerMoves(); }
        }));
        col.addView(power, marginTop(6));

        return col;
    }

    private void updateHud() {
        if (board == null || hudScore == null) return;
        hudScore.setText(String.valueOf(board.score()));
        hudMoves.setText(String.valueOf(board.movesLeft()));
        if (coinText != null) coinText.setText(" " + progress.coins() + "   ");
        objectiveRow.removeAllViews();
        for (Objective o : board.level.objectives) {
            LinearLayout chip = new LinearLayout(this);
            chip.setGravity(Gravity.CENTER_VERTICAL);
            int cp = Ui.dp(this, 6);
            chip.setPadding(cp, 0, cp, 0);
            IconView.Glyph g;
            int color = Ui.TEXT;
            switch (o.kind) {
                case COLLECT_COLOR: g = IconView.Glyph.GEM; color = GemType.byIndex(o.color).argb(); break;
                case CLEAR_JELLY:   g = IconView.Glyph.GEM; color = 0xFFCF8CFF; break;
                case CLEAR_ICE:     g = IconView.Glyph.GEM; color = 0xFFBFEFFF; break;
                default:            g = IconView.Glyph.STAR; color = Ui.GOLD;
            }
            chip.addView(icon(g, color, 18));
            String txt = o.kind == Objective.Kind.SCORE
                    ? " " + o.progress + "/" + o.target
                    : " " + o.remaining();
            TextView tv = Ui.label(this, txt, 15, o.done() ? Ui.GREEN : Ui.TEXT, true);
            chip.addView(tv);
            if (o.done()) chip.addView(icon(IconView.Glyph.STAR, Ui.GREEN, 14));
            objectiveRow.addView(chip);
        }
    }

    private void showCombo(int depth) {
        if (comboText == null || depth < 2) return;
        comboText.setText("COMBO x" + depth);
        comboText.setAlpha(1f);
        comboText.setScaleX(0.6f); comboText.setScaleY(0.6f);
        comboText.animate().alpha(0f).scaleX(1.4f).scaleY(1.4f).setDuration(700).start();
    }

    // ------------------------------------------------------------ power-ups
    private void usePowerHammer() {
        if (board == null || !board.acceptsInput()) return;
        if (glView.isTapArmed()) return;
        if (!progress.spendCoins(30)) { toast("Need 30 coins"); return; }
        sfx.click();
        updateHud();
        glView.armTap();
        toast("Tap a gem to smash");
    }

    private void onBoardTap(final int r, final int c) {
        if (board == null) return;
        glView.queueEvent(new Runnable() {
            @Override public void run() { board.powerRemove(r, c); }
        });
    }

    private void usePowerShuffle() {
        if (board == null || !board.acceptsInput()) return;
        if (!progress.spendCoins(20)) { toast("Need 20 coins"); return; }
        sfx.click();
        updateHud();
        glView.queueEvent(new Runnable() {
            @Override public void run() { board.powerShuffle(); }
        });
    }

    private void usePowerMoves() {
        if (board == null || !board.acceptsInput()) return;
        if (!progress.spendCoins(40)) { toast("Need 40 coins"); return; }
        sfx.click();
        updateHud();
        glView.queueEvent(new Runnable() {
            @Override public void run() { board.addMoves(5); }
        });
    }

    // ------------------------------------------------------------ listener
    @Override public void onCleared(int row, int col, int color, boolean bySpecial) {
        renderer.spawnParticles(row, col, color);
        clearedThisLevel++;
    }

    @Override public void onSpecialForged(int row, int col, Special special) {
        renderer.spawnParticles(row, col, 6);
        sfx.special();
    }

    @Override public void onProgress() {
        runOnUiThread(new Runnable() { @Override public void run() { updateHud(); } });
    }

    @Override public void onCombo(final int depth) {
        sfx.match(depth);
        runOnUiThread(new Runnable() { @Override public void run() { showCombo(depth); } });
    }

    @Override public void onInvalidSwap() { sfx.invalid(); }

    @Override public void onFinished(final boolean won, final int score, final int stars) {
        if (won) sfx.win(); else sfx.lose();
        progress.addMatches(clearedThisLevel);
        if (won) progress.recordWin(currentLevel, stars, score);
        final List<Achievement> newly = Achievement.evaluate(progress);
        runOnUiThread(new Runnable() {
            @Override public void run() {
                showEndDialog(won, score, stars);
                for (Achievement a : newly) toast("Achievement: " + a.title);
            }
        });
    }

    // ------------------------------------------------------------ dialogs
    private void showEndDialog(boolean won, int score, int stars) {
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(0xCC000000);

        LinearLayout panel = vertical();
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setBackground(Ui.gradient(Ui.PANEL2, Ui.PANEL, 24, this));
        int p = Ui.dp(this, 24);
        panel.setPadding(p, p, p, p);

        panel.addView(Ui.label(this, won ? "LEVEL CLEARED" : "OUT OF MOVES",
                26, won ? Ui.ACCENT : Ui.RED, true));

        if (won) {
            LinearLayout srow = new LinearLayout(this);
            srow.setGravity(Gravity.CENTER);
            for (int i = 0; i < 3; i++)
                srow.addView(icon(i < stars ? IconView.Glyph.STAR : IconView.Glyph.STAR_OUTLINE,
                        i < stars ? Ui.GOLD : Ui.alpha(Ui.TEXT_DIM, 120), 44));
            panel.addView(srow, marginTop(14));
        }

        panel.addView(Ui.label(this, "Score  " + score, 20, Ui.TEXT, true), marginTop(14));
        panel.addView(Ui.label(this, "Best  " + progress.bestScore(currentLevel), 14, Ui.TEXT_DIM, false), marginTop(2));

        LinearLayout btns = new LinearLayout(this);
        btns.setGravity(Gravity.CENTER);
        btns.addView(iconButton(IconView.Glyph.HOME, new Runnable() {
            @Override public void run() { sfx.click(); showMap(); }
        }));
        btns.addView(iconButton(IconView.Glyph.RESTART, new Runnable() {
            @Override public void run() { sfx.click(); startLevel(currentLevel); }
        }));
        if (won && currentLevel < LevelFactory.TOTAL) {
            btns.addView(iconButton(IconView.Glyph.NEXT, new Runnable() {
                @Override public void run() { sfx.click(); startLevel(currentLevel + 1); }
            }));
        }
        panel.addView(btns, marginTop(20));

        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(
                Ui.dp(this, 300), -2, Gravity.CENTER);
        dim.addView(panel, plp);
        overlay.addView(dim, new FrameLayout.LayoutParams(-1, -1));
    }

    private void confirmQuitToMap() {
        FrameLayout dim = new FrameLayout(this);
        dim.setBackgroundColor(0xCC000000);
        LinearLayout panel = vertical();
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setBackground(Ui.gradient(Ui.PANEL2, Ui.PANEL, 24, this));
        int p = Ui.dp(this, 24);
        panel.setPadding(p, p, p, p);
        panel.addView(Ui.label(this, "Leave level?", 22, Ui.TEXT, true));
        panel.addView(Ui.label(this, "Progress on this level is lost.", 14, Ui.TEXT_DIM, false), marginTop(6));
        LinearLayout btns = new LinearLayout(this);
        btns.setGravity(Gravity.CENTER);
        btns.addView(textButton("STAY", Ui.PANEL2, new Runnable() {
            @Override public void run() { sfx.click(); overlay.removeView((View) findDim(dim)); }
        }));
        btns.addView(textButton("LEAVE", Ui.RED, new Runnable() {
            @Override public void run() { sfx.click(); showMap(); }
        }));
        panel.addView(btns, marginTop(18));
        dim.addView(panel, new FrameLayout.LayoutParams(Ui.dp(this, 300), -2, Gravity.CENTER));
        overlay.addView(dim, new FrameLayout.LayoutParams(-1, -1));
    }

    private View findDim(View v) { return v; }

    // ------------------------------------------------------------ achievements
    private void showAchievements() {
        LinearLayout col = vertical();
        int pad = Ui.dp(this, 18);
        col.setPadding(pad, Ui.dp(this, 20), pad, pad);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(iconButton(IconView.Glyph.BACK, new Runnable() {
            @Override public void run() { sfx.click(); showMenu(); }
        }));
        top.addView(Ui.label(this, "  ACHIEVEMENTS", 22, Ui.TEXT, true));
        col.addView(top);
        col.addView(spacer(12));

        ScrollView sv = new ScrollView(this);
        LinearLayout list = vertical();
        for (Achievement a : Achievement.values()) {
            boolean got = progress.isAchievementUnlocked(a.id);
            LinearLayout rowl = new LinearLayout(this);
            rowl.setGravity(Gravity.CENTER_VERTICAL);
            rowl.setBackground(Ui.stroked(got ? Ui.PANEL2 : Ui.darken(Ui.PANEL2, 0.65f),
                    got ? Ui.GOLD : Ui.alpha(Ui.TEXT_DIM, 60), 16, 2, this));
            int rp = Ui.dp(this, 12);
            rowl.setPadding(rp, rp, rp, rp);
            rowl.addView(icon(got ? IconView.Glyph.TROPHY : IconView.Glyph.LOCK,
                    got ? Ui.GOLD : Ui.alpha(Ui.TEXT_DIM, 150), 30));
            LinearLayout txt = vertical();
            int lp = Ui.dp(this, 10);
            txt.setPadding(lp, 0, 0, 0);
            TextView t1 = Ui.label(this, a.title, 17, got ? Ui.TEXT : Ui.TEXT_DIM, true);
            t1.setGravity(Gravity.START);
            TextView t2 = Ui.label(this, a.desc, 13, Ui.TEXT_DIM, false);
            t2.setGravity(Gravity.START);
            txt.addView(t1); txt.addView(t2);
            rowl.addView(txt);
            list.addView(rowl, marginTop(8));
        }
        sv.addView(list);
        col.addView(sv);
        setScreen(Screen.ACHIEVEMENTS, col);
    }

    // ------------------------------------------------------------ widgets
    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        return l;
    }

    private View spacer(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, Ui.dp(this, dp)));
        return v;
    }

    private LinearLayout.LayoutParams lp(int w, int h, float weight) {
        return new LinearLayout.LayoutParams(w, h, weight);
    }

    private LinearLayout.LayoutParams marginTop(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = Ui.dp(this, dp);
        return lp;
    }

    private IconView icon(IconView.Glyph g, int color, int sizeDp) {
        IconView v = new IconView(this, g, color);
        int s = Ui.dp(this, sizeDp);
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return v;
    }

    private View bigButton(IconView.Glyph g, String text, int c0, int c1, final Runnable onClick) {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.buttonBg(c0, c1, this));
        int ph = Ui.dp(this, 16);
        b.setPadding(ph, ph, ph, ph);
        b.addView(icon(g, Ui.BG0, 26));
        TextView t = Ui.label(this, "  " + text, 20, Ui.BG0, true);
        b.addView(t);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onClick.run(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 300), -2);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        b.setLayoutParams(lp);
        return b;
    }

    private View menuButton(IconView.Glyph g, String text, final Runnable onClick) {
        LinearLayout b = new LinearLayout(this);
        b.setGravity(Gravity.CENTER_VERTICAL);
        b.setBackground(Ui.stroked(Ui.PANEL, Ui.alpha(Ui.ACCENT, 90), 16, 2, this));
        int ph = Ui.dp(this, 14);
        b.setPadding(ph, ph, ph, ph);
        b.addView(icon(g, Ui.ACCENT, 22));
        b.addView(Ui.label(this, "  " + text, 17, Ui.TEXT, true));
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onClick.run(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 300), -2);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        b.setLayoutParams(lp);
        return b;
    }

    private View iconButton(IconView.Glyph g, final Runnable onClick) {
        IconView v = new IconView(this, g, Ui.TEXT);
        int s = Ui.dp(this, 42);
        v.setBackground(Ui.roundRect(Ui.PANEL2, 12, this));
        int p = Ui.dp(this, 8);
        v.setPadding(p, p, p, p);
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View vv) { onClick.run(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
        lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        v.setLayoutParams(lp);
        return v;
    }

    private View textButton(String text, int color, final Runnable onClick) {
        TextView t = Ui.label(this, text, 16, Ui.TEXT, true);
        t.setBackground(Ui.roundRect(color, 12, this));
        int ph = Ui.dp(this, 12), pv = Ui.dp(this, 10);
        t.setPadding(ph, pv, ph, pv);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onClick.run(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(Ui.dp(this, 6), 0, Ui.dp(this, 6), 0);
        t.setLayoutParams(lp);
        return t;
    }

    private View powerButton(IconView.Glyph g, String cost, final Runnable onClick) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.stroked(Ui.PANEL2, Ui.alpha(Ui.GOLD, 120), 14, 2, this));
        int p = Ui.dp(this, 8);
        b.setPadding(p, p, p, p);
        b.addView(icon(g, Ui.GOLD, 26));
        LinearLayout costRow = new LinearLayout(this);
        costRow.setGravity(Gravity.CENTER);
        costRow.addView(icon(IconView.Glyph.COIN, Ui.GOLD, 12));
        costRow.addView(Ui.label(this, " " + cost, 12, Ui.TEXT, true));
        b.addView(costRow);
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onClick.run(); }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 78), -2);
        lp.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
