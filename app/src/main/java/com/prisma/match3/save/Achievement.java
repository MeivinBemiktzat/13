package com.prisma.match3.save;

/** Offline achievements, evaluated against {@link Progress} counters. */
public enum Achievement {
    FIRST_WIN   ("first_win",   "First Sparkle",   "Win your first level"),
    STARS_25    ("stars_25",    "Star Collector",  "Earn 25 stars"),
    STARS_100   ("stars_100",   "Constellation",   "Earn 100 stars"),
    LEVEL_10    ("level_10",    "Getting Warm",    "Reach level 10"),
    LEVEL_50    ("level_50",    "Seasoned",        "Reach level 50"),
    LEVEL_150   ("level_150",   "Prisma Master",   "Reach level 150"),
    MATCHES_500 ("matches_500", "Chain Reactor",   "Clear 500 gems"),
    MATCHES_5000("matches_5000","Cascade Legend",  "Clear 5000 gems"),
    RICH        ("rich",        "Treasure Hoard",  "Hold 500 coins");

    public final String id, title, desc;

    Achievement(String id, String title, String desc) {
        this.id = id; this.title = title; this.desc = desc;
    }

    /** Evaluate all achievements against current progress, unlocking any newly met. */
    public static java.util.List<Achievement> evaluate(Progress p) {
        java.util.List<Achievement> newly = new java.util.ArrayList<>();
        check(p, FIRST_WIN,    p.unlockedLevel() > 1, newly);
        check(p, STARS_25,     p.totalStars() >= 25, newly);
        check(p, STARS_100,    p.totalStars() >= 100, newly);
        check(p, LEVEL_10,     p.unlockedLevel() >= 10, newly);
        check(p, LEVEL_50,     p.unlockedLevel() >= 50, newly);
        check(p, LEVEL_150,    p.unlockedLevel() >= 150, newly);
        check(p, MATCHES_500,  p.totalMatches() >= 500, newly);
        check(p, MATCHES_5000, p.totalMatches() >= 5000, newly);
        check(p, RICH,         p.coins() >= 500, newly);
        return newly;
    }

    private static void check(Progress p, Achievement a, boolean met, java.util.List<Achievement> out) {
        if (met && !p.isAchievementUnlocked(a.id)) {
            p.unlockAchievement(a.id);
            out.add(a);
        }
    }
}
