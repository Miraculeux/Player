package com.brouken.player;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.extractor.text.CuesWithTiming;
import androidx.media3.extractor.text.SubtitleParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

final class SubtitleOffsetParserFactory implements SubtitleParser.Factory {
    static final long STEP_MS = 250L;
    private final SubtitleParser.Factory delegate;
    private final long offsetUs;
    final String externalSubtitleId;
    private volatile List<CuesWithTiming> externalCues = Collections.emptyList();

    SubtitleOffsetParserFactory(SubtitleParser.Factory delegate, long offsetMs) {
        this(delegate, offsetMs, null);
    }

    SubtitleOffsetParserFactory(SubtitleParser.Factory delegate, long offsetMs, String externalSubtitleId) {
        this.delegate = delegate;
        offsetUs = Math.multiplyExact(offsetMs, 1000L);
        this.externalSubtitleId = externalSubtitleId;
    }

    List<CuesWithTiming> getExternalCues() {
        return externalCues;
    }

    @Override
    public boolean supportsFormat(Format format) {
        return delegate.supportsFormat(format);
    }

    @Override
    public int getCueReplacementBehavior(Format format) {
        return delegate.getCueReplacementBehavior(format);
    }

    @Override
    public SubtitleParser create(Format format) {
        boolean ssa = format != null && MimeTypes.TEXT_SSA.equals(format.sampleMimeType);
        Format parserFormat = format;
        if (ssa && !format.initializationData.isEmpty()) {
            List<byte[]> initializationData = new ArrayList<>(format.initializationData.size());
            for (byte[] data : format.initializationData) {
                initializationData.add(SsaSubtitleSanitizer.sanitize(data, 0, data.length));
            }
            parserFormat = format.buildUpon().setInitializationData(initializationData).build();
        }
        SubtitleParser parser = delegate.create(parserFormat);
        return new SubtitleParser() {
            @Override
            public void parse(byte[] data, int offset, int length, OutputOptions options,
                    Consumer<CuesWithTiming> output) {
                List<CuesWithTiming> parsed = new ArrayList<>();
                byte[] sanitized = ssa ? SsaSubtitleSanitizer.sanitize(data, offset, length) : data;
                parser.parse(sanitized, sanitized == data ? offset : 0,
                        sanitized == data ? length : sanitized.length, OutputOptions.allCues(), parsed::add);
                List<CuesWithTiming> shifted = new ArrayList<>(parsed.size());
                for (CuesWithTiming cues : parsed) {
                    shifted.add(offsetUs == 0L ? cues : new CuesWithTiming(cues.cues,
                            (cues.startTimeUs == C.TIME_UNSET ? 0L : cues.startTimeUs) + offsetUs,
                            cues.durationUs));
                }
                if (externalSubtitleId != null && format != null && externalSubtitleId.equals(format.id)) {
                    externalCues = Collections.unmodifiableList(shifted);
                }
                int first = 0;
                if (options.startTimeUs != C.TIME_UNSET) {
                    long startTimeUs = options.startTimeUs - offsetUs;
                    while (first < parsed.size() && parsed.get(first).endTimeUs != C.TIME_UNSET
                            && parsed.get(first).endTimeUs <= startTimeUs) {
                        first++;
                    }
                }
                // Prioritize the seek position without clipping the active cue; retain earlier cues for live delays.
                for (int i = 0; i < parsed.size(); i++) {
                    output.accept(shifted.get((first + i) % shifted.size()));
                }
            }

            @Override
            public int getCueReplacementBehavior() {
                return parser.getCueReplacementBehavior();
            }

            @Override
            public void reset() {
                parser.reset();
            }
        };
    }
}
