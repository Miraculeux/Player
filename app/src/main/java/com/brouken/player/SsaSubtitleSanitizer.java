package com.brouken.player;

import androidx.media3.common.util.Log;
import androidx.media3.common.util.ParsableByteArray;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class SsaSubtitleSanitizer {
    static byte[] sanitize(byte[] data, int offset, int length) {
        ParsableByteArray input = new ParsableByteArray(data, offset + length);
        input.setPosition(offset);
        Charset charset = input.readUtfCharsetFromBom();
        if (charset == null) {
            charset = StandardCharsets.UTF_8;
        }
        StringBuilder sanitized = new StringBuilder(length);
        boolean scriptInfo = false;
        boolean changed = false;
        String line;
        while ((line = input.readLine(charset)) != null) {
            if (line.startsWith("[")) {
                scriptInfo = line.equalsIgnoreCase("[Script Info]");
            } else if (scriptInfo) {
                int colon = line.indexOf(':');
                if (colon >= 0) {
                    String key = line.substring(0, colon).trim();
                    if (key.equalsIgnoreCase("PlayResX") || key.equalsIgnoreCase("PlayResY")) {
                        if (!isValidResolution(line.substring(colon + 1).trim())) {
                            Log.w("SsaSubtitleSanitizer", "Ignoring invalid script resolution: " + line);
                            // Leaving the dimension unspecified lets Media3 use automatic sizing/placement.
                            sanitized.append(';');
                            changed = true;
                        }
                    }
                }
            }
            sanitized.append(line).append('\n');
        }
        return changed ? sanitized.toString().getBytes(StandardCharsets.UTF_8) : data;
    }

    private static boolean isValidResolution(String value) {
        try {
            float resolution = Float.parseFloat(value);
            return resolution > 0f && !Float.isInfinite(resolution);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
