package com.planetconquest.game.text;

import java.util.Locale;

/** Ngôn ngữ hiển thị được hỗ trợ. Java thuần (không import android.*) để kiểm thử trên JVM. */
public enum Language {
    VI("vi", "VI"),
    EN("en", "EN");

    /** Ngôn ngữ dùng khi máy đặt ngôn ngữ không được hỗ trợ (khớp res/values/). */
    public static final Language DEFAULT = VI;

    public final String tag;
    /** Mã hai chữ hiển thị trên nút đổi ngôn ngữ. */
    public final String code;

    Language(String tag, String code) {
        this.tag = tag;
        this.code = code;
    }

    public Locale locale() { return new Locale(tag); }

    /** Ngôn ngữ kế tiếp theo vòng tròn: VI -> EN -> VI. */
    public Language next() { return values()[(ordinal() + 1) % values().length]; }

    /** Tra theo tag đã lưu; không khớp thì trả {@code fallback}. */
    public static Language fromTag(String tag, Language fallback) {
        for (Language l : values()) if (l.tag.equals(tag)) return l;
        return fallback;
    }

    /** Lần chạy đầu: theo ngôn ngữ của máy nếu được hỗ trợ, ngược lại dùng {@link #DEFAULT}. */
    public static Language forSystem(String systemLanguage) { return fromTag(systemLanguage, DEFAULT); }
}
