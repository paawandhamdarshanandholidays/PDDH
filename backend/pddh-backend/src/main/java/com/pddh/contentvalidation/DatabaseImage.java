package com.pddh.contentvalidation;

import java.util.Base64;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record DatabaseImage(String mediaType, byte[] bytes) {
    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    public static final int MAX_DATA_URL_LENGTH = ((MAX_FILE_SIZE_BYTES + 2) / 3) * 4 + 64;

    private static final Pattern DATA_URL = Pattern.compile(
            "^data:(image/(?:png|jpeg|gif|webp));base64,([A-Za-z0-9+/]*={0,2})$");
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "image/png", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a},
            "image/jpeg", new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff},
            "image/gif", new byte[] {'G', 'I', 'F', '8'},
            "image/webp", new byte[] {'R', 'I', 'F', 'F'});

    public static DatabaseImage decodeDataUrl(String value) {
        if (value == null || value.length() > MAX_DATA_URL_LENGTH) {
            throw new IllegalArgumentException("Image upload exceeds the 5 MB limit.");
        }
        Matcher matcher = DATA_URL.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Image upload must be a base64 PNG, JPEG, GIF, or WebP image.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Image upload is not valid base64.", exception);
        }
        String mediaType = matcher.group(1);
        if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE_BYTES
                || !mediaType.equals(detectMediaType(bytes))) {
            throw new IllegalArgumentException("Image upload is not a supported image or exceeds the 5 MB limit.");
        }
        return new DatabaseImage(mediaType, bytes);
    }

    public static String detectMediaType(byte[] bytes) {
        if (hasSignature(bytes, "image/png")) {
            return "image/png";
        }
        if (hasSignature(bytes, "image/jpeg")) {
            return "image/jpeg";
        }
        if (hasSignature(bytes, "image/gif") && bytes.length >= 6
                && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a') {
            return "image/gif";
        }
        if (hasSignature(bytes, "image/webp") && bytes.length >= 12
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static boolean hasSignature(byte[] bytes, String mediaType) {
        byte[] signature = SIGNATURES.get(mediaType);
        if (bytes == null || signature == null || bytes.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (bytes[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
