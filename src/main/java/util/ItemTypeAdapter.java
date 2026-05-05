package util;

import com.google.gson.*;
import server.model.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ItemTypeAdapter implements JsonSerializer<Item>, JsonDeserializer<Item> {

    @Override
    public JsonElement serialize(Item src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        // --- THUỘC TÍNH CHUNG CỦA ITEM ---
        jsonObject.addProperty("itemId", src.getItemId());
        jsonObject.addProperty("name", src.getName());
        jsonObject.addProperty("description", src.getDescription());
        jsonObject.addProperty("startingPrice", src.getStartingPrice());
        jsonObject.addProperty("sellerId", src.getSellerId());

        // QUAN TRỌNG: Thêm danh sách ảnh vào JSON
        jsonObject.add("images", context.serialize(src.getImages()));

        // --- THUỘC TÍNH RIÊNG THEO LOẠI ---
        if (src instanceof Electronics) {
            jsonObject.addProperty("type", "ELECTRONICS");
            Electronics e = (Electronics) src;
            jsonObject.addProperty("brand", e.getBrand());
            jsonObject.addProperty("warrantyPeriod", e.getWarrantyPeriod());
        } else if (src instanceof Art) {
            jsonObject.addProperty("type", "ART");
            Art a = (Art) src;
            jsonObject.addProperty("creator", a.getCreator());
            jsonObject.addProperty("material", a.getMaterial());
        } else if (src instanceof Vehicle) {
            jsonObject.addProperty("type", "VEHICLE");
            Vehicle v = (Vehicle) src;
            jsonObject.addProperty("model", v.getModel());
            jsonObject.addProperty("odometer", v.getOdometer());
        } else if (src instanceof Fashion) {
            jsonObject.addProperty("type", "FASHION");
            Fashion f = (Fashion) src;
            jsonObject.addProperty("brand", f.getBrand());
            jsonObject.addProperty("material", f.getMaterial());
        } else if (src instanceof Jewelry) {
            jsonObject.addProperty("type", "JEWELRY");
            Jewelry j = (Jewelry) src;
            jsonObject.addProperty("material", j.getMaterial());
            jsonObject.addProperty("weight", j.getWeight());
        }

        return jsonObject;
    }

    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // Lấy type an toàn
        String type = getStringSafe(jsonObject, "type", "UNKNOWN");

        // Lấy các thuộc tính chung AN TOÀN (Không bao giờ bị lỗi null nữa)
        String itemId = getStringSafe(jsonObject, "itemId", "");
        String name = getStringSafe(jsonObject, "name", "No Name");
        String description = getStringSafe(jsonObject, "description", "");
        double startingPrice = getDoubleSafe(jsonObject, "startingPrice", 0.0);
        String sellerId = getStringSafe(jsonObject, "sellerId", "");

        // Lấy lại list ảnh an toàn
        List<String> images = new ArrayList<>();
        if (jsonObject.has("images") && !jsonObject.get("images").isJsonNull()) {
            List<String> parsedImages = context.deserialize(jsonObject.get("images"), List.class);
            if (parsedImages != null) {
                images = parsedImages;
            }
        }

        switch (type) {
            case "ELECTRONICS":
                return new Electronics(itemId, name, description, startingPrice, sellerId,
                        getStringSafe(jsonObject, "brand", ""),
                        getStringSafe(jsonObject, "warrantyPeriod", ""), images);
            case "ART":
                return new Art(itemId, name, description, startingPrice, sellerId,
                        getStringSafe(jsonObject, "creator", ""),
                        getStringSafe(jsonObject, "material", ""), images);
            case "VEHICLE":
                return new Vehicle(itemId, name, description, startingPrice, sellerId,
                        getStringSafe(jsonObject, "model", ""),
                        getIntSafe(jsonObject, "odometer", 0), images);
            case "FASHION":
                return new Fashion(itemId, name, description, startingPrice, sellerId,
                        getStringSafe(jsonObject, "brand", ""),
                        getStringSafe(jsonObject, "material", ""), images);
            case "JEWELRY":
                return new Jewelry(itemId, name, description, startingPrice, sellerId,
                        getStringSafe(jsonObject, "material", ""),
                        getDoubleSafe(jsonObject, "weight", 0.0), images);
            default:
                // ← THAY THROW THÀNH TRỊ MẶC ĐỊNH
                System.err.println("⚠️ Unknown item type: " + type + ", creating default Fashion item");
                return new Fashion(itemId, name, description, startingPrice, sellerId, "", "", images);
        }
    }

    // --- CÁC HÀM HỖ TRỢ LẤY DỮ LIỆU AN TOÀN ---

    private String getStringSafe(JsonObject obj, String key, String defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            return obj.get(key).getAsString();
        }
        return defaultValue;
    }

    private double getDoubleSafe(JsonObject obj, String key, double defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsDouble();
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private int getIntSafe(JsonObject obj, String key, int defaultValue) {
        if (obj.has(key) && !obj.get(key).isJsonNull()) {
            try {
                return obj.get(key).getAsInt();
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }
}