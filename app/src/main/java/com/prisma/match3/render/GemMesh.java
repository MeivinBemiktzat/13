package com.prisma.match3.render;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * A procedurally generated, flat-shaded faceted crystal (a brilliant-cut style
 * bipyramid). Flat per-face normals give crisp facets that the lighting model
 * turns into sparkle, so no imported 3D art asset is required. Interleaved
 * layout: position.xyz, normal.xyz.
 */
public class GemMesh {
    private final FloatBuffer buffer;
    private final int vertexCount;
    private static final int STRIDE = 6 * 4;

    public GemMesh() {
        int ring = 6;
        float rTop = 0.62f, zTable = 0.10f; // ring sits just below the crown
        float topZ = 0.58f;                 // crown apex toward the camera
        float botZ = -0.78f;                // pavilion apex away from camera

        float[] top = {0, 0, topZ};
        float[] bot = {0, 0, botZ};
        float[][] rv = new float[ring][3];
        for (int i = 0; i < ring; i++) {
            double a = 2 * Math.PI * i / ring;
            rv[i] = new float[]{(float) Math.cos(a) * rTop, (float) Math.sin(a) * rTop, zTable};
        }

        float[] data = new float[ring * 2 * 3 * 6];
        int p = 0;
        for (int i = 0; i < ring; i++) {
            float[] v0 = rv[i], v1 = rv[(i + 1) % ring];
            p = face(data, p, top, v0, v1);   // crown facet
            p = face(data, p, bot, v1, v0);   // pavilion facet (reverse winding)
        }
        vertexCount = ring * 2 * 3;

        ByteBuffer bb = ByteBuffer.allocateDirect(data.length * 4).order(ByteOrder.nativeOrder());
        buffer = bb.asFloatBuffer();
        buffer.put(data).position(0);
    }

    private static int face(float[] d, int p, float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6f) len = 1f;
        nx /= len; ny /= len; nz /= len;
        p = vert(d, p, a, nx, ny, nz);
        p = vert(d, p, b, nx, ny, nz);
        p = vert(d, p, c, nx, ny, nz);
        return p;
    }

    private static int vert(float[] d, int p, float[] v, float nx, float ny, float nz) {
        d[p++] = v[0]; d[p++] = v[1]; d[p++] = v[2];
        d[p++] = nx;   d[p++] = ny;   d[p++] = nz;
        return p;
    }

    public void draw(int posLoc, int normLoc) {
        buffer.position(0);
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, STRIDE, buffer);
        GLES20.glEnableVertexAttribArray(posLoc);
        buffer.position(3);
        GLES20.glVertexAttribPointer(normLoc, 3, GLES20.GL_FLOAT, false, STRIDE, buffer);
        GLES20.glEnableVertexAttribArray(normLoc);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
    }
}
