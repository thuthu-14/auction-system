package server.service;

import java.util.Map;

public final class RequestPayloadUtil {
    private RequestPayloadUtil() {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> objectMap(Object data) {
        if (data instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new IllegalArgumentException("Dữ liệu request không hợp lệ");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> stringMap(Object data) {
        if (data instanceof Map<?, ?> map) {
            return (Map<String, String>) map;
        }
        throw new IllegalArgumentException("Dữ liệu request không hợp lệ");
    }

    public static String requiredAuctionId(Object data) {
        Object auctionId = data instanceof Map<?, ?> payload ? payload.get("auctionId") : data;
        String value = text(auctionId);
        if (value == null || "--".equals(value) || "null".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
        }
        return value;
    }

    public static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    public static double requiredDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            return Double.parseDouble(value.toString().trim());
        }
        throw new IllegalArgumentException("Thiếu dữ liệu số: " + key);
    }

    public static double optionalDouble(Map<String, Object> data, String key, double fallback) {
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            return Double.parseDouble(value.toString().trim());
        }
        return fallback;
    }

    public static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }
}
