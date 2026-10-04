package com.planetconquest.game.data;

import android.content.SharedPreferences;

import com.planetconquest.game.text.Language;

import java.util.Locale;

/** Ngôn ngữ người chơi đã chọn, lưu trong SharedPreferences. Chưa chọn thì theo ngôn ngữ của máy. */
public final class LanguageStore {
    private static final String K_LANG = "lang";

    private final SharedPreferences prefs;
    private Language current;

    public LanguageStore(SharedPreferences prefs) {
        this.prefs = prefs;
        current = Language.fromTag(prefs.getString(K_LANG, null), Language.forSystem(Locale.getDefault().getLanguage()));
    }

    public Language get() { return current; }

    public void set(Language language) {
        current = language;
        prefs.edit().putString(K_LANG, language.tag).apply();
    }
}
