package com.prisma.match3.engine;

/**
 * One board cell's live state: its logical gem plus the visual tween fields the
 * renderer interpolates every frame. Keeping the animation state on the tile
 * lets the engine drive a single, authoritative model that the GL renderer
 * simply reads and draws.
 */
public class Tile {
    public enum State { IDLE, FALLING, SWAPPING, CLEARING, SPAWNING }

    public int color;          // gem family index, or EMPTY
    public Special special = Special.NONE;

    // Obstacles layered over/around the gem.
    public int jelly;          // remaining jelly layers under the gem
    public int ice;            // remaining ice layers freezing the gem
    public boolean stone;      // immovable blocker; when true `color` is EMPTY

    // Visual state (grid units; row 0 at top). Rendered position is interpolated.
    public float x, y;         // current drawn position
    public float fromX, fromY; // tween origin for the active phase
    public float scale = 1f;   // 1 normal, <1 clearing/spawning
    public float alpha = 1f;
    public float spin;         // extra spin used for special detonation flair
    public State state = State.IDLE;

    public static final int EMPTY = -1;

    public boolean isEmpty() { return color == EMPTY && !stone; }
    public boolean isPlayable() { return color != EMPTY && !stone; }
    public boolean isMovable() { return color != EMPTY && !stone && ice == 0; }
}
