package com.brouken.player;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SubtitleResumePreferenceTest {
    @Test
    public void initialSelectionUsesTheSameLanguageOrderAsThePlayer() {
        assertArrayEquals(new String[]{"zh-Hans", "zh-Hant", "zh", "en"},
                SubtitleLanguagePreference.getPreferredLanguages(null));
    }

    @Test
    public void systemCaptionLanguageRemainsTheFallback() {
        assertArrayEquals(new String[]{"zh-Hans", "zh-Hant", "zh", "en", "ja"},
                SubtitleLanguagePreference.getPreferredLanguages("ja"));
    }

    @Test
    public void doesNotAddEmptyOrDuplicateFallbackLanguages() {
        String[] preferred = {"zh-Hans", "zh-Hant", "zh", "en"};
        assertArrayEquals(preferred, SubtitleLanguagePreference.getPreferredLanguages(""));
        assertArrayEquals(preferred, SubtitleLanguagePreference.getPreferredLanguages("en"));
    }

    @Test
    public void repairsLegacyOffsetInducedOffWithoutRequiringAnExternalSubtitle() {
        assertTrue(Prefs.isImplicitSubtitleOff("#none", 250L, false));
        assertTrue(Prefs.isImplicitSubtitleOff("#none", -250L, false));
    }

    @Test
    public void preservesExplicitOffAndSelectedTracks() {
        assertFalse(Prefs.isImplicitSubtitleOff("#none", 250L, true));
        assertFalse(Prefs.isImplicitSubtitleOff("#none", 0L, false));
        assertFalse(Prefs.isImplicitSubtitleOff("4", 250L, false));
        assertFalse(Prefs.isImplicitSubtitleOff(null, 250L, false));
    }
}
