package com.brouken.player;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;

import org.junit.Test;

public class PlaybackResumeTest {
    @Test
    public void resumesOnlyPositiveUnfinishedPositions() {
        assertTrue(Prefs.isUnfinishedPosition(1L, 10_000L, false));
        assertTrue(Prefs.isUnfinishedPosition(9_999L, 10_000L, false));
        assertFalse(Prefs.isUnfinishedPosition(0L, 10_000L, false));
        assertFalse(Prefs.isUnfinishedPosition(-1L, 10_000L, false));
    }

    @Test
    public void doesNotResumeCompletedOrOutOfRangeHistory() {
        assertFalse(Prefs.isUnfinishedPosition(5_000L, 10_000L, true));
        assertFalse(Prefs.isUnfinishedPosition(10_000L, 10_000L, false));
        assertFalse(Prefs.isUnfinishedPosition(11_000L, 10_000L, false));
    }

    @Test
    public void acceptsUnknownDurationUnlessMarkedCompleted() {
        assertTrue(Prefs.isUnfinishedPosition(5_000L, C.TIME_UNSET, false));
        assertFalse(Prefs.isUnfinishedPosition(5_000L, C.TIME_UNSET, true));
    }
}
