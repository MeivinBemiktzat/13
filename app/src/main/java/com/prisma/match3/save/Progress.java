package com.prisma.match3.save;

import android.content.Context;
import android.content.SharedPreferences;

import com.prisma.match3.engine.LevelFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Local, offline persistence of campaign progress via SharedPreferences: the
 * highest unlocked level, per-level star ratings and high scores, the coin
 * balance, unlocked achievements and audio preference. Nothing leaves the
 * device.
 */
public class Progress {
    private static final String FILE = "prisma_progress";
    private static final String K_UNLOCKED = "unlocked";
    private static final String K_COINS = "coins";
    private static final String K_SOUND = "sound";
    private static final String K_ACH = "achievements";
    private static final String K_STAR = "star_";
    private static final String K_BEST = "best_";
    private static final String K_TOTALSCORE = "total_score";
    private static final String K_MATCHES = "total_matches";

    private final SharedPreferences sp;

    public Progress(Context ctx) {
        sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int unlockedLevel() { return Math.max(1, sp.getInt(K_UNLOCKED, 1)); }

    public int starsFor(int level) { return sp.getInt(K_STAR + level, 0); }

    public int bestScore(int level) { return sp.getInt(K_BEST + level, 0); }

    public int coins() { return sp.getInt(K_COINS, 0); }

    public boolean soundOn() { return sp.getBoolean(K_SOUND, true); }

    public void setSound(boolean on) { sp.edit().putBoolean(K_SOUND, on).apply(); }

    public int totalStars() {
        int s = 0;
        for (int i = 1; i <= LevelFactory.TOTAL; i++) s += starsFor(i);
        return s;
    }

    public long totalScore() { return sp.getLong(K_TOTALSCORE, 0); }

    public int totalMatches() { return sp.getInt(K_MATCHES, 0); }

    public void addMatches(int n) {
        sp.edit().putInt(K_MATCHES, sp.getInt(K_MATCHES, 0) + n).apply();
    }

    /** Record a win: bump stars/score records, unlock next level, grant coins. */
    public void recordWin(int level, int stars, int score) {
        SharedPreferences.Editor e = sp.edit();
        if (stars > starsFor(level)) e.putInt(K_STAR + level, stars);
        if (score > bestScore(level)) e.putInt(K_BEST + level, score);
        if (level + 1 > unlockedLevel() && level < LevelFactory.TOTAL)
            e.putInt(K_UNLOCKED, level + 1);
        e.putInt(K_COINS, coins() + 20 + stars * 10);
        e.putLong(K_TOTALSCORE, totalScore() + score);
        e.apply();
    }

    public boolean spendCoins(int amount) {
        if (coins() < amount) return false;
        sp.edit().putInt(K_COINS, coins() - amount).apply();
        return true;
    }

    public Set<String> unlockedAchievements() {
        return new HashSet<>(sp.getStringSet(K_ACH, new HashSet<String>()));
    }

    public boolean isAchievementUnlocked(String id) {
        return sp.getStringSet(K_ACH, new HashSet<String>()).contains(id);
    }

    public void unlockAchievement(String id) {
        Set<String> s = new HashSet<>(sp.getStringSet(K_ACH, new HashSet<String>()));
        if (s.add(id)) sp.edit().putStringSet(K_ACH, s).apply();
    }
}
