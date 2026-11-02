package com.brouken.player;

import java.util.Locale;
import java.util.regex.Pattern;

final class SubtitleLanguagePreference {
    static final int SIMPLIFIED_CHINESE = 0;
    static final int TRADITIONAL_CHINESE = 1;
    static final int UNSPECIFIED_CHINESE = 2;
    static final int ENGLISH = 3;
    static final int UNMATCHED = Integer.MAX_VALUE;

    private static final Pattern SIMPLIFIED_LABEL = Pattern.compile(
            "\\b(chs|zh[-_]hans|gb2312|gb18030)\\b|简体|簡體|简中|簡中|简英|簡英");
    private static final Pattern TRADITIONAL_LABEL = Pattern.compile(
            "\\b(cht|zh[-_]hant|big5)\\b|繁体|繁體|繁中|繁英|正体|正體");
    private static final Pattern CHINESE_LABEL = Pattern.compile(
            "\\b(chinese|mandarin|cantonese|zh|zho|chi|cmn|yue)\\b|中文|汉语|漢語|中英");
    private static final Pattern ENGLISH_LABEL = Pattern.compile("\\b(english|eng|en)\\b|英文|英语|英語");
    private static final Pattern SIMPLIFIED_WORD = Pattern.compile("\\b(simplified|sc)\\b");
    private static final Pattern TRADITIONAL_WORD = Pattern.compile("\\b(traditional|tc)\\b");

    private SubtitleLanguagePreference() {}

    static int getPriority(String language, String label) {
        final String normalizedLanguage = language == null ? "" : language.trim().replace('_', '-').toLowerCase(Locale.ROOT);
        final String normalizedLabel = label == null ? "" : label.replace('_', '-').toLowerCase(Locale.ROOT);
        final String[] subtags = normalizedLanguage.split("-", -1);
        final String base = subtags[0];
        final boolean chinese = base.equals("zh") || base.equals("zho") || base.equals("chi")
                || base.equals("cmn") || base.equals("yue") || base.equals("chinese");

        if (chinese) {
            for (String subtag : subtags) {
                if (subtag.equals("hans") || subtag.equals("chs")) {
                    return SIMPLIFIED_CHINESE;
                }
                if (subtag.equals("hant") || subtag.equals("cht")) {
                    return TRADITIONAL_CHINESE;
                }
            }
        }

        if (base.equals("chs") || SIMPLIFIED_LABEL.matcher(normalizedLabel).find()) {
            return SIMPLIFIED_CHINESE;
        }
        if (base.equals("cht") || TRADITIONAL_LABEL.matcher(normalizedLabel).find()) {
            return TRADITIONAL_CHINESE;
        }

        final boolean chineseLabel = CHINESE_LABEL.matcher(normalizedLabel).find();
        if (chinese || chineseLabel) {
            if (SIMPLIFIED_WORD.matcher(normalizedLabel).find()) {
                return SIMPLIFIED_CHINESE;
            }
            if (TRADITIONAL_WORD.matcher(normalizedLabel).find()) {
                return TRADITIONAL_CHINESE;
            }
            if (chinese) {
                for (String subtag : subtags) {
                    if (subtag.equals("cn") || subtag.equals("sg")) {
                        return SIMPLIFIED_CHINESE;
                    }
                    if (subtag.equals("tw") || subtag.equals("hk") || subtag.equals("mo")) {
                        return TRADITIONAL_CHINESE;
                    }
                }
            }
            return UNSPECIFIED_CHINESE;
        }
        if (base.equals("en") || base.equals("eng") || ENGLISH_LABEL.matcher(normalizedLabel).find()) {
            return ENGLISH;
        }
        return UNMATCHED;
    }
}
