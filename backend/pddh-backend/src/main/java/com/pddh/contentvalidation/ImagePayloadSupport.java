package com.pddh.contentvalidation;

import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

public final class ImagePayloadSupport {
    private ImagePayloadSupport() {
    }

    public static byte[] prepareForStorage(
            ObjectNode payload, String type, String id, byte[] existingImage) {
        String field = imageField(payload, type);
        if (!payload.has(field) || payload.get(field).isNull()) {
            if (existingImage != null) {
                payload.put(field, imagePath(type, id));
            }
            return existingImage;
        }
        String value = payload.path(field).isTextual() ? payload.path(field).asText() : "";
        if (value.startsWith("data:")) {
            DatabaseImage image = DatabaseImage.decodeDataUrl(value);
            payload.put(field, imagePath(type, id));
            return image.bytes();
        }
        if (existingImage != null && isStoredImageUrl(value, type, id)) {
            payload.put(field, imagePath(type, id));
            return existingImage;
        }
        return null;
    }

    public static Map<String, Object> response(
            Map<String, Object> content, String type, String id, boolean hasImage) {
        if (hasImage) {
            content.put(imageField(content, type), imagePath(type, id));
        }
        return content;
    }

    public static String imagePath(String type, String id) {
        String route = "stays".equals(type) ? "hotels" : type;
        return "/api/catalog/" + route + "/" + id + "/image";
    }

    private static String imageField(ObjectNode payload, String type) {
        return "gallery".equals(type) || payload.has("src") && !payload.has("imageUrl")
                ? "src"
                : payload.has("image") && !payload.has("imageUrl") ? "image" : "imageUrl";
    }

    private static String imageField(Map<String, Object> content, String type) {
        return "gallery".equals(type) || content.containsKey("src") && !content.containsKey("imageUrl")
                ? "src"
                : content.containsKey("image") && !content.containsKey("imageUrl") ? "image" : "imageUrl";
    }

    private static boolean isStoredImageUrl(String value, String type, String id) {
        try {
            URI uri = URI.create(value);
            String path = uri.getPath();
            return imagePath(type, id).equals(path)
                    && (uri.getScheme() == null || uri.getScheme().equalsIgnoreCase("http")
                            || uri.getScheme().equalsIgnoreCase("https"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
