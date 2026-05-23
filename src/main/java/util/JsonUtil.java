package util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import server.model.Item;
import server.model.User;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class JsonUtil {

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Item.class, new ItemTypeAdapter())
            .registerTypeAdapter(User.class, new UserTypeAdapter())
            .create();

    // HÀM QUAN TRỌNG VỪA THÊM: Để Controller có thể lấy Gson đã cấu hình Adapter
    public static Gson getGson() {
        return gson;
    }

    public static <T> void saveToJson(String filePath, T data) throws IOException {
        String json = gson.toJson(data);
        Files.createDirectories(Paths.get(filePath).getParent());
        Files.write(Paths.get(filePath), json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        LoggerUtil.info("✓ Saved to JSON: " + filePath);
    }

    public static <T> T loadFromJson(String filePath, Class<T> clazz) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            LoggerUtil.warn("JSON file not found: " + filePath);
            return null;
        }

        String json = new String(Files.readAllBytes(Paths.get(filePath)), java.nio.charset.StandardCharsets.UTF_8);
        if (json.trim().isEmpty()) {
            return null;
        }

        return gson.fromJson(json, clazz);
    }

    /**
     * Load list từ JSON file
     */
    public static <T> List<T> loadListFromJson(String filePath, Class<T> clazz) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            LoggerUtil.warn("JSON file not found: " + filePath);
            return new ArrayList<>();
        }

        String json = new String(Files.readAllBytes(Paths.get(filePath)), java.nio.charset.StandardCharsets.UTF_8);
        if (json.trim().isEmpty() || json.trim().equals("[]")) {
            return new ArrayList<>();
        }

        Type listType = TypeToken.getParameterized(List.class, clazz).getType();
        return gson.fromJson(json, listType);
    }

    /**
     * Load Map từ JSON file
     */
    public static <K, V> Map<K, V> loadMapFromJson(String filePath, Class<K> keyType, Class<V> valueType) throws IOException {
        if (!Files.exists(Paths.get(filePath))) {
            LoggerUtil.warn("JSON file not found: " + filePath);
            return new HashMap<>();
        }

        String json = new String(Files.readAllBytes(Paths.get(filePath)), java.nio.charset.StandardCharsets.UTF_8);
        if (json.trim().isEmpty()) {
            return new HashMap<>();
        }

        Type mapType = TypeToken.getParameterized(Map.class, keyType, valueType).getType();
        return gson.fromJson(json, mapType);
    }

    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public static void createFileIfNotExists(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.write(path, "[]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            LoggerUtil.info("✓ Created JSON file: " + filePath);
        }
    }
}