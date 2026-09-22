package com.prisma.match3.render;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * Additive GL_POINTS particle burst used for match/detonation sparkle. Particles
 * are pooled and recycled; the buffer is rebuilt each frame from the live set.
 * Layout per particle: pos.xyz, color.rgba, size.
 */
public class ParticleSystem {
    private static final int MAX = 900;
    private static final int FLOATS = 8;

    private final float[] px = new float[MAX], py = new float[MAX], pz = new float[MAX];
    private final float[] vx = new float[MAX], vy = new float[MAX], vz = new float[MAX];
    private final float[] life = new float[MAX], maxLife = new float[MAX];
    private final float[] cr = new float[MAX], cg = new float[MAX], cb = new float[MAX];
    private final float[] size = new float[MAX];
    private int cursor;

    private final Random rnd = new Random();
    private ShaderProgram prog;
    private FloatBuffer buf;
    private int aPos, aColor, aSize, uMVP;

    private static final String VS =
            "uniform mat4 uMVP;\n" +
            "attribute vec3 aPos;\n" +
            "attribute vec4 aColor;\n" +
            "attribute float aSize;\n" +
            "varying vec4 vColor;\n" +
            "void main(){ gl_Position = uMVP * vec4(aPos,1.0); gl_PointSize = aSize; vColor = aColor; }";

    private static final String FS =
            "precision mediump float;\n" +
            "varying vec4 vColor;\n" +
            "void main(){\n" +
            "  vec2 d = gl_PointCoord - vec2(0.5);\n" +
            "  float r = length(d);\n" +
            "  float a = smoothstep(0.5, 0.0, r);\n" +
            "  gl_FragColor = vec4(vColor.rgb, vColor.a * a);\n" +
            "}";

    public void init() {
        prog = new ShaderProgram(VS, FS);
        aPos = prog.attrib("aPos");
        aColor = prog.attrib("aColor");
        aSize = prog.attrib("aSize");
        uMVP = prog.uniform("uMVP");
        ByteBuffer bb = ByteBuffer.allocateDirect(MAX * FLOATS * 4).order(ByteOrder.nativeOrder());
        buf = bb.asFloatBuffer();
    }

    public void burst(float x, float y, float r, float g, float b, int count) {
        for (int i = 0; i < count; i++) {
            int idx = cursor;
            cursor = (cursor + 1) % MAX;
            px[idx] = x; py[idx] = y; pz[idx] = 0.4f;
            double a = rnd.nextDouble() * Math.PI * 2;
            float sp = 1.6f + rnd.nextFloat() * 3.2f;
            vx[idx] = (float) Math.cos(a) * sp;
            vy[idx] = (float) Math.sin(a) * sp;
            vz[idx] = 0.4f + rnd.nextFloat() * 1.4f;
            maxLife[idx] = life[idx] = 0.45f + rnd.nextFloat() * 0.5f;
            cr[idx] = r; cg[idx] = g; cb[idx] = b;
            size[idx] = 10f + rnd.nextFloat() * 18f;
        }
    }

    public void update(float dt) {
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            life[i] -= dt;
            px[i] += vx[i] * dt;
            py[i] += vy[i] * dt;
            pz[i] += vz[i] * dt;
            vx[i] *= 0.90f; vy[i] *= 0.90f;
            vz[i] -= 4.0f * dt; // gentle pull back into the board plane
        }
    }

    public void draw(float[] mvp) {
        buf.position(0);
        int n = 0;
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) continue;
            float t = life[i] / maxLife[i];
            buf.put(px[i]).put(py[i]).put(pz[i]);
            buf.put(cr[i]).put(cg[i]).put(cb[i]).put(t);
            buf.put(size[i] * (0.4f + 0.6f * t));
            n++;
        }
        if (n == 0) return;
        buf.position(0);

        prog.use();
        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvp, 0);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE); // additive glow
        GLES20.glDepthMask(false);

        buf.position(0);
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, FLOATS * 4, buf);
        GLES20.glEnableVertexAttribArray(aPos);
        buf.position(3);
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, FLOATS * 4, buf);
        GLES20.glEnableVertexAttribArray(aColor);
        buf.position(7);
        GLES20.glVertexAttribPointer(aSize, 1, GLES20.GL_FLOAT, false, FLOATS * 4, buf);
        GLES20.glEnableVertexAttribArray(aSize);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, n);

        GLES20.glDepthMask(true);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }
}
