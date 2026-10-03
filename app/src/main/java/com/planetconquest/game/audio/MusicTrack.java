package com.planetconquest.game.audio;

import com.planetconquest.game.R;

/** Bài nhạc nền; NONE = im lặng. */
public enum MusicTrack {
    NONE(0), MENU(R.raw.bgm_menu), GAME(R.raw.bgm_game);

    final int res;

    MusicTrack(int res) { this.res = res; }
}
