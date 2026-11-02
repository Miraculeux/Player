package com.brouken.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SubtitleLanguagePreferenceTest {
    @Test
    public void recognizesSimplifiedLanguageTags() {
        for (String language : new String[]{"zh-Hans", "zh-CN", "zh-SG", "ZH_hans_CN", "chi-Hans", "zho-CN", "cmn-Hans", "chs", "zh-CHS"}) {
            assertEquals(language, SubtitleLanguagePreference.SIMPLIFIED_CHINESE,
                    SubtitleLanguagePreference.getPriority(language, null));
        }
    }

    @Test
    public void recognizesTraditionalLanguageTags() {
        for (String language : new String[]{"zh-Hant", "zh-TW", "zh-HK", "zh-MO", "ZH_hant_TW", "chi-Hant", "zho-TW", "cmn-Hant", "yue-HK", "cht", "zh-CHT"}) {
            assertEquals(language, SubtitleLanguagePreference.TRADITIONAL_CHINESE,
                    SubtitleLanguagePreference.getPriority(language, null));
        }
    }

    @Test
    public void distinguishesChineseScriptsFromTrackLabels() {
        for (String label : new String[]{"简体中文", "簡體中文", "简中", "简英双语", "CHS", "CHS_ENG", "Chinese (Simplified)", "Chinese SC", "GB2312", "zh-Hans"}) {
            assertEquals(label, SubtitleLanguagePreference.SIMPLIFIED_CHINESE,
                    SubtitleLanguagePreference.getPriority("zh", label));
        }
        for (String label : new String[]{"繁体中文", "繁體中文", "繁中", "繁英雙語", "正體中文", "CHT", "CHT_ENG", "Chinese (Traditional)", "Chinese TC", "BIG5", "zh-Hant"}) {
            assertEquals(label, SubtitleLanguagePreference.TRADITIONAL_CHINESE,
                    SubtitleLanguagePreference.getPriority("zh", label));
        }
    }

    @Test
    public void usesLabelsWhenLanguageIsMissing() {
        assertEquals(SubtitleLanguagePreference.SIMPLIFIED_CHINESE,
                SubtitleLanguagePreference.getPriority(null, "简体中文"));
        assertEquals(SubtitleLanguagePreference.TRADITIONAL_CHINESE,
                SubtitleLanguagePreference.getPriority("und", "Chinese (Traditional)"));
        assertEquals(SubtitleLanguagePreference.UNSPECIFIED_CHINESE,
                SubtitleLanguagePreference.getPriority(null, "中文"));
        assertEquals(SubtitleLanguagePreference.ENGLISH,
                SubtitleLanguagePreference.getPriority(null, "English [SDH]"));
    }

    @Test
    public void explicitScriptTakesPrecedenceOverRegionAndLabel() {
        assertEquals(SubtitleLanguagePreference.TRADITIONAL_CHINESE,
                SubtitleLanguagePreference.getPriority("zh-Hant-CN", "简体中文"));
        assertEquals(SubtitleLanguagePreference.SIMPLIFIED_CHINESE,
                SubtitleLanguagePreference.getPriority("zh-Hans-TW", "繁體中文"));
    }

    @Test
    public void scriptLabelTakesPrecedenceOverRegion() {
        assertEquals(SubtitleLanguagePreference.TRADITIONAL_CHINESE,
                SubtitleLanguagePreference.getPriority("zh-CN", "繁體中文"));
        assertEquals(SubtitleLanguagePreference.SIMPLIFIED_CHINESE,
                SubtitleLanguagePreference.getPriority("zh-TW", "简体中文"));
    }

    @Test
    public void genericChineseHasItsOwnFallbackPriority() {
        for (String language : new String[]{"zh", "zho", "chi", "cmn", "yue"}) {
            assertEquals(language, SubtitleLanguagePreference.UNSPECIFIED_CHINESE,
                    SubtitleLanguagePreference.getPriority(language, null));
        }
        assertTrue(SubtitleLanguagePreference.SIMPLIFIED_CHINESE < SubtitleLanguagePreference.TRADITIONAL_CHINESE);
        assertTrue(SubtitleLanguagePreference.TRADITIONAL_CHINESE < SubtitleLanguagePreference.UNSPECIFIED_CHINESE);
        assertTrue(SubtitleLanguagePreference.UNSPECIFIED_CHINESE < SubtitleLanguagePreference.ENGLISH);
    }

    @Test
    public void recognizesEnglishAndDoesNotTreatItsRegionAsChinese() {
        for (String language : new String[]{"en", "eng", "en-US", "en-GB", "en-CN"}) {
            assertEquals(language, SubtitleLanguagePreference.ENGLISH,
                    SubtitleLanguagePreference.getPriority(language, null));
        }
        assertEquals(SubtitleLanguagePreference.ENGLISH,
                SubtitleLanguagePreference.getPriority(null, "英文"));
    }

    @Test
    public void leavesUnknownLanguagesAndUnrelatedLabelsUnmatched() {
        assertEquals(SubtitleLanguagePreference.UNMATCHED,
                SubtitleLanguagePreference.getPriority(null, null));
        assertEquals(SubtitleLanguagePreference.UNMATCHED,
                SubtitleLanguagePreference.getPriority("-", null));
        assertEquals(SubtitleLanguagePreference.UNMATCHED,
                SubtitleLanguagePreference.getPriority("ja", "日本語"));
        assertEquals(SubtitleLanguagePreference.UNMATCHED,
                SubtitleLanguagePreference.getPriority("und", "Orchestra"));
        assertEquals(SubtitleLanguagePreference.UNMATCHED,
                SubtitleLanguagePreference.getPriority("fr", "French"));
    }
}
