package com.brouken.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import androidx.media3.common.MimeTypes;
import androidx.media3.common.text.Cue;

import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class SubtitleUtilsTest {
    @Test
    public void automaticFontSizePreservesExistingSystemScaling() {
        assertEquals(SubtitleUtils.normalizeFontScale(1.5f, true),
                SubtitleUtils.getFontScale(1.5f, true, 0), 0f);
        assertEquals(SubtitleUtils.normalizeFontScale(2f, false),
                SubtitleUtils.getFontScale(2f, false, 0), 0f);
    }

    @Test
    public void customFontSizeDoesNotFollowSystemFontChanges() {
        assertEquals(SubtitleUtils.getFontScale(1f, true, 150),
                SubtitleUtils.getFontScale(2f, true, 150), 0f);
        assertEquals(SubtitleUtils.getFontScale(0.5f, false, 125),
                SubtitleUtils.getFontScale(1.5f, false, 125), 0f);
    }

    @Test
    public void customFontSizesAreExactPercentagesOfNormalSize() {
        float normal = SubtitleUtils.getFontScale(1f, true, 100);
        assertEquals(normal * 0.5f, SubtitleUtils.getFontScale(1f, true, 50), 0.00001f);
        assertEquals(normal * 1.5f, SubtitleUtils.getFontScale(1f, true, 150), 0.00001f);
        assertEquals(normal * 3f, SubtitleUtils.getFontScale(1f, true, 300), 0.00001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNegativeFontSizes() {
        SubtitleUtils.getFontScale(1f, true, -1);
    }

    @Test
    public void removesSubtitleWindowWithoutChangingTextOrPosition() {
        Cue original = new Cue.Builder()
                .setText("Subtitle")
                .setWindowColor(0xFF000000)
                .setLine(0.8f, Cue.LINE_TYPE_FRACTION)
                .setPosition(0.4f)
                .setSize(0.7f)
                .build();
        Cue cleaned = SubtitleUtils.withoutBackgrounds(Collections.singletonList(original)).get(0);
        assertFalse(cleaned.windowColorSet);
        assertEquals(original.text, cleaned.text);
        assertEquals(original.line, cleaned.line, 0f);
        assertEquals(original.position, cleaned.position, 0f);
        assertEquals(original.size, cleaned.size, 0f);
        assertTrue(original.windowColorSet);
    }

    @Test
    public void retainsCuesThatAlreadyHaveNoBackground() {
        Cue original = new Cue.Builder().setText("Subtitle").build();
        List<Cue> cleaned = SubtitleUtils.withoutBackgrounds(Collections.singletonList(original));
        assertEquals(1, cleaned.size());
        assertSame(original, cleaned.get(0));
        assertTrue(SubtitleUtils.withoutBackgrounds(Collections.emptyList()).isEmpty());
    }

    @Test
    public void detectsAssAndSsaWithoutCaseSensitivity() {
        assertEquals(MimeTypes.TEXT_SSA, SubtitleUtils.getSubtitleMime("movie.ass"));
        assertEquals(MimeTypes.TEXT_SSA, SubtitleUtils.getSubtitleMime("movie.ASS"));
        assertEquals(MimeTypes.TEXT_SSA, SubtitleUtils.getSubtitleMime("movie.SsA"));
    }

    @Test
    public void detectsOtherSupportedSubtitleFormats() {
        assertEquals(MimeTypes.TEXT_VTT, SubtitleUtils.getSubtitleMime("movie.VTT"));
        assertEquals(MimeTypes.APPLICATION_TTML, SubtitleUtils.getSubtitleMime("movie.TTML"));
        assertEquals(MimeTypes.APPLICATION_TTML, SubtitleUtils.getSubtitleMime("movie.XML"));
        assertEquals(MimeTypes.APPLICATION_TTML, SubtitleUtils.getSubtitleMime("movie.DFXP"));
        assertEquals(MimeTypes.APPLICATION_SUBRIP, SubtitleUtils.getSubtitleMime("movie.SRT"));
    }

    @Test
    public void matchesExternalSubtitleIdsAfterMediaSourceMerging() {
        String id = "file:///storage/ABCD-1234/Movies/subtitle.ASS";
        assertTrue(SubtitleUtils.matchesExternalSubtitleId(id, id));
        assertTrue(SubtitleUtils.matchesExternalSubtitleId("1:" + id, id));
        assertTrue(SubtitleUtils.matchesExternalSubtitleId("2:1:" + id, id));
        assertFalse(SubtitleUtils.matchesExternalSubtitleId(null, id));
        assertFalse(SubtitleUtils.matchesExternalSubtitleId(id, null));
        assertFalse(SubtitleUtils.matchesExternalSubtitleId("1", id));
        assertFalse(SubtitleUtils.matchesExternalSubtitleId("1:file:///other/subtitle.ASS", id));
    }
}
