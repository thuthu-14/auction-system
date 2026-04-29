/*
package util;

import com.google.gson.*;
import server.model.Admin;
import server.model.RegularUser;
import server.model.User;

import java.lang.reflect.Type;

public class UserTypeAdapter implements JsonSerializer<User>, JsonDeserializer<User> {

    @Override
    public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        if (src instanceof Admin) {
            jsonObject.addProperty("type", "ADMIN");
        } else {
            jsonObject.addProperty("type", "REGULAR");
        }

        JsonElement element = context.serialize(src);
        if (element != null && element.isJsonObject()) {
            JsonObject srcObj = element.getAsJsonObject();
            srcObj.entrySet().forEach(entry -> jsonObject.add(entry.getKey(), entry.getValue()));
        }

        return jsonObject;
    }

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        String type = null;
        if (jsonObject.has("type") && !jsonObject.get("type").isJsonNull()) {
            type = jsonObject.get("type").getAsString();
        } else if (jsonObject.has("role") && !jsonObject.get("role").isJsonNull()) {
            // Backward compatible cho dữ liệu cũ chưa có field "type"
            String role = jsonObject.get("role").getAsString();
            type = "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "REGULAR";
        } else {
            type = "REGULAR";
        }

        switch (type.toUpperCase()) {
            case "ADMIN":
                return context.deserialize(json, Admin.class);
            case "REGULAR":
                return context.deserialize(json, RegularUser.class);
            default:
                throw new JsonParseException("Unknown user type: " + type);
        }
    }
}
*/