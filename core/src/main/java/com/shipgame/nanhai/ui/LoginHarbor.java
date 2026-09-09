package com.shipgame.nanhai.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** One masked water pass and six cloth sprites, all in the painting's coordinates. */
public final class LoginHarbor extends Group implements Disposable {
    private static final String ROOT = "textures/login/";
    private final Array<Texture> textures = new Array<>();
    private ShaderProgram seaShader;
    private float seaPhase;
    private boolean disposed;

    public LoginHarbor() {
        setName("loginHarbor");
        setTouchable(Touchable.disabled);
        try {
            JsonValue manifest = new JsonReader().parse(Gdx.files.internal(ROOT + "layers.json"));
            setSize(manifest.getFloat("width"), manifest.getFloat("height"));
            final Texture base = texture("harbor-base");
            final Texture mask = texture("sea-mask");
            seaShader = new ShaderProgram(Gdx.files.internal("shaders/login-sea.vert"),
                    Gdx.files.internal("shaders/login-sea.frag"));
            if (!seaShader.isCompiled()) {
                Gdx.app.error("LoginHarbor", "Sea shader unavailable: " + seaShader.getLog());
                seaShader.dispose();
                seaShader = null; // Keep the account UI and cloth animation usable on older drivers.
            }
            Actor water = new Actor() {
                @Override public void draw(Batch batch, float parentAlpha) {
                    ShaderProgram previous = batch.getShader();
                    float previousColor = batch.getPackedColor();
                    Color tint = getColor();
                    try {
                        if (seaShader != null) {
                            batch.flush();
                            mask.bind(1);
                            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
                            batch.setShader(seaShader);
                            seaShader.setUniformi("u_mask", 1);
                            seaShader.setUniformf("u_phase", seaPhase);
                        }
                        batch.setColor(tint.r, tint.g, tint.b, tint.a * parentAlpha);
                        batch.draw(base, 0, 0, getWidth(), getHeight());
                    } finally {
                        batch.setShader(previous); // Flush water before restoring the UI shader.
                        batch.setPackedColor(previousColor);
                    }
                }
            };
            water.setSize(getWidth(), getHeight());
            addActor(water);
            for (JsonValue layer : manifest.get("layers")) {
                Image cloth = new WindCloth(texture(layer.getString("name")), layer);
                // Manifest positions are top-left; Scene2D and pivots are bottom-left.
                cloth.setBounds(layer.getFloat("x"), getHeight() - layer.getFloat("y") - layer.getFloat("height"),
                        layer.getFloat("width"), layer.getFloat("height"));
                cloth.setOrigin(layer.getFloat("pivotX"), layer.getFloat("pivotY"));
                cloth.setName(layer.getString("name"));
                addActor(cloth);
            }
        } catch (RuntimeException failure) {
            dispose();
            throw failure;
        }
    }

    private Texture texture(String name) {
        Texture texture = new Texture(Gdx.files.internal(ROOT + name + ".png"));
        textures.add(texture);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    /** Match the existing full-viewport artwork layout, including extended aspect ratios. */
    public void fit(float width, float height) {
        setScale(width / getWidth(), height / getHeight());
    }

    @Override public void act(float delta) {
        // Clamp resume spikes and wrap phases to preserve precision in mobile GLSL.
        float step = MathUtils.clamp(delta, 0, .1f);
        seaPhase = (seaPhase + step * .35f) % MathUtils.PI2;
        super.act(step);
    }

    public boolean hasSeaShader() { return seaShader != null; }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        if (seaShader != null) { seaShader.dispose(); seaShader = null; }
        for (Texture texture : textures) texture.dispose();
        textures.clear();
        clearChildren();
    }

    private static final class WindCloth extends Image {
        private final float rotation, pulse, rate;
        private float phase;

        WindCloth(Texture texture, JsonValue layer) {
            super(texture);
            rotation = layer.getFloat("rotation");
            pulse = layer.getFloat("pulse");
            rate = layer.getFloat("rate");
            phase = layer.getFloat("phase");
            act(0);
        }

        @Override public void act(float delta) {
            super.act(delta);
            phase = (phase + delta * rate) % MathUtils.PI2;
            setRotation(MathUtils.sin(phase) * rotation);
            setScale(1 + MathUtils.sin(phase + .6f) * pulse,
                    1 + MathUtils.sin(phase) * pulse * .3f);
        }
    }
}
