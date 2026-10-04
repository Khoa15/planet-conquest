package com.planetconquest.game.text;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LanguageTest {
    @Test public void nextCyclesThroughAllLanguages() {
        assertEquals(Language.EN, Language.VI.next());
        assertEquals(Language.VI, Language.EN.next());
    }

    @Test public void fromTagFindsSupportedAndFallsBack() {
        assertEquals(Language.EN, Language.fromTag("en", Language.VI));
        assertEquals(Language.VI, Language.fromTag("vi", Language.EN));
        assertEquals(Language.EN, Language.fromTag(null, Language.EN));
        assertEquals(Language.VI, Language.fromTag("fr", Language.VI));
        assertEquals(Language.EN, Language.fromTag("fr", Language.DEFAULT));
    }

    @Test public void systemLanguageFollowsDeviceOrDefaults() {
        assertEquals(Language.EN, Language.forSystem("en"));
        assertEquals(Language.VI, Language.forSystem("vi"));
        assertEquals(Language.EN, Language.forSystem("ja"));
        assertEquals(Language.EN, Language.DEFAULT);
    }

    @Test public void tagsAndCodesAreUnique() {
        for (Language a : Language.values())
            for (Language b : Language.values())
                if (a != b) { assertEquals(false, a.tag.equals(b.tag)); assertEquals(false, a.code.equals(b.code)); }
    }
}
