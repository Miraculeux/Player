package com.brouken.player;

import androidx.media3.common.Label;
import androidx.media3.common.Metadata;
import androidx.media3.extractor.metadata.Chapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

final class VideoChapter {
    final long startTimeMs;
    final String title;

    VideoChapter(long startTimeMs, String title) {
        this.startTimeMs = startTimeMs;
        this.title = title == null || title.trim().isEmpty() ? null : title.trim();
    }

    static List<VideoChapter> fromMetadata(Metadata metadata) {
        List<VideoChapter> chapters = new ArrayList<>();
        if (metadata != null) {
            for (int i = 0; i < metadata.length(); i++) {
                if (metadata.get(i) instanceof Chapter) {
                    Chapter chapter = (Chapter) metadata.get(i);
                    if (!chapter.isHidden()) {
                        Label title = chapter.getTitle();
                        chapters.add(new VideoChapter(chapter.getStartTimeMs(), title == null ? null : title.value));
                    }
                }
            }
        }
        return chapters;
    }

    static List<VideoChapter> normalize(List<VideoChapter> chapters, long durationMs) {
        TreeMap<Long, VideoChapter> sorted = new TreeMap<>();
        for (VideoChapter chapter : chapters) {
            if (chapter.startTimeMs < 0 || (durationMs > 0 && chapter.startTimeMs >= durationMs)) {
                continue;
            }
            VideoChapter existing = sorted.get(chapter.startTimeMs);
            if (existing == null || (existing.title == null && chapter.title != null)) {
                sorted.put(chapter.startTimeMs, chapter);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(sorted.values()));
    }

    static int currentIndex(List<VideoChapter> chapters, long positionMs) {
        int low = 0;
        int high = chapters.size() - 1;
        int current = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (chapters.get(middle).startTimeMs <= positionMs) {
                current = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return current;
    }

    static int previousIndex(List<VideoChapter> chapters, long positionMs) {
        return Math.max(-1, currentIndex(chapters, positionMs) - 1);
    }

    static int nextIndex(List<VideoChapter> chapters, long positionMs) {
        int next = currentIndex(chapters, positionMs) + 1;
        return next < chapters.size() ? next : -1;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VideoChapter)) {
            return false;
        }
        VideoChapter chapter = (VideoChapter) other;
        return startTimeMs == chapter.startTimeMs && Objects.equals(title, chapter.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTimeMs, title);
    }
}
