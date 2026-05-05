package util;

import com.google.gson.*;
import common.UserRole;
import server.model.Admin;
import server.model.RegularUser;
import server.model.User;
import java.lang.reflect.Type;

public class UserTypeAdapter implements JsonSerializer<User>, JsonDeserializer<User> {

    @Override
    public JsonElement serialize(User src, Type typeOfSrc, JsonSerializationContext context) {
        // Sử dụng src.getClass() để Gson biết chính xác đang serialize Admin hay RegularUser
        // Tránh việc gọi lại JsonSerializer<User> gây đệ quy vô hạn
        JsonObject jsonObject = context.serialize(src, src.getClass()).getAsJsonObject();

        // Thêm trường "type" hoặc "role" để hỗ trợ nhận diện lúc đọc file
        if (src instanceof Admin) {
            jsonObject.addProperty("type", "ADMIN");
        } else {
            jsonObject.addProperty("type", "REGULAR");
        }

        return jsonObject;
    }

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        // Kiểm tra field "type" chúng ta vừa thêm hoặc field "role" có sẵn trong model
        String type = "REGULAR";
        if (obj.has("type")) {
            type = obj.get("type").getAsString();
        } else if (obj.has("role")) {
            type = obj.get("role").getAsString();
        }

        // Quyết định class con để khởi tạo
        if ("ADMIN".equalsIgnoreCase(type)) {
            return context.deserialize(json, Admin.class);
        } else {
            return context.deserialize(json, RegularUser.class);
        }
    }
}