package com.prisma.match3.render;

import android.opengl.GLES20;

/** Minimal compile/link helper for a GLSL ES vertex+fragment program. */
public class ShaderProgram {
    public final int program;

    public ShaderProgram(String vs, String fs) {
        int v = compile(GLES20.GL_VERTEX_SHADER, vs);
        int f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, v);
        GLES20.glAttachShader(program, f);
        GLES20.glLinkProgram(program);
        int[] ok = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES20.glGetProgramInfoLog(program);
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Program link failed: " + log);
        }
        GLES20.glDeleteShader(v);
        GLES20.glDeleteShader(f);
    }

    public void use() { GLES20.glUseProgram(program); }

    public int attrib(String name) { return GLES20.glGetAttribLocation(program, name); }

    public int uniform(String name) { return GLES20.glGetUniformLocation(program, name); }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES20.glGetShaderInfoLog(s);
            GLES20.glDeleteShader(s);
            throw new RuntimeException("Shader compile failed: " + log + "\n" + src);
        }
        return s;
    }
}
