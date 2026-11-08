package com.brouken.player;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.ui.AspectRatioFrameLayout;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedHashMap;
import java.util.Set;

class Prefs {
    // Previously used
    // private static final String PREF_KEY_AUDIO_TRACK = "audioTrack";
    // private static final String PREF_KEY_AUDIO_TRACK_FFMPEG = "audioTrackFfmpeg";
    // private static final String PREF_KEY_SUBTITLE_TRACK = "subtitleTrack";

    private static final String PREF_KEY_MEDIA_URI = "mediaUri";
    private static final String PREF_KEY_MEDIA_TYPE = "mediaType";
    private static final String PREF_KEY_MEDIA_COMPLETED = "mediaCompleted";
    private static final String PREF_KEY_SUBTITLE_OFFSET_PREFIX = "subtitleOffset:";
    private static final String PREF_KEY_BRIGHTNESS = "brightness";
    private static final String PREF_KEY_FIRST_RUN = "firstRun";
    private static final String PREF_KEY_SUBTITLE_URI = "subtitleUri";

    private static final String PREF_KEY_AUDIO_TRACK_ID = "audioTrackId";
    private static final String PREF_KEY_SUBTITLE_TRACK_ID = "subtitleTrackId";
    private static final String PREF_KEY_SUBTITLE_OFF_EXPLICIT = "subtitleOffExplicit";
    private static final String PREF_KEY_RESIZE_MODE = "resizeMode";
    private static final String PREF_KEY_ORIENTATION = "orientation";
    private static final String PREF_KEY_SCALE = "scale";
    private static final String PREF_KEY_SCOPE_URI = "scopeUri";
    private static final String PREF_KEY_ASK_SCOPE = "askScope";
    private static final String PREF_KEY_AUTO_PIP = "autoPiP";
    private static final String PREF_KEY_TUNNELING = "tunneling";
    private static final String PREF_KEY_SKIP_SILENCE = "skipSilence";
    private static final String PREF_KEY_FRAMERATE_MATCHING = "frameRateMatching";
    private static final String PREF_KEY_REPEAT_TOGGLE = "repeatToggle";
    private static final String PREF_KEY_SPEED = "speed";
    private static final String PREF_KEY_FILE_ACCESS = "fileAccess";
    private static final String PREF_KEY_DECODER_PRIORITY = "decoderPriority";
    private static final String PREF_KEY_MAP_DV7 = "mapDV7ToHevc";
    private static final String PREF_KEY_LANGUAGE_AUDIO = "languageAudio";
    private static final String PREF_KEY_SUBTITLE_STYLE_EMBEDDED = "subtitleStyleEmbedded";
    private static final String PREF_KEY_SUBTITLE_STYLE_BOLD = "subtitleStyleBold";
    private static final String PREF_KEY_SUBTITLE_FONT_SIZE = "subtitleFontSize";

    public static final String TRACK_DEFAULT = "default";
    public static final String TRACK_DEVICE = "device";
    public static final String TRACK_ENGLISH_CHINESE = "english_chinese";

    final Context mContext;
    final SharedPreferences mSharedPreferences;

    public Uri mediaUri;
    public Uri subtitleUri;
    public Uri scopeUri;
    public String mediaType;
    public boolean mediaCompleted;
    public long subtitleOffsetMs;
    public int resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT;
    public Utils.Orientation orientation = Utils.Orientation.UNSPECIFIED;
    public float scale = 1.f;
    public float speed = 1.f;

    public String subtitleTrackId;
    public String audioTrackId;

    public int brightness = -1;
    public boolean firstRun = true;
    public boolean askScope = true;
    public boolean autoPiP = false;

    public boolean tunneling = false;
    public boolean skipSilence = false;
    public boolean frameRateMatching = false;
    public boolean repeatToggle = false;
    public String fileAccess = "auto";
    public int decoderPriority = DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON;
    public boolean mapDV7ToHevc = false;
    public String languageAudio = TRACK_ENGLISH_CHINESE;
    public boolean subtitleStyleEmbedded = true;
    public boolean subtitleStyleBold = false;
    public int subtitleFontSize = 0;

