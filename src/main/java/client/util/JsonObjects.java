package client.util;

import com.google.gson.JsonObject;

public final class JsonObjects {
    private JsonObjects() {
    }

    public static String getString(JsonObject object, String key, String defaultValue) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString()
                : defaultValue;
    }

    public static int getInt(JsonObject object, String key, int defaultValue) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsInt()
                : defaultValue;
    }

    public static double getDouble(JsonObject object, String key, double defaultValue) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsDouble()
                : defaultValue;
    }

    public static JsonObject getObject(JsonObject object, String key, JsonObject defaultValue) {
        return object != null && object.has(key) && object.get(key).isJsonObject()
                ? object.getAsJsonObject(key)
                : defaultValue;
    }
}
