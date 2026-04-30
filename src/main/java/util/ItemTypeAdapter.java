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

        // ✅ Thêm type
        if (src instanceof Electronics) {
            jsonObject.addProperty("type", "ELECTRONICS");
            Electronics e = (Electronics) src;
            jsonObject.addProperty("itemId", e.getItemId());
            jsonObject.addProperty("name", e.getName());
            jsonObject.addProperty("description", e.getDescription());
            jsonObject.addProperty("startingPrice", e.getStartingPrice());
            jsonObject.addProperty("sellerId", e.getSellerId());
            jsonObject.addProperty("brand", e.getBrand());
            jsonObject.addProperty("warrantyPeriod", e.getWarrantyPeriod());  // ← FIX: warranty → warrantyPeriod
        } else if (src instanceof Art) {
            jsonObject.addProperty("type", "ART");
            Art a = (Art) src;
            jsonObject.addProperty("itemId", a.getItemId());
            jsonObject.addProperty("name", a.getName());
            jsonObject.addProperty("description", a.getDescription());
            jsonObject.addProperty("startingPrice", a.getStartingPrice());
            jsonObject.addProperty("sellerId", a.getSellerId());
            jsonObject.addProperty("creator", a.getCreator());  // ← FIX: artist → creator
            jsonObject.addProperty("material", a.getMaterial());  // ← FIX: medium → material
        } else if (src instanceof Vehicle) {
            jsonObject.addProperty("type", "VEHICLE");
            Vehicle v = (Vehicle) src;
            jsonObject.addProperty("itemId", v.getItemId());
            jsonObject.addProperty("name", v.getName());
            jsonObject.addProperty("description", v.getDescription());
            jsonObject.addProperty("startingPrice", v.getStartingPrice());
            jsonObject.addProperty("sellerId", v.getSellerId());
            jsonObject.addProperty("model", v.getModel());  // ← FIX: Không có brand
            jsonObject.addProperty("odometer", v.getOdometer());  // ← FIX: Thêm odometer
        } else if (src instanceof Fashion) {
            jsonObject.addProperty("type", "FASHION");
            Fashion f = (Fashion) src;
            jsonObject.addProperty("itemId", f.getItemId());
            jsonObject.addProperty("name", f.getName());
            jsonObject.addProperty("description", f.getDescription());
            jsonObject.addProperty("startingPrice", f.getStartingPrice());
            jsonObject.addProperty("sellerId", f.getSellerId());
            jsonObject.addProperty("brand", f.getBrand());
            jsonObject.addProperty("material", f.getMaterial());  // ← FIX: size → material
        } else if (src instanceof Jewelry) {
            jsonObject.addProperty("type", "JEWELRY");
            Jewelry j = (Jewelry) src;
            jsonObject.addProperty("itemId", j.getItemId());
            jsonObject.addProperty("name", j.getName());
            jsonObject.addProperty("description", j.getDescription());
            jsonObject.addProperty("startingPrice", j.getStartingPrice());
            jsonObject.addProperty("sellerId", j.getSellerId());
            jsonObject.addProperty("material", j.getMaterial());
            jsonObject.addProperty("weight", j.getWeight());
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

        String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "UNKNOWN";

        switch (type) {
            case "ELECTRONICS":
                return new Electronics(
                        jsonObject.get("itemId").getAsString(),
                        jsonObject.get("name").getAsString(),
                        jsonObject.get("description").getAsString(),
                        jsonObject.get("startingPrice").getAsDouble(),
                        jsonObject.get("sellerId").getAsString(),
                        jsonObject.get("brand").getAsString(),
                        jsonObject.get("warrantyPeriod").getAsString()  // ← FIX
                );
            case "ART":
                return new Art(
                        jsonObject.get("itemId").getAsString(),
                        jsonObject.get("name").getAsString(),
                        jsonObject.get("description").getAsString(),
                        jsonObject.get("startingPrice").getAsDouble(),
                        jsonObject.get("sellerId").getAsString(),
                        jsonObject.get("creator").getAsString(),  // ← FIX
                        jsonObject.get("material").getAsString()  // ← FIX
                );
            case "VEHICLE":
                return new Vehicle(
                        jsonObject.get("itemId").getAsString(),
                        jsonObject.get("name").getAsString(),
                        jsonObject.get("description").getAsString(),
                        jsonObject.get("startingPrice").getAsDouble(),
                        jsonObject.get("sellerId").getAsString(),
                        jsonObject.get("model").getAsString(),  // ← FIX
                        jsonObject.get("odometer").getAsInt()  // ← FIX
                );
            case "FASHION":
                return new Fashion(
                        jsonObject.get("itemId").getAsString(),
                        jsonObject.get("name").getAsString(),
                        jsonObject.get("description").getAsString(),
                        jsonObject.get("startingPrice").getAsDouble(),
                        jsonObject.get("sellerId").getAsString(),
                        jsonObject.get("brand").getAsString(),
                        jsonObject.get("material").getAsString()  // ← FIX
                );
            case "JEWELRY":
                return new Jewelry(
                        jsonObject.get("itemId").getAsString(),
                        jsonObject.get("name").getAsString(),
                        jsonObject.get("description").getAsString(),
                        jsonObject.get("startingPrice").getAsDouble(),
                        jsonObject.get("sellerId").getAsString(),
                        jsonObject.get("material").getAsString(),
                        jsonObject.get("weight").getAsDouble()
                );
            default:
                throw new JsonParseException("Unknown item type: " + type);
        }
    }
}