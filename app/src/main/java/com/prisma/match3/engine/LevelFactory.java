package com.prisma.match3.engine;

import java.util.Random;

/**
 * Deterministically synthesises the full campaign of levels. Each level id maps
 * to the same layout every time (seeded by id), so progress and star records
 * stay meaningful across sessions while offering hundreds of distinct stages
 * with escalating difficulty and rotating objective / obstacle themes.
 */
public final class LevelFactory {

    public static final int TOTAL = 300;

    private LevelFactory() {}

    public static Level get(int id) {
        if (id < 1) id = 1;
        if (id > TOTAL) id = TOTAL;
        Random rnd = new Random(0x9E3779B9L * id + 12345L);

        // Difficulty ramp 0..1 across the campaign.
        float t = (id - 1) / (float) (TOTAL - 1);

        int cols = 7;
        int rows = id < 6 ? 7 : 8;
        int numColors = id < 10 ? 5 : (id < 60 ? 6 : 7);
        int moves = Math.round(30 - 8 * t) + rnd.nextInt(6); // ~30 -> ~22 (+jitter)
        boolean specials = id >= 3;

        Level lv = new Level(id, rows, cols, numColors, moves, specials);

        // ---- Objectives: rotate themes, stack more as difficulty rises ----
        int theme = (id - 1) % 5;
        int baseScore = 2500 + id * 350 + Math.round(t * 6000);

        switch (theme) {
            case 0: // pure score run
                lv.objectives.add(new Objective(Objective.Kind.SCORE, -1, baseScore));
                break;
            case 1: // collect a color
                lv.objectives.add(new Objective(Objective.Kind.COLLECT_COLOR,
                        rnd.nextInt(numColors), 30 + Math.round(t * 45)));
                lv.objectives.add(new Objective(Objective.Kind.SCORE, -1, baseScore / 2));
                break;
            case 2: // jelly clear
                sprinkleJelly(lv, rnd, 10 + Math.round(t * 22));
                lv.objectives.add(new Objective(Objective.Kind.CLEAR_JELLY, -1, lv.jellyTotal()));
                break;
            case 3: // ice break
                sprinkleIce(lv, rnd, 8 + Math.round(t * 18));
                lv.objectives.add(new Objective(Objective.Kind.CLEAR_ICE, -1, lv.iceTotal()));
                lv.objectives.add(new Objective(Objective.Kind.SCORE, -1, baseScore / 2));
                break;
            default: // mixed marathon: two colors + score
                lv.objectives.add(new Objective(Objective.Kind.COLLECT_COLOR,
                        rnd.nextInt(numColors), 25 + Math.round(t * 30)));
                lv.objectives.add(new Objective(Objective.Kind.COLLECT_COLOR,
                        rnd.nextInt(numColors), 25 + Math.round(t * 30)));
                lv.objectives.add(new Objective(Objective.Kind.SCORE, -1, baseScore));
                break;
        }

        // ---- Stone blockers appear in later tiers ----
        if (id >= 40 && rnd.nextFloat() < 0.55f) {
            int blockers = 1 + Math.round(t * 5);
            for (int i = 0; i < blockers; i++) {
                int r = rnd.nextInt(rows);
                int c = rnd.nextInt(cols);
                // keep the board mostly open; never wall off entire columns
                if (countStoneInCol(lv, c) < rows - 3) lv.stone[r][c] = true;
            }
        }

        // ---- Star thresholds derived from the toughest score present ----
        int scoreTarget = baseScore;
        for (Objective o : lv.objectives)
            if (o.kind == Objective.Kind.SCORE) scoreTarget = Math.max(scoreTarget, o.target);
        lv.starScore[0] = Math.round(scoreTarget * 0.6f);
        lv.starScore[1] = scoreTarget;
        lv.starScore[2] = Math.round(scoreTarget * 1.6f);

        return lv;
    }

    private static void sprinkleJelly(Level lv, Random rnd, int count) {
        int placed = 0, guard = 0;
        while (placed < count && guard++ < count * 20) {
            int r = rnd.nextInt(lv.rows), c = rnd.nextInt(lv.cols);
            if (lv.jelly[r][c] == 0) {
                lv.jelly[r][c] = rnd.nextFloat() < 0.25f ? 2 : 1;
                placed++;
            }
        }
    }

    private static void sprinkleIce(Level lv, Random rnd, int count) {
        int placed = 0, guard = 0;
        while (placed < count && guard++ < count * 20) {
            int r = rnd.nextInt(lv.rows), c = rnd.nextInt(lv.cols);
            if (lv.ice[r][c] == 0) {
                lv.ice[r][c] = rnd.nextFloat() < 0.2f ? 2 : 1;
                placed++;
            }
        }
    }

    private static int countStoneInCol(Level lv, int c) {
        int n = 0;
        for (int r = 0; r < lv.rows; r++) if (lv.stone[r][c]) n++;
        return n;
    }
}
