package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Disposable;

/**
 * Infinite clockwise rotating conic-gold border: a conic-gradient "light"
 * sweeps around the rectangle forever (angle-driven brightness, animated by
 * time). Pure cosmetics — touchable is disabled so the actor never blocks the
 * card's own clicks. Uses a tiny GLSL shader over a 1x1 white quad so it works
 * on both desktop and Android GLES2/3 through the stage's SpriteBatch.
 */
public final class RotatingGoldBorder extends Actor implements Disposable {
    private static final String VERT = "attribute vec4 a_position;\n"
            + "attribute vec4 a_color;\n"
            + "attribute vec2 a_texCoord0;\n"
            + "uniform mat4 u_projTrans;\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoord0;\n"
            + "void main(){\n"
            + "  v_color = a_color;\n"
            + "  v_texCoord0 = a_texCoord0;\n"
            + "  gl_Position = u_projTrans * a_position;\n"
            + "}";
    private static final String FRAG = "#ifdef GL_ES\n"
            + "precision mediump float;\n"
            + "#endif\n"
            + "varying vec4 v_color;\n"
            + "varying vec2 v_texCoord0;\n"
            + "uniform sampler2D u_texture;\n"
            + "uniform vec2 u_size;\n"
            + "uniform float u_time;\n"
            + "void main(){\n"
            + "  vec4 tex = texture2D(u_texture, v_texCoord0);\n"
            + "  vec2 p = v_texCoord0 * u_size;\n"
            + "  float d = min(min(p.x, p.y), min(u_size.x - p.x, u_size.y - p.y));\n"
            + "  float band = smoothstep(2.0, 4.0, d) * (1.0 - smoothstep(11.0, 13.0, d));\n"
            + "  vec2 c = u_size * 0.5;\n"
            + "  float ang = atan(p.y - c.y, p.x - c.x);\n"
            + "  float head = -u_time * 1.6; // decreasing angle = clockwise on screen\n"
            + "  float diff = ang - head;\n"
            + "  diff = mod(diff + 3.14159265, 6.2831853) - 3.14159265;\n"
            + "  float glow = pow(max(0.0, 1.0 - abs(diff) / 2.2), 2.0);\n"
            + "  float lit = 0.22 + 0.78 * glow;\n"
            + "  vec3 gold = mix(vec3(0.55, 0.42, 0.22), vec3(0.95, 0.80, 0.47), lit);\n"
            + "  gl_FragColor = vec4(gold, band * (0.55 + 0.45 * glow)) * tex.a;\n"
            + "}";

    private final ShaderProgram shader;
    private final Texture pixel;
    private float time;

    public RotatingGoldBorder() {
        shader = new ShaderProgram(VERT, FRAG);
        if (!shader.isCompiled()) {
            Gdx.app.error("RotatingGoldBorder", "shader compile failed: " + shader.getLog());
        }
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixel = new Texture(p);
        p.dispose();
        setTouchable(Touchable.disabled);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        time += delta; // continuous sweep: never resets, never blocks
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (!shader.isCompiled() || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        batch.flush(); // switch the batch to the border shader for one quad only
        ShaderProgram old = batch.getShader();
        batch.setShader(shader);
        shader.setUniformf("u_size", getWidth(), getHeight());
        shader.setUniformf("u_time", time);
        batch.draw(pixel, getX(), getY(), getWidth(), getHeight());
        batch.flush();
        batch.setShader(old);
    }

    @Override
    public void dispose() {
        shader.dispose();
        pixel.dispose();
    }
}