package com.brouken.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.MimeTypes;

import org.junit.Test;

public class SubtitleUtilsTest {
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