    private LinkedHashMap positions;

    public boolean persistentMode = true;
    public long nonPersitentPosition = -1L;

    public Prefs(Context context) {
        mContext = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        loadSavedPreferences();
        loadPositions();
        loadSubtitleOffset();
        if (isImplicitSubtitleOff(subtitleTrackId, subtitleOffsetMs,
                mSharedPreferences.contains(PREF_KEY_SUBTITLE_OFF_EXPLICIT))) {
            subtitleTrackId = null;
            mSharedPreferences.edit().remove(PREF_KEY_SUBTITLE_TRACK_ID).apply();
        }
    }

    private void loadSavedPreferences() {
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_URI))
            mediaUri = Uri.parse(mSharedPreferences.getString(PREF_KEY_MEDIA_URI, null));
        if (mSharedPreferences.contains(PREF_KEY_MEDIA_TYPE))
            mediaType = mSharedPreferences.getString(PREF_KEY_MEDIA_TYPE, null);
        mediaCompleted = mSharedPreferences.getBoolean(PREF_KEY_MEDIA_COMPLETED, false);
        brightness = mSharedPreferences.getInt(PREF_KEY_BRIGHTNESS, brightness);
        firstRun = mSharedPreferences.getBoolean(PREF_KEY_FIRST_RUN, firstRun);
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_URI))
            subtitleUri = Uri.parse(mSharedPreferences.getString(PREF_KEY_SUBTITLE_URI, null));
        if (mSharedPreferences.contains(PREF_KEY_AUDIO_TRACK_ID))
            audioTrackId = mSharedPreferences.getString(PREF_KEY_AUDIO_TRACK_ID, audioTrackId);
        if (mSharedPreferences.contains(PREF_KEY_SUBTITLE_TRACK_ID))
            subtitleTrackId = mSharedPreferences.getString(PREF_KEY_SUBTITLE_TRACK_ID, subtitleTrackId);
        if (mSharedPreferences.contains(PREF_KEY_RESIZE_MODE))
            resizeMode = mSharedPreferences.getInt(PREF_KEY_RESIZE_MODE, resizeMode);
        orientation = Utils.Orientation.values()[mSharedPreferences.getInt(PREF_KEY_ORIENTATION, orientation.value)];
        scale = mSharedPreferences.getFloat(PREF_KEY_SCALE, scale);
        if (mSharedPreferences.contains(PREF_KEY_SCOPE_URI))
            scopeUri = Uri.parse(mSharedPreferences.getString(PREF_KEY_SCOPE_URI, null));
        askScope = mSharedPreferences.getBoolean(PREF_KEY_ASK_SCOPE, askScope);
        speed = mSharedPreferences.getFloat(PREF_KEY_SPEED, speed);
        loadUserPreferences();
    }

    public void loadUserPreferences() {
        autoPiP = mSharedPreferences.getBoolean(PREF_KEY_AUTO_PIP, autoPiP);
        tunneling = mSharedPreferences.getBoolean(PREF_KEY_TUNNELING, tunneling);
        skipSilence = mSharedPreferences.getBoolean(PREF_KEY_SKIP_SILENCE, skipSilence);
        frameRateMatching = mSharedPreferences.getBoolean(PREF_KEY_FRAMERATE_MATCHING, frameRateMatching);
        repeatToggle = mSharedPreferences.getBoolean(PREF_KEY_REPEAT_TOGGLE, repeatToggle);
        fileAccess = mSharedPreferences.getString(PREF_KEY_FILE_ACCESS, fileAccess);
        decoderPriority = Integer.parseInt(mSharedPreferences.getString(PREF_KEY_DECODER_PRIORITY, String.valueOf(decoderPriority)));
        mapDV7ToHevc = mSharedPreferences.getBoolean(PREF_KEY_MAP_DV7, mapDV7ToHevc);
        languageAudio = mSharedPreferences.getString(PREF_KEY_LANGUAGE_AUDIO, languageAudio);
        subtitleStyleEmbedded = mSharedPreferences.getBoolean(PREF_KEY_SUBTITLE_STYLE_EMBEDDED, subtitleStyleEmbedded);
        subtitleStyleBold = mSharedPreferences.getBoolean(PREF_KEY_SUBTITLE_STYLE_BOLD, subtitleStyleBold);
        subtitleFontSize = Integer.parseInt(mSharedPreferences.getString(PREF_KEY_SUBTITLE_FONT_SIZE, "0"));
    }

    static String[] getPreferredAudioLanguages(String preference, String[] deviceLanguages) {
        switch (preference) {
            case TRACK_ENGLISH_CHINESE:
                return new String[]{"en", "zh"};
            case TRACK_DEVICE:
                return deviceLanguages;
            case TRACK_DEFAULT:
                return new String[0];
            default:
                return new String[]{preference};
        }
    }

    public void updateSubtitleFontSize(int sizePercent) {
        subtitleFontSize = sizePercent;
        mSharedPreferences.edit().putString(PREF_KEY_SUBTITLE_FONT_SIZE, Integer.toString(sizePercent)).apply();
    }

    public void updateMedia(final Context context, final Uri uri, final String type) {
        mediaUri = uri;
        mediaType = type;
        updateCompleted(false);
        loadSubtitleOffset();
        updateSubtitle(null);
        updateMeta(null, null, AspectRatioFrameLayout.RESIZE_MODE_FIT, 1.f, 1.f);

        if (mediaType != null && mediaType.endsWith("/*")) {
            mediaType = null;
        }

        if (mediaType == null) {
            if (ContentResolver.SCHEME_CONTENT.equals(mediaUri.getScheme())) {
                mediaType = context.getContentResolver().getType(mediaUri);
            }
        }

        if (persistentMode) {
            final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
            if (mediaUri == null)
                sharedPreferencesEditor.remove(PREF_KEY_MEDIA_URI);
            else
                sharedPreferencesEditor.putString(PREF_KEY_MEDIA_URI, mediaUri.toString());
            if (mediaType == null)
                sharedPreferencesEditor.remove(PREF_KEY_MEDIA_TYPE);
            else
                sharedPreferencesEditor.putString(PREF_KEY_MEDIA_TYPE, mediaType);
            sharedPreferencesEditor.apply();
        }
    }

    public void updateSubtitle(final Uri uri) {
        subtitleUri = uri;
        subtitleTrackId = null;
        if (persistentMode) {
            final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
            if (uri == null)
                sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_URI);
            else
                sharedPreferencesEditor.putString(PREF_KEY_SUBTITLE_URI, uri.toString());
            sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_TRACK_ID);
            sharedPreferencesEditor.apply();
        }
    }

    public void updatePosition(final long position) {
        if (mediaUri == null)
            return;

        while (positions.size() > 100)
            positions.remove(positions.keySet().toArray()[0]);

        if (persistentMode) {
            positions.put(mediaUri.toString(), position);
            savePositions();
        } else {
            nonPersitentPosition = position;
        }
    }

    public static boolean isUnfinishedPosition(long position, long duration, boolean completed) {
        return !completed && position > 0 && (duration <= 0 || position < duration);
    }

    static boolean isImplicitSubtitleOff(String trackId, long offsetMs, boolean explicitMarkerPresent) {
        return "#none".equals(trackId) && offsetMs != 0L && !explicitMarkerPresent;
    }

    public void updateCompleted(boolean completed) {
        mediaCompleted = completed;
        if (persistentMode) {
            mSharedPreferences.edit().putBoolean(PREF_KEY_MEDIA_COMPLETED, completed).apply();
        }
    }

    private void loadSubtitleOffset() {
        subtitleOffsetMs = persistentMode && mediaUri != null
                ? mSharedPreferences.getLong(PREF_KEY_SUBTITLE_OFFSET_PREFIX + mediaUri, 0L) : 0L;
    }

    public void updateSubtitleOffset(long offsetMs) {
        subtitleOffsetMs = offsetMs;
        if (persistentMode && mediaUri != null) {
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            String key = PREF_KEY_SUBTITLE_OFFSET_PREFIX + mediaUri;
            if (offsetMs == 0L) {
                editor.remove(key);
            } else {
                editor.putLong(key, offsetMs);
            }
            editor.apply();
        }
    }

    public void updateBrightness(final int brightness) {
        if (brightness >= -1) {
            this.brightness = brightness;
            final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
            sharedPreferencesEditor.putInt(PREF_KEY_BRIGHTNESS, brightness);
            sharedPreferencesEditor.apply();
        }
    }

    public void markFirstRun() {
        this.firstRun = false;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putBoolean(PREF_KEY_FIRST_RUN, false);
        sharedPreferencesEditor.apply();
    }

    public void markScopeAsked() {
        this.askScope = false;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putBoolean(PREF_KEY_ASK_SCOPE, false);
        sharedPreferencesEditor.apply();
    }

    private void savePositions() {
        try {
            FileOutputStream fos = mContext.openFileOutput("positions", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(positions);
            os.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPositions() {
        try {
            FileInputStream fis = mContext.openFileInput("positions");
            ObjectInputStream is = new ObjectInputStream(fis);
            positions = (LinkedHashMap) is.readObject();
            is.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            positions = new LinkedHashMap(10);
        }
    }

    public long getPosition() {
        if (!persistentMode) {
            return nonPersitentPosition;
        }

        Object val = positions.get(mediaUri.toString());
        if (val != null)
            return (long) val;

        // Return position for uri from limited scope (loaded after using Next action)
        if (ContentResolver.SCHEME_CONTENT.equals(mediaUri.getScheme())) {
            final String searchPath = SubtitleUtils.getTrailPathFromUri(mediaUri);
            if (searchPath == null || searchPath.length() < 1)
                return 0L;
            final Set<String> keySet = positions.keySet();
            final Object[] keys = keySet.toArray();
            for (int i = keys.length; i > 0; i--) {
                final String key = (String) keys[i - 1];
                final Uri uri = Uri.parse(key);
                if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                    final String keyPath = SubtitleUtils.getTrailPathFromUri(uri);
                    if (searchPath.equals(keyPath)) {
                        return (long) positions.get(key);
                    }
                }
            }
        }

        return 0L;
    }

    public void updateOrientation() {
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        sharedPreferencesEditor.putInt(PREF_KEY_ORIENTATION, orientation.value);
        sharedPreferencesEditor.apply();
    }

    public void updateMeta(final String audioTrackId, final String subtitleTrackId, final int resizeMode, final float scale, final float speed) {
        this.audioTrackId = audioTrackId;
        this.subtitleTrackId = subtitleTrackId;
        this.resizeMode = resizeMode;
        this.scale = scale;
        this.speed = speed;
        if (persistentMode) {
            final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
            if (audioTrackId == null)
                sharedPreferencesEditor.remove(PREF_KEY_AUDIO_TRACK_ID);
            else
                sharedPreferencesEditor.putString(PREF_KEY_AUDIO_TRACK_ID, audioTrackId);
            if (subtitleTrackId == null)
                sharedPreferencesEditor.remove(PREF_KEY_SUBTITLE_TRACK_ID);
            else
                sharedPreferencesEditor.putString(PREF_KEY_SUBTITLE_TRACK_ID, subtitleTrackId);
            sharedPreferencesEditor.putInt(PREF_KEY_RESIZE_MODE, resizeMode);
            sharedPreferencesEditor.putFloat(PREF_KEY_SCALE, scale);
            sharedPreferencesEditor.putFloat(PREF_KEY_SPEED, speed);
            sharedPreferencesEditor.putBoolean(PREF_KEY_SUBTITLE_OFF_EXPLICIT, "#none".equals(subtitleTrackId));
            sharedPreferencesEditor.apply();
        }
    }

    public void updateScope(final Uri uri) {
        scopeUri = uri;
        final SharedPreferences.Editor sharedPreferencesEditor = mSharedPreferences.edit();
        if (uri == null)
            sharedPreferencesEditor.remove(PREF_KEY_SCOPE_URI);
        else
            sharedPreferencesEditor.putString(PREF_KEY_SCOPE_URI, uri.toString());
        sharedPreferencesEditor.apply();
    }

    public void setPersistent(boolean persistentMode) {
        this.persistentMode = persistentMode;
    }
}