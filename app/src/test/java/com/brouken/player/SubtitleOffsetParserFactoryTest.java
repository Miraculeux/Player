package com.brouken.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.SubtitleParser;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.function.Consumer;

public class SubtitleOffsetParserFactoryTest {
    private SubtitleParser.OutputOptions receivedOptions;
    private boolean reset;
    private long cueStartUs = 12_000_000L;
    private List<CuesWithTiming> cueInput;
    private final SubtitleParser parser = new SubtitleParser() {
        @Override
        public void parse(byte[] data, int offset, int length, OutputOptions options,
                Consumer<CuesWithTiming> output) {
            receivedOptions = options;
            if (cueInput != null) {
                cueInput.forEach(output);
                return;
            }
            long start = cueStartUs == C.TIME_UNSET ? C.TIME_UNSET : Math.max(cueStartUs, options.startTimeUs);
            output.accept(new CuesWithTiming(Collections.emptyList(), start, 2_000_000L));
        }

        @Override
        public int getCueReplacementBehavior() {
            return Format.CUE_REPLACEMENT_BEHAVIOR_REPLACE;
        }

        @Override
        public void reset() {
            reset = true;
        }
    };
    private final SubtitleParser.Factory factory = new SubtitleParser.Factory() {
        @Override
        public boolean supportsFormat(Format format) {
            return true;
        }

        @Override
        public int getCueReplacementBehavior(Format format) {
            return parser.getCueReplacementBehavior();
        }

        @Override
        public SubtitleParser create(Format format) {
            return parser;
        }
    };

    private CuesWithTiming parse(long offsetMs, SubtitleParser.OutputOptions options) {
        List<CuesWithTiming> result = new ArrayList<>();
        new SubtitleOffsetParserFactory(factory, offsetMs).create(null)
                .parse(new byte[0], options, result::add);
        assertEquals(1, result.size());
        return result.get(0);
    }

    @Test
    public void stepIsExactlyQuarterOfASecond() {
        assertEquals(250L, SubtitleOffsetParserFactory.STEP_MS);
    }

    @Test
    public void positiveOffsetDelaysCueAndPreservesDuration() {
        CuesWithTiming cues = parse(250L, SubtitleParser.OutputOptions.allCues());
        assertEquals(12_250_000L, cues.startTimeUs);
        assertEquals(2_000_000L, cues.durationUs);
        assertEquals(14_250_000L, cues.endTimeUs);
        assertEquals(C.TIME_UNSET, receivedOptions.startTimeUs);
        assertSame(SubtitleParser.OutputOptions.allCues(), receivedOptions);
    }

    @Test
    public void negativeOffsetAdvancesCueIncludingBeforeVideoStart() {
        assertEquals(11_750_000L, parse(-250L, SubtitleParser.OutputOptions.allCues()).startTimeUs);
        cueStartUs = 0L;
        assertEquals(-250_000L, parse(-250L, SubtitleParser.OutputOptions.allCues()).startTimeUs);
    }

    @Test
    public void seekingDoesNotClipTheCueNeededByLiveOffsets() {
        assertEquals(12_750_000L, parse(750L,
                SubtitleParser.OutputOptions.onlyCuesAfter(12_500_000L)).startTimeUs);
        assertSame(SubtitleParser.OutputOptions.allCues(), receivedOptions);
        assertEquals(11_750_000L, parse(-250L,
                SubtitleParser.OutputOptions.cuesAfterThenRemainingCuesBefore(12_500_000L)).startTimeUs);
    }

    @Test
    public void implicitSampleRelativeCueTimesBecomeRelativeOffsets() {
        cueStartUs = C.TIME_UNSET;
        assertEquals(250_000L, parse(250L, SubtitleParser.OutputOptions.allCues()).startTimeUs);
        assertEquals(-250_000L, parse(-250L, SubtitleParser.OutputOptions.allCues()).startTimeUs);
    }

    @Test
    public void zeroOffsetKeepsOriginalCueTimesAndResetIsForwarded() {
        assertEquals(cueStartUs, parse(0L, SubtitleParser.OutputOptions.onlyCuesAfter(12_500_000L)).startTimeUs);
        assertSame(SubtitleParser.OutputOptions.allCues(), receivedOptions);
        SubtitleParser.Factory shifted = new SubtitleOffsetParserFactory(factory, 250L);
        assertTrue(shifted.supportsFormat(null));
        assertEquals(parser.getCueReplacementBehavior(), shifted.getCueReplacementBehavior(null));
        SubtitleParser wrapped = shifted.create(null);
        assertEquals(parser.getCueReplacementBehavior(), wrapped.getCueReplacementBehavior());
        wrapped.reset();
        assertTrue(reset);
    }

    @Test
    public void retainsCompleteExternalTimelineForDelayingAfterASeek() {
        Format format = new Format.Builder().setId("external-ass").build();
        SubtitleOffsetParserFactory shifted = new SubtitleOffsetParserFactory(factory, 250L, format.id);
        List<CuesWithTiming> result = new ArrayList<>();
        shifted.create(format).parse(new byte[0], SubtitleParser.OutputOptions.onlyCuesAfter(12_500_000L),
                result::add);
        assertEquals(12_250_000L, shifted.getExternalCues().get(0).startTimeUs);
        assertEquals(14_250_000L, shifted.getExternalCues().get(0).endTimeUs);
        assertSame(shifted.getExternalCues().get(0), result.get(0));
    }

    @Test
    public void prioritizesActiveCueWithoutDiscardingTheEarlierTimeline() {
        cueInput = Arrays.asList(
                new CuesWithTiming(Collections.emptyList(), 0L, 10_000_000L),
                new CuesWithTiming(Collections.emptyList(), 10_000_000L, 10_000_000L),
                new CuesWithTiming(Collections.emptyList(), 20_000_000L, 10_000_000L));
        Format format = new Format.Builder().setId("external-ass").build();
        SubtitleOffsetParserFactory shifted = new SubtitleOffsetParserFactory(factory, 0L, format.id);
        List<CuesWithTiming> result = new ArrayList<>();
        shifted.create(format).parse(new byte[0], SubtitleParser.OutputOptions.onlyCuesAfter(20_500_000L),
                result::add);
        assertEquals(20_000_000L, result.get(0).startTimeUs);
        assertEquals(0L, result.get(1).startTimeUs);
        assertEquals(10_000_000L, result.get(2).startTimeUs);
        assertEquals(cueInput, shifted.getExternalCues());
    }

    @Test
    public void doesNotUseAnEmbeddedTrackAsTheExternalTimeline() {
        Format format = new Format.Builder().setId("embedded").build();
        SubtitleOffsetParserFactory shifted = new SubtitleOffsetParserFactory(factory, 250L, "external-ass");
        List<CuesWithTiming> result = new ArrayList<>();
        shifted.create(format).parse(new byte[0], SubtitleParser.OutputOptions.allCues(), result::add);
        assertTrue(shifted.getExternalCues().isEmpty());
        assertEquals(12_250_000L, result.get(0).startTimeUs);
    }
}
