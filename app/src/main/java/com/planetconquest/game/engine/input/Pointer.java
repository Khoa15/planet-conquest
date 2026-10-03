package com.planetconquest.game.engine.input;

import com.planetconquest.game.engine.model.Planet;
import com.planetconquest.game.engine.model.Selection;

import java.util.ArrayList;
import java.util.Arrays;

/** Trạng thái một ngón tay đang chạm: điểm đầu, điểm hiện tại, chế độ cử chỉ, đường khoanh và vùng chọn tạm. */
public final class Pointer {
    public float sx, sy, x, y, len, lockX, lockY;
    public Planet startPlanet, hover;
    public GestureMode mode = GestureMode.UNDECIDED;
    public final ArrayList<Planet> qsel = new ArrayList<Planet>();
    public float[] px = new float[256], py = new float[256];
    public int pn;
    public Selection live;

    void add(float x, float y) {
        if (pn == px.length) { px = Arrays.copyOf(px, pn * 2); py = Arrays.copyOf(py, pn * 2); }
        px[pn] = x; py[pn] = y; pn++;
    }
}
