package com.prisma.match3.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable description of one level: board size, palette size, move budget,
 * objectives, the initial obstacle layout and the score thresholds for 1/2/3
 * stars. Levels are produced by {@link LevelFactory}.
 */
public class Level {
    public final int id;            // 1-based
    public final int rows, cols;
    public final int numColors;
    public final int moves;
    public final List<Objective> objectives = new ArrayList<>();

    // Obstacle layouts, sized rows x cols. Values are starting layer counts.
    public final int[][] jelly;     // >0 => jelly present with N layers
    public final int[][] ice;       // >0 => gem frozen under N ice layers
    public final boolean[][] stone; // immovable blocker (no gem occupies it)

    public final int[] starScore = new int[3]; // ascending score thresholds
    public final boolean specialsEnabled;

    public Level(int id, int rows, int cols, int numColors, int moves, boolean specials) {
        this.id = id;
        this.rows = rows;
        this.cols = cols;
        this.numColors = numColors;
        this.moves = moves;
        this.specialsEnabled = specials;
        this.jelly = new int[rows][cols];
        this.ice = new int[rows][cols];
        this.stone = new boolean[rows][cols];
    }

    public int jellyTotal() {
        int n = 0;
        for (int[] row : jelly) for (int v : row) n += v > 0 ? 1 : 0;
        return n;
    }

    public int iceTotal() {
        int n = 0;
        for (int[] row : ice) for (int v : row) n += v > 0 ? 1 : 0;
        return n;
    }
}
