package com.prisma.match3.engine;

/** Board -> presentation callbacks for feedback (particles, sound, UI refresh). */
public interface GameListener {
    /** A gem was cleared at grid (row,col) with the given color index. */
    void onCleared(int row, int col, int color, boolean bySpecial);
    /** A special was forged at (row,col). */
    void onSpecialForged(int row, int col, Special special);
    /** Score or objective progress changed. */
    void onProgress();
    /** Cascade / combo depth reached (1 = first clear of a chain). */
    void onCombo(int depth);
    /** An attempted swap produced no match and was reverted. */
    void onInvalidSwap();
    /** Level resolved: won == true means all objectives met. */
    void onFinished(boolean won, int score, int stars);
}
