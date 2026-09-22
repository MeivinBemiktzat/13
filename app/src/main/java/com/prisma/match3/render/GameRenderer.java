package com.prisma.match3.render;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;

import com.prisma.match3.engine.Board;
import com.prisma.match3.engine.GemType;
import com.prisma.match3.engine.Special;
import com.prisma.match3.engine.Tile;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 2.0 renderer. Draws an animated gradient backdrop, the board as
 * individually lit 3D crystals (with specular, rim light and facet sheen),
 * translucent obstacle overlays (jelly / ice / stone) and an additive particle
 * layer. It advances the {@link Board} simulation each frame and processes swap
 * requests on the GL thread so all board access is single-threaded.
 */
public class GameRenderer implements GLSurfaceView.Renderer {

    public interface Hooks { void afterFrame(); }

    private volatile Board board;
    private Hooks hooks;

    private GemMesh mesh;
    private final ParticleSystem particles = new ParticleSystem();

    private ShaderProgram gemProg, bgProg;
    private int gPos, gNorm, gMVP, gModel, gColor, gSheen, gAlpha, gEmissive;
    private int bPos, bTime, bRes;
    private FloatBuffer bgQuad;

    private final float[] proj = new float[16];
    private final float[] model = new float[16];
    private final float[] mvp = new float[16];
    private final float[] tmp = new float[16];

    public volatile float halfW = 5, halfH = 6;
    private int cols = 7, rows = 8;
    private int projCols = -1, projRows = -1;
    private int vpW = 1, vpH = 1;
    private long lastNs;
    private float time;

    private final ConcurrentLinkedQueue<int[]> swaps = new ConcurrentLinkedQueue<>();

    private static final String GEM_VS =
            "uniform mat4 uMVP; uniform mat4 uModel;\n" +
            "attribute vec3 aPos; attribute vec3 aNormal;\n" +
            "varying vec3 vN; varying vec3 vV;\n" +
            "void main(){ vN = mat3(uModel)*aNormal; vV = vec3(0.0,0.0,1.0);\n" +
            "  gl_Position = uMVP * vec4(aPos,1.0); }";

    private static final String GEM_FS =
            "precision mediump float;\n" +
            "uniform vec3 uColor; uniform vec3 uSheen; uniform float uAlpha; uniform float uEmissive;\n" +
            "varying vec3 vN; varying vec3 vV;\n" +
            "void main(){\n" +
            "  vec3 N = normalize(vN);\n" +
            "  vec3 L = normalize(vec3(0.35,0.65,0.85));\n" +
            "  vec3 V = normalize(vV);\n" +
            "  float diff = max(dot(N,L),0.0);\n" +
            "  vec3 H = normalize(L+V);\n" +
            "  float spec = pow(max(dot(N,H),0.0),42.0);\n" +
            "  float rim = pow(1.0-max(dot(N,V),0.0),2.5);\n" +
            "  vec3 c = uColor*(0.32+0.72*diff);\n" +
            "  c += uSheen*spec*1.1;\n" +
            "  c += uSheen*rim*0.35;\n" +
            "  c += uColor*uEmissive;\n" +
            "  gl_FragColor = vec4(c, uAlpha);\n" +
            "}";

    private static final String BG_VS =
            "attribute vec2 aPos; varying vec2 vUv;\n" +
            "void main(){ vUv = aPos*0.5+0.5; gl_Position = vec4(aPos,0.0,1.0); }";

    private static final String BG_FS =
            "precision mediump float; varying vec2 vUv; uniform float uTime;\n" +
            "void main(){\n" +
            "  vec3 top = vec3(0.06,0.05,0.16);\n" +
            "  vec3 bot = vec3(0.10,0.06,0.24);\n" +
            "  vec3 col = mix(bot, top, vUv.y);\n" +
            "  float g1 = 0.10*sin(uTime*0.6 + vUv.x*6.0) + 0.10*cos(uTime*0.4 + vUv.y*5.0);\n" +
            "  float d = distance(vUv, vec2(0.5+0.18*sin(uTime*0.25), 0.55+0.12*cos(uTime*0.2)));\n" +
            "  col += vec3(0.12,0.09,0.22)*smoothstep(0.7,0.0,d);\n" +
            "  col += g1*vec3(0.05,0.04,0.10);\n" +
            "  gl_FragColor = vec4(col,1.0);\n" +
            "}";

    public void setBoard(Board b) {
        this.board = b;
        if (b != null) { this.cols = b.cols; this.rows = b.rows; }
    }

    public void setHooks(Hooks h) { this.hooks = h; }

    public void requestSwap(int r1, int c1, int r2, int c2) {
        swaps.add(new int[]{r1, c1, r2, c2});
    }

    public float worldX(float c) { return c - (cols - 1) / 2f; }
    public float worldY(float r) { return (rows - 1) / 2f - r; }

    public void spawnParticles(int r, int c, int colorIdx) {
        GemType g = GemType.byIndex(colorIdx);
        particles.burst(worldX(c), worldY(r), g.sr, g.sg, g.sb, 16);
    }

    // -------------------------------------------------------------- GL setup
    @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.05f, 0.04f, 0.14f, 1f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        mesh = new GemMesh();
        particles.init();

