package com.planetconquest.game.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import java.util.Random;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.engine.util.MathUtil.TAU;
import static com.planetconquest.game.engine.util.MathUtil.sin;

/** Nền chung của mọi màn hình: bitmap tinh vân + sao tĩnh, cộng vài ngôi sao nhấp nháy theo thời gian. */
public final class StarField {
    private final DrawKit kit;
    private Bitmap bg;
    private float[] tw = new float[0];

    public StarField(DrawKit kit) { this.kit = kit; }

    public void build() {
        float W = kit.w, H = kit.h, dp = kit.dp;
        if (W <= 0 || H <= 0) return;
        if (bg != null) bg.recycle();
        bg = Bitmap.createBitmap((int) W, (int) H, Bitmap.Config.ARGB_8888);
        Canvas g = new Canvas(bg);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, H, 0xFF070A1C, 0xFF04050D, Shader.TileMode.CLAMP));
        g.drawRect(0, 0, W, H, p);
        float[][] neb = {{.2f, .25f, .55f}, {.88f, .62f, .6f}, {.4f, .95f, .5f}};
        int[] nc = {0x5A3CC8, 0x288CC8, 0xC8468C};
        for (int i = 0; i < 3; i++) {
            p.setShader(new RadialGradient(neb[i][0] * W, neb[i][1] * H, neb[i][2] * Math.max(W, H), 0x26000000 | nc[i], nc[i], Shader.TileMode.CLAMP));
            g.drawRect(0, 0, W, H, p);
        }
        p.setShader(null);
        Random r = new Random(11);
        for (int i = 0; i < 170; i++) {
            p.setColor(Color.argb((int) ((.2f + r.nextFloat() * .6f) * 255), 255, 255, 255));
            g.drawCircle(r.nextFloat() * W, r.nextFloat() * H, (r.nextFloat() * 1.1f + .2f) * dp, p);
        }
        tw = new float[28 * 4];
        for (int i = 0; i < 28; i++) { tw[i * 4] = r.nextFloat() * W; tw[i * 4 + 1] = r.nextFloat() * H; tw[i * 4 + 2] = r.nextFloat() * TAU; tw[i * 4 + 3] = .8f + r.nextFloat() * 1.4f; }
    }

    public void draw(Canvas c, float t) {
        if (bg != null) c.drawBitmap(bg, 0, 0, null); else c.drawColor(Palette.C_BG);
        for (int i = 0; i < tw.length; i += 4) {
            kit.fill.setColor(alpha(0xFFFFFFFF, .25f + .35f * sin(t * tw[i + 3] + tw[i + 2])));
            c.drawRect(tw[i], tw[i + 1], tw[i] + 1.6f * kit.dp, tw[i + 1] + 1.6f * kit.dp, kit.fill);
        }
    }
}
