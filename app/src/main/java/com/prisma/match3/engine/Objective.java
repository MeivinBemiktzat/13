package com.prisma.match3.engine;

/** A single win condition for a level. A level may combine several. */
public class Objective {
    public enum Kind {
        SCORE,          // reach target points
        COLLECT_COLOR,  // clear `target` gems of `color`
        CLEAR_JELLY,    // remove `target` jelly tiles
        CLEAR_ICE       // shatter `target` iced tiles
    }

    public final Kind kind;
    public final int color;   // gem index for COLLECT_COLOR, else -1
    public final int target;
    public int progress;

    public Objective(Kind kind, int color, int target) {
        this.kind = kind;
        this.color = color;
        this.target = target;
    }

    public boolean done() { return progress >= target; }

    public int remaining() { return Math.max(0, target - progress); }

    public String label() {
        switch (kind) {
            case SCORE:         return "Score " + target;
            case COLLECT_COLOR: return "Collect " + GemType.byIndex(color).display;
            case CLEAR_JELLY:   return "Clear jelly";
            case CLEAR_ICE:     return "Break ice";
        }
        return "";
    }
}
