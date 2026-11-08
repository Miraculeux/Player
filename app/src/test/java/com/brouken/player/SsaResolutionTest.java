package com.brouken.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.text.Cue;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.DefaultSubtitleParserFactory;
import androidx.media3.extractor.text.SubtitleParser;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class SsaResolutionTest {
    private static final String HEADER = "[Script Info]\nPlayResX: 0\nPlayResY: 0\n\n"
            + "[V4+ Styles]\n"
            + "Format: Name, Fontname, Fontsize, PrimaryColour, Bold, Alignment\n"
            + "Style: Default,Arial,20,&H00FFFFFF,-1,2\n\n"
            + "[Events]\n";
    private static final String EVENT_FORMAT = "Format: Layer, Start, End, Style, Text\n";
    private static final String EVENT = "Dialogue: 0,0:00:20.93,0:00:22.02,Default,First\\NSecond\n";
    private static final Format FORMAT = new Format.Builder().setSampleMimeType(MimeTypes.TEXT_SSA).build();

    private List<CuesWithTiming> parse(SubtitleParser parser, byte[] data) {
        List<CuesWithTiming> result = new ArrayList<>();
        parser.parse(data, SubtitleParser.OutputOptions.allCues(), result::add);
        return result;
    }

    @Test
    public void upstreamZeroResolutionProducesInfiniteFontSize() {
        List<CuesWithTiming> result = parse(new DefaultSubtitleParserFactory().create(FORMAT),
                (HEADER + EVENT_FORMAT + EVENT).getBytes(StandardCharsets.UTF_8));
        assertFalse(result.isEmpty());
        assertTrue(Float.isInfinite(result.get(0).cues.get(0).textSize));
    }

    @Test
    public void zeroResolutionUsesAutomaticFontSizeAndRetainsTextAndTiming() {
        List<CuesWithTiming> result = parse(new SubtitleOffsetParserFactory(
                new DefaultSubtitleParserFactory(), 0L).create(FORMAT),
                ("\uFEFF" + HEADER + EVENT_FORMAT + EVENT).getBytes(StandardCharsets.UTF_8));
        assertFalse(result.isEmpty());
        Cue cue = result.get(0).cues.get(0);
        assertEquals(Cue.DIMEN_UNSET, cue.textSize, 0f);
        assertEquals("First\nSecond", cue.text.toString());
        assertEquals(20_930_000L, result.get(0).startTimeUs);
        assertEquals(1_090_000L, result.get(0).durationUs);
        assertEquals(0.5f, cue.position, 0f);
        assertEquals(0.95f, cue.line, 0f);
        Spanned styled = (Spanned) cue.text;
        assertEquals(1, styled.getSpans(0, styled.length(), ForegroundColorSpan.class).length);
        assertEquals(1, styled.getSpans(0, styled.length(), StyleSpan.class).length);
    }

    @Test
    public void validResolutionPreservesFontSizeAndExplicitPosition() {
        String text = HEADER.replace("PlayResX: 0", "PlayResX: 384")
                .replace("PlayResY: 0", "PlayResY: 288") + EVENT_FORMAT
                + EVENT.replace("First", "{\\pos(192,250)}First");
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        assertSame(data, SsaSubtitleSanitizer.sanitize(data, 0, data.length));
        Cue cue = parse(new SubtitleOffsetParserFactory(new DefaultSubtitleParserFactory(), 0L)
                .create(FORMAT), data).get(0).cues.get(0);
        assertEquals(20f / 288f, cue.textSize, 0f);
        assertEquals(0.5f, cue.position, 0f);
        assertEquals(250f / 288f, cue.line, 0f);
    }

    @Test
    public void invalidDimensionsCannotProduceInvalidSizingOrPositions() {
        for (String value : Arrays.asList("0", "-1", "NaN", "Infinity", "1e100", "invalid")) {
            String text = HEADER.replace(": 0", ": " + value) + EVENT_FORMAT
                    + EVENT.replace("First", "{\\pos(192,250)}First");
            Cue cue = parse(new SubtitleOffsetParserFactory(new DefaultSubtitleParserFactory(), 0L)
                    .create(FORMAT), text.getBytes(StandardCharsets.UTF_8)).get(0).cues.get(0);
            assertEquals(value, Cue.DIMEN_UNSET, cue.textSize, 0f);
            assertEquals(value, 0.5f, cue.position, 0f);
            assertEquals(value, 0.95f, cue.line, 0f);
        }
    }

    @Test
    public void embeddedHeadersAndOffsetsUseTheSameCompatibilityFix() {
        Format embedded = FORMAT.buildUpon().setInitializationData(Arrays.asList(
                EVENT_FORMAT.trim().getBytes(StandardCharsets.UTF_8),
                HEADER.getBytes(StandardCharsets.UTF_8))).build();
        CuesWithTiming result = parse(new SubtitleOffsetParserFactory(new DefaultSubtitleParserFactory(), 250L)
                .create(embedded), EVENT.getBytes(StandardCharsets.UTF_8)).get(0);
        assertEquals(Cue.DIMEN_UNSET, result.cues.get(0).textSize, 0f);
        assertEquals(21_180_000L, result.startTimeUs);
        assertEquals(1_090_000L, result.durationUs);
    }

    @Test
    public void utf16BomAndSampleByteRangesAreSupported() {
        for (byte[] content : Arrays.asList(
                ("\uFEFF" + HEADER + EVENT_FORMAT + EVENT).getBytes(StandardCharsets.UTF_16LE),
                ("\uFEFF" + HEADER + EVENT_FORMAT + EVENT).getBytes(StandardCharsets.UTF_16BE),
                (HEADER + EVENT_FORMAT + EVENT).getBytes(StandardCharsets.UTF_8))) {
            byte[] data = new byte[content.length + 8];
            Arrays.fill(data, (byte) 'x');
            System.arraycopy(content, 0, data, 3, content.length);
            List<CuesWithTiming> result = new ArrayList<>();
            new SubtitleOffsetParserFactory(new DefaultSubtitleParserFactory(), 0L).create(FORMAT)
                    .parse(data, 3, content.length, SubtitleParser.OutputOptions.allCues(), result::add);
            assertEquals("First\nSecond", result.get(0).cues.get(0).text.toString());
            assertEquals(Cue.DIMEN_UNSET, result.get(0).cues.get(0).textSize, 0f);
        }
    }

    @Test
    public void doesNotModifyResolutionLikeTextOutsideScriptInfo() {
        byte[] data = ("[Events]\nPlayResY: 0\n").getBytes(StandardCharsets.UTF_8);
        assertSame(data, SsaSubtitleSanitizer.sanitize(data, 0, data.length));
    }

    @Test
    public void validHeightStillControlsFontSizeWhenOnlyWidthIsInvalid() {
        String text = HEADER.replace("PlayResY: 0", "PlayResY: 288") + EVENT_FORMAT + EVENT;
        Cue cue = parse(new SubtitleOffsetParserFactory(new DefaultSubtitleParserFactory(), 0L)
                .create(FORMAT), text.getBytes(StandardCharsets.UTF_8)).get(0).cues.get(0);
        assertEquals(20f / 288f, cue.textSize, 0f);
    }
}
