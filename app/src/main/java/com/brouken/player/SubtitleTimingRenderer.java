package com.brouken.player;

import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.decoder.DecoderInputBuffer;
import androidx.media3.exoplayer.BaseRenderer;
import androidx.media3.exoplayer.ExoPlaybackException;
import androidx.media3.exoplayer.FormatHolder;
import androidx.media3.exoplayer.RendererCapabilities;
import androidx.media3.exoplayer.source.MediaSource.MediaPeriodId;
import androidx.media3.exoplayer.text.TextOutput;
import androidx.media3.extractor.text.CueDecoder;
import androidx.media3.extractor.text.CuesWithTiming;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

final class SubtitleTimingRenderer extends BaseRenderer {
    static final int MSG_SET_OFFSET = 10_001;
    private final TextOutput output;
    private final Handler outputHandler;
    private final CueDecoder decoder = new CueDecoder();
    private final DecoderInputBuffer input = new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_NORMAL);
    private final SubtitleCueHistory history = new SubtitleCueHistory();
    private final long initialOffsetUs;
    private final SubtitleOffsetParserFactory parserFactory;
    private List<CuesWithTiming> cachedExternalCues;
    private boolean externalTrack;
    private long offsetUs;
    private long positionUs;
    private int replacementBehavior;
    private boolean inputEnded;
    private boolean ended;
    private List<Cue> lastCues = Collections.emptyList();

    SubtitleTimingRenderer(TextOutput output, Looper outputLooper, long offsetMs,
            SubtitleOffsetParserFactory parserFactory) {
        super(C.TRACK_TYPE_TEXT);
        this.output = output;
        outputHandler = new Handler(outputLooper);
        offsetUs = initialOffsetUs = Math.multiplyExact(offsetMs, 1000L);
        this.parserFactory = parserFactory;
    }

    @Override
    public String getName() {
        return "SubtitleTimingRenderer";
    }

    @Override
    public int supportsFormat(Format format) {
        return RendererCapabilities.create(MimeTypes.APPLICATION_MEDIA3_CUES.equals(format.sampleMimeType)
                ? C.FORMAT_HANDLED : C.FORMAT_UNSUPPORTED_TYPE);
    }

    @Override
    protected void onStreamChanged(Format[] formats, long startPositionUs, long offsetUs,
            MediaPeriodId mediaPeriodId) {
        replacementBehavior = formats[0].cueReplacementBehavior;
        externalTrack = SubtitleUtils.matchesExternalSubtitleId(formats[0].id, parserFactory.externalSubtitleId);
        history.clear();
        cachedExternalCues = null;
        inputEnded = ended = false;
    }

    @Override
    protected void onPositionReset(long positionUs, boolean joining, boolean streamReset) {
        this.positionUs = positionUs;
        history.clear();
        cachedExternalCues = null;
        inputEnded = ended = false;
        publish(Collections.emptyList());
    }

    @Override
    public void handleMessage(int messageType, Object message) throws ExoPlaybackException {
        if (messageType == MSG_SET_OFFSET) {
            offsetUs = Math.multiplyExact((Long) message, 1000L);
            if (getState() != STATE_DISABLED) {
                render(positionUs, 0L);
            }
        } else {
            super.handleMessage(messageType, message);
        }
    }

    @Override
    public void render(long positionUs, long elapsedRealtimeUs) throws ExoPlaybackException {
        this.positionUs = positionUs;
        long subtitlePositionUs = positionUs - (offsetUs - initialOffsetUs);
        for (int i = 0; !inputEnded && i < 100; i++) {
            input.clear();
            FormatHolder holder = getFormatHolder();
            int result = readSource(holder, input, 0);
            if (result == C.RESULT_NOTHING_READ) {
                break;
            }
            if (result == C.RESULT_FORMAT_READ) {
                replacementBehavior = holder.format.cueReplacementBehavior;
            } else if (result == C.RESULT_BUFFER_READ) {
                if (input.isEndOfStream()) {
                    inputEnded = true;
                    break;
                }
                input.flip();
                ByteBuffer data = input.data;
                if (data == null) {
                    throw createRendererException(new IllegalStateException("Missing subtitle cue data"),
                            holder.format, PlaybackException.ERROR_CODE_DECODING_FAILED);
                }
                try {
                    CuesWithTiming cues = decoder.decode(input.timeUs, data.array(),
                            data.arrayOffset() + data.position(), data.remaining());
                    if (!externalTrack || parserFactory.getExternalCues().isEmpty()) {
                        history.add(cues);
                    }
                } catch (IllegalArgumentException e) {
                    throw createRendererException(e, holder.format, PlaybackException.ERROR_CODE_DECODING_FAILED);
                }
            }
        }
        if (externalTrack && cachedExternalCues != parserFactory.getExternalCues()) {
            cachedExternalCues = parserFactory.getExternalCues();
            if (!cachedExternalCues.isEmpty()) {
                history.clear();
                for (CuesWithTiming cues : cachedExternalCues) {
                    history.add(new CuesWithTiming(cues.cues,
                            cues.startTimeUs + getStreamOffsetUs(), cues.durationUs));
                }
            }
        }
        long streamEndUs = getStreamEndPositionUs();
        ended = streamEndUs != C.TIME_UNSET
                ? positionUs - getStreamOffsetUs() >= streamEndUs
                : inputEnded && subtitlePositionUs >= history.endTimeUs();
        publish(ended && streamEndUs != C.TIME_UNSET ? Collections.emptyList()
                : history.at(subtitlePositionUs, replacementBehavior));
    }

    private void publish(List<Cue> cues) {
        if (!cues.equals(lastCues)) {
            lastCues = cues;
            CueGroup group = new CueGroup(cues, Math.max(0L, positionUs - getStreamOffsetUs()));
            outputHandler.post(() -> output.onCues(group));
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isEnded() {
        return ended;
    }

    @Override
    protected void onDisabled() {
        outputHandler.removeCallbacksAndMessages(null);
        history.clear();
        publish(Collections.emptyList());
    }
}
