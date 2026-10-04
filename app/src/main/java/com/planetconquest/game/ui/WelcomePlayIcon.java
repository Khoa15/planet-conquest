package com.planetconquest.game.ui;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.planetconquest.game.engine.model.Faction;

import static com.planetconquest.game.engine.util.ColorUtil.alpha;
import static com.planetconquest.game.engine.util.ColorUtil.mix;
import static com.planetconquest.game.engine.util.MathUtil.sin;

/**
 * Biểu tượng play nằm đúng tâm hành tinh xanh của màn Welcome: lõi kính xanh ngọc đậm, tam giác trắng chuyển xanh nhạt
 * có quầng sáng, lõi thở nhẹ. Số liệu lấy từ planet-ui/welcome.js (PLAY_*).
 */
public final class WelcomePlayIcon extends Painter {
    private static final float ICON_H = .56f, DISC_R = .56f, DISC_ALPHA = .58f, PULSE = .03f;

    public WelcomePlayIcon(DrawKit kit) { super(kit); }

    private float cachedR = -1;
    private Shader discS, glowS, triS;

    /** Shader dựng ở gốc toạ độ, chỉ dựng lại khi bán kính đổi quá 0,25px (tránh cấp phát mỗi khung hình). */
    private void buildShaders(float R, int col, int light, int dark) {
        if (Math.abs(R - cachedR) < .25f) return;
        cachedR = R;
        float dr = R * DISC_R, h = R * ICON_H, w = h * .87f;
        discS = new RadialGradient(-dr * .25f, -dr * .3f, dr * 1.3f,
                new int[]{alpha(mix(col, 0xFF000000, .35f), DISC_ALPHA), alpha(dark, DISC_ALPHA + .12f)}, null, Shader.TileMode.CLAMP);
        glowS = new RadialGradient(0, 0, h * .95f, new int[]{alpha(col, .45f), alpha(col, 0)}, null, Shader.TileMode.CLAMP);
        triS = new LinearGradient(-w / 3, -h / 2, w * 2 / 3, h / 2, 0xFFFFFFFF, mix(light, 0xFFFFFFFF, .3f), Shader.TileMode.CLAMP);
    }

    /** (x, y) là tâm hành tinh, R bán kính trên màn hình, scale là hiệu ứng nhấn/chạm của nút. */
    public void draw(Canvas c, float x, float y, float R, float scale, float t) {
        int col = Faction.COLORS[0], light = Faction.LIGHT[0], dark = mix(col, 0xFF000000, .62f);
        buildShaders(R, col, light, dark);
        c.save();
        c.translate(x, y);
        c.scale(scale, scale);
        float dr = R * DISC_R * (1 + PULSE * sin(t * 2.4f));
        fill.setColor(0xFFFFFFFF);                           // paint dùng chung: đặt lại màu đục trước khi vẽ bằng shader
        fill.setShader(discS);
        c.drawCircle(0, 0, dr, fill);
        stroke.setShader(null); stroke.setPathEffect(null);
        stroke.setColor(alpha(light, .5f)); stroke.setStrokeWidth(1.5f * dp);
        c.drawCircle(0, 0, dr, stroke);

        float h = R * ICON_H, w = h * .87f, x0 = -w / 3, x1 = w * 2 / 3;   // tam giác có trọng tâm đúng tâm hành tinh
        fill.setShader(glowS);
        c.drawCircle(0, 0, h * .95f, fill);
        path.reset();
        path.moveTo(x0, -h / 2); path.lineTo(x0, h / 2); path.lineTo(x1, 0); path.close();
        fill.setShader(triS);
        c.drawPath(path, fill);
        fill.setShader(null);
        stroke.setColor(0xFFFFFFFF); stroke.setShader(triS); stroke.setStrokeWidth(h * .16f);
        c.drawPath(path, stroke);
        stroke.setShader(null);
        c.restore();
    }
}
