package com.brouken.player;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.text.Cue;
import androidx.media3.extractor.text.CuesWithTiming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class SubtitleCueHistory {
    private static final Comparator<CuesWithTiming> DISPLAY_ORDER =
            Comparator.comparingLong((CuesWithTiming group) -> group.startTimeUs)
                    .thenComparing(Comparator.comparingLong((CuesWithTiming group) -> group.durationUs).reversed());
    private final List<CuesWithTiming> groups = new ArrayList<>();

    void add(CuesWithTiming cues) {
        int index = Collections.binarySearch(groups, cues, DISPLAY_ORDER);
        groups.add(index < 0 ? -index - 1 : index, cues);
    }

    List<Cue> at(long timeUs, int replacementBehavior) {
        List<Cue> result = new ArrayList<>();
        for (CuesWithTiming group : groups) {
            if (group.startTimeUs > timeUs) {
                break;
            }
            if (replacementBehavior == Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE) {
                result.clear();
            }
            if (group.durationUs == C.TIME_UNSET || timeUs < group.endTimeUs) {
                result.addAll(group.cues);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : result;
    }

    long endTimeUs() {
        long endUs = 0L;
        for (CuesWithTiming group : groups) {
            endUs = Math.max(endUs, group.durationUs == C.TIME_UNSET ? group.startTimeUs : group.endTimeUs);
        }
        return endUs;
    }

    void clear() {
        groups.clear();
    }
}
