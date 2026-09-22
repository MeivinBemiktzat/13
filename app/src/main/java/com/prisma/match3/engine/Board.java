package com.prisma.match3.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The authoritative match-3 simulation: grid state, swap validation, match /
 * cascade resolution, special-gem forging and detonation, obstacle handling,
 * scoring and objective tracking. Time-based animation is advanced by
 * {@link #update(float)} so the GL renderer only ever reads interpolated tile
 * positions; all game rules live here.
 */
public class Board {

    public enum Phase { READY, SWAP, SWAP_BACK, CLEAR, FALL, FINISHED }

    public final int rows, cols;
    public final Level level;
    private final Tile[][] grid;
    private final Random rnd = new Random();
    private final GameListener listener;

    private Phase phase = Phase.READY;
    private float phaseT, phaseDur;

    private int movesLeft;
    private int score;
    private int comboDepth;
    private boolean won;
    private int stars;

    // pending swap being validated
    private int sr1, sc1, sr2, sc2;

    private static final float SWAP_DUR = 0.16f;
    private static final float CLEAR_DUR = 0.22f;
    private static final float FALL_DUR = 0.30f;

    public Board(Level level, GameListener listener) {
        this.level = level;
        this.rows = level.rows;
        this.cols = level.cols;
        this.listener = listener;
        this.movesLeft = level.moves;
        this.grid = new Tile[rows][cols];
        init();
    }

    // ---------------------------------------------------------------- setup
    private void init() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                Tile t = new Tile();
                t.x = c; t.y = r;
                t.jelly = level.jelly[r][c];
                t.ice = level.ice[r][c];
                t.stone = level.stone[r][c];
                t.color = t.stone ? Tile.EMPTY : randColorNoMatch(r, c);
                grid[r][c] = t;
            }
        // guarantee at least one legal move
        if (!hasValidMove()) shuffle();
    }

    private int randColorNoMatch(int r, int c) {
        for (int attempt = 0; attempt < 40; attempt++) {
            int col = rnd.nextInt(level.numColors);
            boolean hMatch = c >= 2 && grid[r][c - 1] != null && grid[r][c - 2] != null
                    && grid[r][c - 1].color == col && grid[r][c - 2].color == col;
            boolean vMatch = r >= 2 && grid[r - 1][c] != null && grid[r - 2][c] != null
                    && grid[r - 1][c].color == col && grid[r - 2][c].color == col;
            if (!hMatch && !vMatch) return col;
        }
        return rnd.nextInt(level.numColors);
    }

    // ---------------------------------------------------------------- access
    public Tile tile(int r, int c) { return grid[r][c]; }
    public Phase phase() { return phase; }
    public int score() { return score; }
    public int movesLeft() { return movesLeft; }
    public boolean acceptsInput() { return phase == Phase.READY; }
    public boolean finished() { return phase == Phase.FINISHED; }
    public boolean won() { return won; }
    public int stars() { return stars; }

    // ---------------------------------------------------------------- input
    /** Attempt to swap two adjacent cells. Returns true if the swap was begun. */
    public boolean trySwap(int r1, int c1, int r2, int c2) {
        if (phase != Phase.READY) return false;
        if (!inBounds(r1, c1) || !inBounds(r2, c2)) return false;
        if (Math.abs(r1 - r2) + Math.abs(c1 - c2) != 1) return false;
        Tile a = grid[r1][c1], b = grid[r2][c2];
        if (!a.isMovable() || !b.isMovable()) return false;

        swapLogical(r1, c1, r2, c2);
        beginTween(a, c1, r1); // a now sits at (r2,c2) target
        beginTween(b, c2, r2);
        sr1 = r1; sc1 = c1; sr2 = r2; sc2 = c2;
        phase = Phase.SWAP;
        phaseT = 0; phaseDur = SWAP_DUR;
        return true;
    }

    private void swapLogical(int r1, int c1, int r2, int c2) {
        Tile tmp = grid[r1][c1];
        grid[r1][c1] = grid[r2][c2];
        grid[r2][c2] = tmp;
    }

    private void beginTween(Tile t, float fromCol, float fromRow) {
        t.fromX = fromCol; t.fromY = fromRow;
        t.x = fromCol; t.y = fromRow;
        t.state = Tile.State.SWAPPING;
    }

    // ---------------------------------------------------------------- update
    public void update(float dt) {
        if (phase == Phase.READY || phase == Phase.FINISHED) return;
        phaseT += dt;
        float p = phaseDur <= 0 ? 1f : Math.min(1f, phaseT / phaseDur);
        float e = easeOut(p);

        switch (phase) {
            case SWAP:
            case SWAP_BACK:
                animateMoves(e);
                if (p >= 1f) onSwapArrived();
                break;
            case CLEAR:
                animateClears(p);
                if (p >= 1f) onClearDone();
                break;
            case FALL:
                animateMoves(e);
                if (p >= 1f) onFallDone();
                break;
            default: break;
        }
    }

    private void animateMoves(float e) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                Tile t = grid[r][c];
                if (t.state == Tile.State.SWAPPING || t.state == Tile.State.FALLING
                        || t.state == Tile.State.SPAWNING) {
                    t.x = t.fromX + (c - t.fromX) * e;
                    t.y = t.fromY + (r - t.fromY) * e;
                    if (t.state == Tile.State.SPAWNING) t.scale = 0.4f + 0.6f * e;
                }
            }
    }

    private void animateClears(float p) {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                Tile t = grid[r][c];
                if (t.state == Tile.State.CLEARING) {
                    t.scale = 1f - p;
                    t.alpha = 1f - p;
                    t.spin = p * 6f;
                }
            }
    }

    // ------------------------------------------------------------ swap result
    private void onSwapArrived() {
        settlePositions();
        if (phase == Phase.SWAP_BACK) {
            phase = Phase.READY;
            return;
        }
        Tile a = grid[sr2][sc2]; // a moved into (sr2,sc2)
        Tile b = grid[sr1][sc1];
        boolean special = a.special != Special.NONE || b.special != Special.NONE;

        List<int[]> clears = new ArrayList<>();
        boolean matched = collectMatches(clears);

        if (special) {
            // Swapping onto/with a special always detonates.
            activateSpecialSwap(sr1, sc1, sr2, sc2, clears);
            matched = true;
        }

        if (!matched) {
            // revert
            swapLogical(sr1, sc1, sr2, sc2);
            beginTween(grid[sr1][sc1], sc2, sr2);
            beginTween(grid[sr2][sc2], sc1, sr1);
            phase = Phase.SWAP_BACK; phaseT = 0; phaseDur = SWAP_DUR;
            if (listener != null) listener.onInvalidSwap();
            return;
        }

        movesLeft--;
        comboDepth = 0;
        beginClear(clears, true);
    }

    // ------------------------------------------------------------ clear phase
    private void beginClear(List<int[]> clears, boolean playerMove) {
        comboDepth++;
        // forge specials from qualifying runs before clearing
        forgeSpecials(clears);
        // expand any specials that fall inside the clear set
        expandSpecials(clears);

        boolean any = false;
        for (int[] cell : clears) {
            int r = cell[0], c = cell[1];
            Tile t = grid[r][c];
            if (t.state == Tile.State.CLEARING || t.color == Tile.EMPTY || t.stone) continue;
            any = true;
            t.state = Tile.State.CLEARING;
            scoreClear();
            trackObjectives(t.color);
            reduceJelly(t);
            damageNeighbours(r, c);
            if (listener != null) listener.onCleared(r, c, t.color, cell.length > 2);
        }
        if (!any) { settleAfterCascade(); return; }
        if (listener != null) listener.onCombo(comboDepth);
        phase = Phase.CLEAR; phaseT = 0; phaseDur = CLEAR_DUR;
    }

    private void onClearDone() {
        // remove cleared gems, keep forged specials (they were unmarked)
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                Tile t = grid[r][c];
                if (t.state == Tile.State.CLEARING) {
                    t.color = Tile.EMPTY;
                    t.special = Special.NONE;
                    t.scale = 1f; t.alpha = 1f; t.spin = 0f;
                    t.state = Tile.State.IDLE;
                }
            }
        applyGravityAndRefill();
        phase = Phase.FALL; phaseT = 0; phaseDur = FALL_DUR;
    }

    private void onFallDone() {
        settlePositions();
        List<int[]> next = new ArrayList<>();
        if (collectMatches(next)) {
            beginClear(next, false);       // cascade
        } else {
            settleAfterCascade();
        }
    }

    private void settleAfterCascade() {
        if (!hasValidMove()) shuffle();
        checkEnd();
        if (phase != Phase.FINISHED) phase = Phase.READY;
        if (listener != null) listener.onProgress();
    }

    // ------------------------------------------------------------ matching
    /** Fills `out` with [row,col] of every gem in a run of >=3. Returns true if any. */
    private boolean collectMatches(List<int[]> out) {
        boolean[][] m = new boolean[rows][cols];
        // horizontal
        for (int r = 0; r < rows; r++) {
            int run = 1;
            for (int c = 1; c <= cols; c++) {
                boolean same = c < cols && matchable(r, c) && matchable(r, c - 1)
                        && grid[r][c].color == grid[r][c - 1].color;
                if (same) run++;
                else {
                    if (run >= 3) for (int k = c - run; k < c; k++) m[r][k] = true;
                    run = 1;
                }
            }
        }
        // vertical
        for (int c = 0; c < cols; c++) {
            int run = 1;
            for (int r = 1; r <= rows; r++) {
                boolean same = r < rows && matchable(r, c) && matchable(r - 1, c)
                        && grid[r][c].color == grid[r - 1][c].color;
                if (same) run++;
                else {
                    if (run >= 3) for (int k = r - run; k < r; k++) m[k][c] = true;
                    run = 1;
                }
            }
        }
        boolean any = false;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (m[r][c]) { out.add(new int[]{r, c}); any = true; }
        return any;
    }

    private boolean matchable(int r, int c) {
        Tile t = grid[r][c];
        return t.color != Tile.EMPTY && !t.stone && t.ice == 0;
    }

    // ------------------------------------------------------------ specials
    private void forgeSpecials(List<int[]> clears) {
        if (!level.specialsEnabled) return;
        // Detect runs again to size them; upgrade one pivot per qualifying run.
        // Horizontal runs.
        for (int r = 0; r < rows; r++) {
            int c = 0;
            while (c < cols) {
                if (!matchable(r, c)) { c++; continue; }
                int start = c, color = grid[r][c].color;
                while (c < cols && matchable(r, c) && grid[r][c].color == color) c++;
                int len = c - start;
                if (len >= 4) forge(r, start + len / 2, color, len >= 5 ? Special.NOVA : Special.STRIPE_H, clears);
            }
        }
        // Vertical runs.
        for (int cc = 0; cc < cols; cc++) {
            int r = 0;
            while (r < rows) {
                if (!matchable(r, cc)) { r++; continue; }
                int start = r, color = grid[r][cc].color;
                while (r < rows && matchable(r, cc) && grid[r][cc].color == color) r++;
                int len = r - start;
                if (len >= 4) forge(start + len / 2, cc, color, len >= 5 ? Special.NOVA : Special.STRIPE_V, clears);
            }
        }
        // Bomb at intersections (cell in both an H>=3 and V>=3 run).
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (partOfClear(clears, r, c) && inH3(r, c) && inV3(r, c))
                    forge(r, c, grid[r][c].color, Special.BOMB, clears);
    }

    private void forge(int r, int c, int color, Special sp, List<int[]> clears) {
        Tile t = grid[r][c];
        if (t.stone || t.color == Tile.EMPTY) return;
        t.special = sp;
        t.color = color;
        // this pivot survives: drop it from the clear set
        for (int i = clears.size() - 1; i >= 0; i--)
            if (clears.get(i)[0] == r && clears.get(i)[1] == c) clears.remove(i);
        if (listener != null) listener.onSpecialForged(r, c, sp);
    }

    private boolean inH3(int r, int c) {
        int color = grid[r][c].color, n = 1;
        for (int k = c - 1; k >= 0 && matchable(r, k) && grid[r][k].color == color; k--) n++;
        for (int k = c + 1; k < cols && matchable(r, k) && grid[r][k].color == color; k++) n++;
        return n >= 3;
    }

    private boolean inV3(int r, int c) {
        int color = grid[r][c].color, n = 1;
        for (int k = r - 1; k >= 0 && matchable(k, c) && grid[k][c].color == color; k--) n++;
        for (int k = r + 1; k < rows && matchable(k, c) && grid[k][c].color == color; k++) n++;
        return n >= 3;
    }

    private boolean partOfClear(List<int[]> clears, int r, int c) {
        for (int[] cell : clears) if (cell[0] == r && cell[1] == c) return true;
        return false;
    }

    /** Detonate every special caught in the current clear set, chaining. */
    private void expandSpecials(List<int[]> clears) {
        boolean[][] queued = new boolean[rows][cols];
        List<int[]> work = new ArrayList<>(clears);
        for (int[] cell : clears) queued[cell[0]][cell[1]] = true;

        for (int i = 0; i < work.size(); i++) {
            int r = work.get(i)[0], c = work.get(i)[1];
            Tile t = grid[r][c];
            if (t.special == Special.NONE) continue;
            Special sp = t.special;
            t.special = Special.NONE; // consumed
            List<int[]> blast = new ArrayList<>();
            switch (sp) {
                case STRIPE_H: for (int k = 0; k < cols; k++) blast.add(new int[]{r, k, 1}); break;
                case STRIPE_V: for (int k = 0; k < rows; k++) blast.add(new int[]{k, c, 1}); break;
                case BOMB:
                    for (int dr = -2; dr <= 2; dr++)
                        for (int dc = -2; dc <= 2; dc++) {
                            int nr = r + dr, nc = c + dc;
                            if (inBounds(nr, nc)) blast.add(new int[]{nr, nc, 1});
                        }
                    break;
                case NOVA:
                    int target = t.color;
                    for (int rr = 0; rr < rows; rr++)
                        for (int ccx = 0; ccx < cols; ccx++)
                            if (grid[rr][ccx].color == target) blast.add(new int[]{rr, ccx, 1});
                    break;
                default: break;
            }
            for (int[] b : blast) {
                int br = b[0], bc = b[1];
                Tile bt = grid[br][bc];
                if (bt.stone || bt.color == Tile.EMPTY) continue;
                if (!queued[br][bc]) {
                    queued[br][bc] = true;
                    int[] add = new int[]{br, bc, 1};
                    clears.add(add);
                    work.add(add);
                }
            }
        }
    }

    private void activateSpecialSwap(int r1, int c1, int r2, int c2, List<int[]> clears) {
        // Ensure both swapped cells are in the clear set so expandSpecials fires them.
        if (!partOfClear(clears, r1, c1)) clears.add(new int[]{r1, c1});
        if (!partOfClear(clears, r2, c2)) clears.add(new int[]{r2, c2});
    }

    // ------------------------------------------------------------ obstacles
    private void reduceJelly(Tile t) {
        if (t.jelly > 0) {
            t.jelly--;
            if (t.jelly == 0) bumpObjective(Objective.Kind.CLEAR_JELLY, -1);
        }
    }

    private void damageNeighbours(int r, int c) {
        int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] o : d) {
            int nr = r + o[0], nc = c + o[1];
            if (!inBounds(nr, nc)) continue;
            Tile n = grid[nr][nc];
            if (n.ice > 0) {
                n.ice--;
                if (n.ice == 0) bumpObjective(Objective.Kind.CLEAR_ICE, -1);
            } else if (n.stone) {
                n.stone = false;          // shatter blocker; cell becomes refillable
                n.color = Tile.EMPTY;
            }
        }
    }

    // ------------------------------------------------------------ gravity
    private void applyGravityAndRefill() {
        for (int c = 0; c < cols; c++) {
            int writeRow = rows - 1;
            for (int r = rows - 1; r >= 0; r--) {
                Tile t = grid[r][c];
                // Stones and still-frozen (iced) gems are fixed: they anchor the
                // column and split it into segments; nothing falls through them.
                if (t.stone || t.ice > 0) { writeRow = r - 1; continue; }
                if (t.color != Tile.EMPTY) {
                    if (writeRow != r) {
                        moveTile(r, c, writeRow, c);
                    }
                    writeRow--;
                }
            }
            // refill empties from writeRow upward within reachable cells
            int spawnIndex = 0;
            for (int r = writeRow; r >= 0; r--) {
                Tile t = grid[r][c];
                if (t.stone || t.ice > 0) break; // barrier: stop the top segment here
                if (t.color == Tile.EMPTY) {
                    t.color = rnd.nextInt(level.numColors);
                    t.special = Special.NONE;
                    t.state = Tile.State.SPAWNING;
                    t.fromX = c; t.fromY = -1 - spawnIndex;
                    t.x = c; t.y = t.fromY;
                    t.scale = 0.4f; t.alpha = 1f;
                    spawnIndex++;
                }
            }
            // any leftover empty cells beneath stones: fill them in place (small drop)
            for (int r = 0; r < rows; r++) {
                Tile t = grid[r][c];
                if (!t.stone && t.color == Tile.EMPTY) {
                    t.color = rnd.nextInt(level.numColors);
                    t.special = Special.NONE;
                    t.state = Tile.State.SPAWNING;
                    t.fromX = c; t.fromY = r - 1.2f;
                    t.x = c; t.y = t.fromY;
                    t.scale = 0.4f;
                }
            }
        }
    }

    private void moveTile(int fromR, int fromC, int toR, int toC) {
        Tile moving = grid[fromR][fromC];
        Tile dest = grid[toR][toC];
        // swap the objects so obstacle state stays bound to its cell:
        // move only the gem payload (color/special) and its visual, keep jelly/ice/stone with cell.
        dest.color = moving.color;
        dest.special = moving.special;
        dest.state = Tile.State.FALLING;
        dest.fromX = toC; dest.fromY = fromR;   // fall from old row
        dest.x = toC; dest.y = fromR;
        dest.scale = 1f; dest.alpha = 1f;

        moving.color = Tile.EMPTY;
        moving.special = Special.NONE;
        moving.state = Tile.State.IDLE;
    }

    private void settlePositions() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                Tile t = grid[r][c];
                t.x = c; t.y = r; t.scale = 1f; t.alpha = 1f; t.spin = 0f;
                if (t.state != Tile.State.CLEARING) t.state = Tile.State.IDLE;
            }
    }

    // ------------------------------------------------------------ scoring
    private void scoreClear() {
        int gain = 60 * Math.max(1, comboDepth);
        score += gain;
        for (Objective o : level.objectives)
            if (o.kind == Objective.Kind.SCORE) o.progress = score;
    }

    private void trackObjectives(int color) {
        bumpObjective(Objective.Kind.COLLECT_COLOR, color);
    }

    private void bumpObjective(Objective.Kind kind, int color) {
        for (Objective o : level.objectives)
            if (o.kind == kind && (color < 0 || o.color == color)) o.progress++;
    }

    // ------------------------------------------------------------ end state
    private void checkEnd() {
        boolean all = true;
        for (Objective o : level.objectives) if (!o.done()) { all = false; break; }
        if (all) {
            won = true;
            stars = computeStars();
            phase = Phase.FINISHED;
            if (listener != null) listener.onFinished(true, score, stars);
        } else if (movesLeft <= 0) {
            won = false;
            stars = 0;
            phase = Phase.FINISHED;
            if (listener != null) listener.onFinished(false, score, 0);
        }
    }

    private int computeStars() {
        int s = 0;
        for (int th : level.starScore) if (score >= th) s++;
        return Math.max(1, s); // winning always yields at least one star
    }

    // ------------------------------------------------------------ helpers
    private boolean inBounds(int r, int c) { return r >= 0 && r < rows && c >= 0 && c < cols; }

    private static float easeOut(float p) { return 1f - (1f - p) * (1f - p); }

    /** True if any adjacent swap would create a match. */
    public boolean hasValidMove() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isMovable()) continue;
                if (grid[r][c].special != Special.NONE) return true;
                if (c + 1 < cols && grid[r][c + 1].isMovable()) {
                    swapLogical(r, c, r, c + 1);
                    boolean ok = createsMatch(r, c) || createsMatch(r, c + 1);
                    swapLogical(r, c, r, c + 1);
                    if (ok) return true;
                }
                if (r + 1 < rows && grid[r + 1][c].isMovable()) {
                    swapLogical(r, c, r + 1, c);
                    boolean ok = createsMatch(r, c) || createsMatch(r + 1, c);
                    swapLogical(r, c, r + 1, c);
                    if (ok) return true;
                }
            }
        return false;
    }

    private boolean createsMatch(int r, int c) { return inH3(r, c) || inV3(r, c); }

    private void shuffle() {
        List<Integer> colors = new ArrayList<>();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c].isMovable()) colors.add(grid[r][c].color);
        for (int attempt = 0; attempt < 30; attempt++) {
            java.util.Collections.shuffle(colors, rnd);
            int i = 0;
            for (int r = 0; r < rows; r++)
                for (int c = 0; c < cols; c++)
                    if (grid[r][c].isMovable()) grid[r][c].color = colors.get(i++);
            if (hasValidMove()) break;
        }
        settlePositions();
    }

    // ------------------------------------------------------------ power-ups
    /** Hammer: instantly clear one gem (no move spent). */
    public boolean powerRemove(int r, int c) {
        if (phase != Phase.READY || !inBounds(r, c)) return false;
        Tile t = grid[r][c];
        if (t.color == Tile.EMPTY || t.stone) return false;
        comboDepth = 0;
        List<int[]> clears = new ArrayList<>();
        clears.add(new int[]{r, c});
        beginClear(clears, false);
        return true;
    }

    /** Shuffle: rearrange the movable gems (no move spent). */
    public boolean powerShuffle() {
        if (phase != Phase.READY) return false;
        shuffle();
        if (listener != null) listener.onProgress();
        return true;
    }

    /** Booster: grant extra moves. */
    public void addMoves(int n) {
        movesLeft += n;
        if (listener != null) listener.onProgress();
    }

    /** Adjacent-cell hint for the UI: returns {r1,c1,r2,c2} or null. */
    public int[] findHint() {
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                if (!grid[r][c].isMovable()) continue;
                if (c + 1 < cols && grid[r][c + 1].isMovable()) {
                    swapLogical(r, c, r, c + 1);
                    boolean ok = createsMatch(r, c) || createsMatch(r, c + 1);
                    swapLogical(r, c, r, c + 1);
                    if (ok) return new int[]{r, c, r, c + 1};
                }
                if (r + 1 < rows && grid[r + 1][c].isMovable()) {
                    swapLogical(r, c, r + 1, c);
                    boolean ok = createsMatch(r, c) || createsMatch(r + 1, c);
                    swapLogical(r, c, r + 1, c);
                    if (ok) return new int[]{r, c, r + 1, c};
                }
            }
        return null;
    }
}
