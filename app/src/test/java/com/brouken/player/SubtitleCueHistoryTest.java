package com.brouken.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.text.Cue;
import androidx.media3.extractor.text.CuesWithTiming;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class SubtitleCueHistoryTest {
    private static final int MERGE = Format.CUE_REPLACEMENT_BEHAVIOR_MERGE;
    private static final int REPLACE = Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE;

    private CuesWithTiming group(String text, long start, long duration) {
        return new CuesWithTiming(Collections.singletonList(new Cue.Builder().setText(text).build()),
                start, duration);
    }

    @Test
    public void liveOffsetsCanRevisitPreviouslyDisplayedCues() {
        SubtitleCueHistory history = new SubtitleCueHistory();
        CuesWithTiming first = group("First", 0L, 1_000_000L);
        CuesWithTiming second = group("Second", 1_000_000L, 1_000_000L);
        history.add(first);
        history.add(second);
        assertEquals(second.cues, history.at(1_100_000L, MERGE));
        assertEquals(first.cues, history.at(1_100_000L - 250_000L, MERGE));
        assertEquals(second.cues, history.at(900_000L + 250_000L, MERGE));
    }

    @Test
    public void mergesOverlappingCuesInTimeOrder() {
        SubtitleCueHistory history = new SubtitleCueHistory();
        CuesWithTiming first = group("First", 0L, 2_000L);
        CuesWithTiming second = group("Second", 1_000L, 2_000L);
        history.add(second);
        history.add(first);
        assertEquals(Arrays.asList(first.cues.get(0), second.cues.get(0)), history.at(1_500L, MERGE));
        assertEquals(second.cues, history.at(2_000L, MERGE));
    }

    @Test
    public void replacingCuesDoNotReviveAnOlderCueAfterNewestExpires() {
        SubtitleCueHistory history = new SubtitleCueHistory();
        CuesWithTiming first = group("First", 0L, 10_000L);
        CuesWithTiming second = group("Second", 1_000L, 1_000L);
        history.add(first);
        history.add(second);
        assertEquals(second.cues, history.at(1_500L, REPLACE));
        assertTrue(history.at(2_000L, REPLACE).isEmpty());
    }

    @Test
    public void unknownDurationRemainsUntilReplacedAndClearRemovesOldTrack() {
        SubtitleCueHistory history = new SubtitleCueHistory();
        CuesWithTiming first = group("Bitmap", 0L, C.TIME_UNSET);
        history.add(first);
        assertEquals(first.cues, history.at(100_000L, REPLACE));
        history.add(new CuesWithTiming(Collections.emptyList(), 200_000L, C.TIME_UNSET));
        assertTrue(history.at(200_000L, REPLACE).isEmpty());
        history.clear();
        assertTrue(history.at(0L, MERGE).isEmpty());
        assertEquals(0L, history.endTimeUs());
    }
}