        gemProg = new ShaderProgram(GEM_VS, GEM_FS);
        gPos = gemProg.attrib("aPos");
        gNorm = gemProg.attrib("aNormal");
        gMVP = gemProg.uniform("uMVP");
        gModel = gemProg.uniform("uModel");
        gColor = gemProg.uniform("uColor");
        gSheen = gemProg.uniform("uSheen");
        gAlpha = gemProg.uniform("uAlpha");
        gEmissive = gemProg.uniform("uEmissive");

        bgProg = new ShaderProgram(BG_VS, BG_FS);
        bPos = bgProg.attrib("aPos");
        bTime = bgProg.uniform("uTime");

        float[] quad = {-1, -1, 1, -1, -1, 1, 1, -1, 1, 1, -1, 1};
        ByteBuffer bb = ByteBuffer.allocateDirect(quad.length * 4).order(ByteOrder.nativeOrder());
        bgQuad = bb.asFloatBuffer();
        bgQuad.put(quad).position(0);

        lastNs = System.nanoTime();
    }

    @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        vpW = width; vpH = height;
        rebuildProjection();
    }

    private void rebuildProjection() {
        float aspect = vpW / (float) Math.max(1, vpH);
        // Fit the board (cols x rows cells) with margin, preserving aspect.
        float needH = rows / 2f + 0.9f;
        float needW = cols / 2f + 0.6f;
        float hh = Math.max(needH, needW / aspect);
        halfH = hh;
        halfW = hh * aspect;
        Matrix.orthoM(proj, 0, -halfW, halfW, -halfH, halfH, -12f, 12f);
        projCols = cols; projRows = rows;
    }

    // -------------------------------------------------------------- frame
    @Override public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = (now - lastNs) / 1_000_000_000f;
        lastNs = now;
        if (dt > 0.05f) dt = 0.05f;
        time += dt;

        Board b = board;
        if (b != null && (b.cols != projCols || b.rows != projRows)) {
            cols = b.cols; rows = b.rows;
            rebuildProjection();
        }
        if (b != null) {
            int[] s;
            while ((s = swaps.poll()) != null) b.trySwap(s[0], s[1], s[2], s[3]);
            b.update(dt);
        }
        particles.update(dt);

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        drawBackground();

        if (b != null) drawBoard(b);
        particles.draw(proj);

        if (hooks != null) hooks.afterFrame();
    }

    private void drawBackground() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        bgProg.use();
        GLES20.glUniform1f(bTime, time);
        bgQuad.position(0);
        GLES20.glVertexAttribPointer(bPos, 2, GLES20.GL_FLOAT, false, 0, bgQuad);
        GLES20.glEnableVertexAttribArray(bPos);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    private void drawBoard(Board b) {
        gemProg.use();
        for (int r = 0; r < b.rows; r++) {
            for (int c = 0; c < b.cols; c++) {
                Tile t = b.tile(r, c);
                float wx = worldX(t.x), wy = worldY(t.y);

                if (t.stone) {
                    drawGem(wx, wy, 0.60f, 0f, 0.45f, 0.45f, 0.50f, 0.7f, 0.72f, 0.78f, 1f, 0f);
                    continue;
                }
                if (t.jelly > 0) {
                    float a = t.jelly >= 2 ? 0.55f : 0.35f;
                    drawGem(wx, wy, 0.82f, time * 20f, 0.35f, 0.22f, 0.55f, 0.8f, 0.6f, 1.0f, a, 0.1f);
                }
                if (t.color == Tile.EMPTY) continue;

                GemType g = GemType.byIndex(t.color);
                float emis = t.special != Special.NONE ? 0.25f + 0.2f * (float) Math.sin(time * 6) : 0f;
                float spin = time * 26f + (r * 13 + c * 7) + t.spin * 90f;
                drawGem(wx, wy, 0.60f * t.scale, spin,
                        g.r, g.g, g.b, g.sr, g.sg, g.sb, t.alpha, emis);

                if (t.ice > 0) {
                    float a = t.ice >= 2 ? 0.6f : 0.4f;
                    drawGem(wx, wy, 0.78f, -time * 15f, 0.75f, 0.85f, 1.0f, 0.9f, 0.95f, 1.0f, a, 0.05f);
                }
            }
        }
    }

    /** Draw one crystal at world (x,y) with rotation and material. */
    private void drawGem(float x, float y, float scale, float spinDeg,
                         float r, float g, float b, float sr, float sg, float sb,
                         float alpha, float emissive) {
        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, 0f);
        Matrix.rotateM(model, 0, 18f, 1f, 0f, 0f);
        Matrix.rotateM(model, 0, spinDeg, 0f, 1f, 0f);
        Matrix.scaleM(model, 0, scale, scale, scale);
        Matrix.multiplyMM(mvp, 0, proj, 0, model, 0);

        boolean blend = alpha < 0.999f;
        if (blend) {
            GLES20.glEnable(GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glDepthMask(false);
        }
        GLES20.glUniformMatrix4fv(gMVP, 1, false, mvp, 0);
        GLES20.glUniformMatrix4fv(gModel, 1, false, model, 0);
        GLES20.glUniform3f(gColor, r, g, b);
        GLES20.glUniform3f(gSheen, sr, sg, sb);
        GLES20.glUniform1f(gAlpha, alpha);
        GLES20.glUniform1f(gEmissive, emissive);
        mesh.draw(gPos, gNorm);
        if (blend) {
            GLES20.glDepthMask(true);
            GLES20.glDisable(GLES20.GL_BLEND);
        }
    }
}
