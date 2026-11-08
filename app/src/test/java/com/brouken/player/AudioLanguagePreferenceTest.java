package com.brouken.player;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class AudioLanguagePreferenceTest {
    @Test
    public void prefersEnglishThenChineseRegardlessOfDeviceLanguages() {
        String[] languages = Prefs.getPreferredAudioLanguages(
                Prefs.TRACK_ENGLISH_CHINESE, new String[]{"jpn", "zho"});
        assertArrayEquals(new String[]{"en", "zh"}, languages);
    }

    @Test
    public void fileDefaultDoesNotPreferAnyLanguage() {
        assertArrayEquals(new String[0],
                Prefs.getPreferredAudioLanguages(Prefs.TRACK_DEFAULT, new String[]{"eng"}));
    }

    @Test
    public void preservesDeviceLanguageOrder() {
        assertArrayEquals(new String[]{"jpn", "zho", "eng"},
                Prefs.getPreferredAudioLanguages(Prefs.TRACK_DEVICE,
                        new String[]{"jpn", "zho", "eng"}));
    }

    @Test
    public void preservesExplicitLanguagePreference() {
        assertArrayEquals(new String[]{"fra"},
                Prefs.getPreferredAudioLanguages("fra", new String[]{"eng", "zho"}));
    }
}
