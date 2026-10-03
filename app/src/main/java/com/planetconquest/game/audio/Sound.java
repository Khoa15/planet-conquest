package com.planetconquest.game.audio;

import com.planetconquest.game.R;

/** Hiệu ứng âm thanh ngắn: tài nguyên và âm lượng gắn liền với từng tiếng. */
public enum Sound {
    CLICK(R.raw.sfx_click, .6f), LASSO(R.raw.sfx_lasso, .5f), MOVE(R.raw.sfx_move, .5f), POINT(R.raw.sfx_point, .5f),
    ATTACK(R.raw.sfx_attack, .45f), UPGRADE(R.raw.sfx_upgrade, .7f), CAPTURE(R.raw.sfx_capture, .8f),
    LEVELUP(R.raw.sfx_levelup, .8f), WIN(R.raw.sfx_win, .9f), LOSE(R.raw.sfx_lose, .9f);

    final int res;
    final float volume;

    Sound(int res, float volume) { this.res = res; this.volume = volume; }
}
