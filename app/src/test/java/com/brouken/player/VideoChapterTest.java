package com.brouken.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.Label;
import androidx.media3.common.Metadata;
import androidx.media3.extractor.metadata.Chapter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VideoChapterTest {
    private static List<VideoChapter> chapters() {
        return VideoChapter.normalize(Arrays.asList(
                new VideoChapter(0, "Intro"),
                new VideoChapter(10000, "Middle"),
                new VideoChapter(20000, "End")), 30000);
    }

    @Test
    public void sortsAndDeduplicatesChaptersAcrossTracks() {
        List<VideoChapter> result = VideoChapter.normalize(Arrays.asList(
                new VideoChapter(20000, "End"),
                new VideoChapter(0, null),
                new VideoChapter(10000, "Middle"),
                new VideoChapter(0, " Intro ")), 30000);
        assertEquals(chapters(), result);
    }

    @Test
    public void excludesNegativeAndOutOfDurationStartTimes() {
        List<VideoChapter> result = VideoChapter.normalize(Arrays.asList(
                new VideoChapter(-1, "Invalid"),
                new VideoChapter(0, "Intro"),
                new VideoChapter(30000, "At end"),
                new VideoChapter(Long.MAX_VALUE, "Beyond end")), 30000);
        assertEquals(Collections.singletonList(new VideoChapter(0, "Intro")), result);
    }

    @Test
    public void retainsChaptersWhileDurationIsUnknown() {
        List<VideoChapter> result = VideoChapter.normalize(
                Collections.singletonList(new VideoChapter(10000, "Chapter")), -1);
        assertEquals(1, result.size());
    }

    @Test
    public void normalizesMissingTitles() {
        assertNull(new VideoChapter(0, "   ").title);
        assertNull(new VideoChapter(0, null).title);
        assertEquals("Intro", new VideoChapter(0, " Intro ").title);
    }

    @Test
    public void findsCurrentChapterAtExactBoundaries() {
        assertEquals(0, VideoChapter.currentIndex(chapters(), 0));
        assertEquals(0, VideoChapter.currentIndex(chapters(), 9999));
        assertEquals(1, VideoChapter.currentIndex(chapters(), 10000));
        assertEquals(2, VideoChapter.currentIndex(chapters(), 20000));
        assertEquals(2, VideoChapter.currentIndex(chapters(), 29999));
    }

    @Test
    public void previousAlwaysMeansPrecedingChapter() {
        assertEquals(0, VideoChapter.previousIndex(chapters(), 10000));
        assertEquals(0, VideoChapter.previousIndex(chapters(), 19000));
        assertEquals(1, VideoChapter.previousIndex(chapters(), 29999));
    }

    @Test
    public void nextSeeksToFollowingChapterAtAndBetweenBoundaries() {
        assertEquals(1, VideoChapter.nextIndex(chapters(), 0));
        assertEquals(1, VideoChapter.nextIndex(chapters(), 9999));
        assertEquals(2, VideoChapter.nextIndex(chapters(), 10000));
        assertEquals(2, VideoChapter.nextIndex(chapters(), 19999));
    }

    @Test
    public void doesNotWrapAtEitherEnd() {
        assertEquals(-1, VideoChapter.previousIndex(chapters(), 0));
        assertEquals(-1, VideoChapter.previousIndex(chapters(), 9000));
        assertEquals(-1, VideoChapter.nextIndex(chapters(), 20000));
        assertEquals(-1, VideoChapter.nextIndex(chapters(), 29999));
    }

    @Test
    public void handlesPlaybackBeforeFirstChapter() {
        List<VideoChapter> delayed = Collections.singletonList(new VideoChapter(5000, "Chapter"));
        assertEquals(-1, VideoChapter.currentIndex(delayed, 0));
        assertEquals(-1, VideoChapter.previousIndex(delayed, 0));
        assertEquals(0, VideoChapter.nextIndex(delayed, 0));
    }

    @Test
    public void handlesMissingAndSingleChapterMetadata() {
        assertEquals(-1, VideoChapter.currentIndex(Collections.emptyList(), 0));
        assertEquals(-1, VideoChapter.previousIndex(Collections.emptyList(), 0));
        assertEquals(-1, VideoChapter.nextIndex(Collections.emptyList(), 0));
        List<VideoChapter> single = Collections.singletonList(new VideoChapter(0, null));
        assertEquals(-1, VideoChapter.previousIndex(single, 15000));
        assertEquals(-1, VideoChapter.nextIndex(single, 15000));
        assertEquals(0, VideoChapter.currentIndex(single, 15000));
    }

    @Test
    public void navigationUsesChaptersInsideTheFile() {
        assertEquals(0, VideoChapter.navigationIndex(chapters(), 15000, false, true));
        assertEquals(2, VideoChapter.navigationIndex(chapters(), 15000, true, true));
    }

    @Test
    public void navigationFallsBackToFilesAtChapterBoundaries() {
        assertEquals(-1, VideoChapter.navigationIndex(chapters(), 0, false, true));
        assertEquals(-1, VideoChapter.navigationIndex(chapters(), 9000, false, true));
        assertEquals(-1, VideoChapter.navigationIndex(chapters(), 20000, true, true));
        assertEquals(-1, VideoChapter.navigationIndex(chapters(), 29999, true, true));
        assertEquals(0, VideoChapter.navigationIndex(
                Collections.singletonList(new VideoChapter(5000, "Intro")), 0, true, true));
    }

    @Test
    public void navigationFallsBackToFilesWithoutUsableChapters() {
        assertEquals(-1, VideoChapter.navigationIndex(Collections.emptyList(), 0, false, true));
        assertEquals(-1, VideoChapter.navigationIndex(Collections.emptyList(), 0, true, true));
        assertEquals(-1, VideoChapter.navigationIndex(chapters(), 15000, false, false));
        assertEquals(-1, VideoChapter.navigationIndex(chapters(), 15000, true, false));
        List<VideoChapter> single = Collections.singletonList(new VideoChapter(0, "Intro"));
        assertEquals(-1, VideoChapter.navigationIndex(single, 5000, false, true));
        assertEquals(-1, VideoChapter.navigationIndex(single, 5000, true, true));
    }

    @Test
    public void readsChapterMetadataAndExcludesHiddenEntries() {
        Metadata metadata = new Metadata(chapter(0, false), chapter(10000, true), chapter(20000, false));
        List<VideoChapter> result = VideoChapter.fromMetadata(metadata);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).startTimeMs);
        assertEquals(20000, result.get(1).startTimeMs);
        assertNull(result.get(0).title);
        assertTrue(VideoChapter.fromMetadata(null).isEmpty());
    }

    @Test
    public void readsNativeChapterTitle() {
        Chapter chapter = new Chapter.Builder()
                .setStartTimeMs(10000)
                .setEndTimeMs(20000)
                .setTitle(new Label(null, "Middle"))
                .build();
        assertEquals(Collections.singletonList(new VideoChapter(10000, "Middle")),
                VideoChapter.fromMetadata(new Metadata(chapter)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void normalizedChapterListCannotBeMutated() {
        chapters().clear();
    }

    private static Chapter chapter(long startTimeMs, boolean hidden) {
        return new Chapter() {
            @Override
            public long getStartTimeMs() { return startTimeMs; }
            @Override
            public long getEndTimeMs() { return startTimeMs + 10000; }
            @Override
            public boolean isHidden() { return hidden; }
            @Override
            public Label getTitle() { return null; }
        };
    }
}
