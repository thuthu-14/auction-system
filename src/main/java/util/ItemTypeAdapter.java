// src/main/java/util/ItemTypeAdapter.java
package util;

import com.google.gson.*;
import server.model.*;
import common.ItemCategory;
import java.lang.reflect.Type;

/**
 * ItemTypeAdapter - Hỗ trợ Gson serialize/deserialize polymorphic Item
 *
 * Vấn đề: Item là abstract, Gson ko biết serialize Item nào (Electronics, Art, etc)
 * Giải pháp: Custom type adapter - thêm field "type" vào JSON
 */
public class ItemTypeAdapter implements JsonSerializer<Item>, JsonDeserializer<Item> {

    /**
     * Serialize Item → JSON
     * Thêm field "type" để biết loại Item
     */
    @Override
    public JsonElement serialize(Item src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        // Thêm type
        if (src instanceof Electronics) {
            jsonObject.addProperty("type", "ELECTRONICS");
        } else if (src instanceof Art) {
            jsonObject.addProperty("type", "ART");
        } else if (src instanceof Vehicle) {
            jsonObject.addProperty("type", "VEHICLE");
        } else if (src instanceof Fashion) {
            jsonObject.addProperty("type", "FASHION");
        } else if (src instanceof Jewelry) {
            jsonObject.addProperty("type", "JEWELRY");
        }

        // Serialize tất cả fields
        JsonElement element = context.serialize(src);
        if (element.isJsonObject()) {
            JsonObject srcObj = element.getAsJsonObject();
            srcObj.entrySet().forEach(entry -> jsonObject.add(entry.getKey(), entry.getValue()));
        }

        return jsonObject;
    }

    /**
     * Deserialize JSON → Item
     * Đọc field "type" để tạo đúng class
     */
    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // Lấy type
        String type = jsonObject.get("type").getAsString();

        // Deserialize dựa trên type
        switch (type) {
            case "ELECTRONICS":
                return context.deserialize(json, Electronics.class);
            case "ART":
                return context.deserialize(json, Art.class);
            case "VEHICLE":
                return context.deserialize(json, Vehicle.class);
            case "FASHION":
                return context.deserialize(json, Fashion.class);
            case "JEWELRY":
                return context.deserialize(json, Jewelry.class);
            default:
                throw new JsonParseException("Unknown item type: " + type);
        }
    }
}
