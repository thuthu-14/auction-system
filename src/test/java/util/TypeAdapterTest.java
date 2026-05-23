package util;

import com.google.gson.Gson;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import server.model.Admin;
import server.model.Art;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Item;
import server.model.Jewelry;
import server.model.OtherItem;
import server.model.RegularUser;
import server.model.User;
import server.model.Vehicle;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeAdapterTest {

    private final Gson gson = JsonUtil.getGson();

    @Test
    void itemTypeAdapterSerializesAndDeserializesAllConcreteItemTypes() {
        assertRoundTrip(new Electronics("E1", "Phone", "Desc", 50_000.0,
                "seller", "Brand", "12 months", List.of("phone.jpg")), Electronics.class);
        assertRoundTrip(new Art("A1", "Art", "Desc", 100_000.0,
                "seller", "Artist", "Canvas", List.of()), Art.class);
        assertRoundTrip(new Vehicle("V1", "Bike", "Desc", 500_000.0,
                "seller", "Model", 123, List.of()), Vehicle.class);
        assertRoundTrip(new Fashion("F1", "Shirt", "Desc", 10_000.0,
                "seller", "Brand", "Cotton", List.of()), Fashion.class);
        assertRoundTrip(new Jewelry("J1", "Ring", "Desc", 25_000.0,
                "seller", "Gold", 2.5, List.of()), Jewelry.class);
        assertRoundTrip(new OtherItem("O1", "Other", "Desc", 10.0,
                "seller", List.of("other.jpg")), OtherItem.class);
    }

    @Test
    void itemTypeAdapterUsesSafeDefaultsForMissingAndInvalidFields() {
        Item item = gson.fromJson("""
                {
                  "type": "UNKNOWN",
                  "startingPrice": "not-a-number",
                  "images": null
                }
                """, Item.class);

        assertInstanceOf(Fashion.class, item);
        assertEquals("", item.getItemId());
        assertEquals("No Name", item.getName());
        assertEquals(0.0, item.getStartingPrice(), 0.001);
        assertTrue(item.getImages().isEmpty());
    }

    @Test
    void itemTypeAdapterPrivateSafeReadersReturnDefaultsForMissingNullAndInvalidValues() throws Exception {
        ItemTypeAdapter adapter = new ItemTypeAdapter();
        JsonObject json = new JsonObject();
        json.add("name", JsonNull.INSTANCE);
        json.addProperty("startingPrice", "bad-double");
        json.add("odometer", JsonNull.INSTANCE);

        assertEquals("fallback", invoke(adapter, "getStringSafe", json, "missing", "fallback"));
        assertEquals("fallback", invoke(adapter, "getStringSafe", json, "name", "fallback"));
        assertEquals(9.5, (double) invoke(adapter, "getDoubleSafe", json, "startingPrice", 9.5), 0.001);
        assertEquals(8.5, (double) invoke(adapter, "getDoubleSafe", json, "missingDouble", 8.5), 0.001);
        assertEquals(7, invoke(adapter, "getIntSafe", json, "odometer", 7));
    }

    @Test
    void userTypeAdapterSerializesAndDeserializesAdminAndRegularUser() {
        Admin admin = new Admin("admin-1", "admin", "password", "admin@example.com");
        RegularUser regular = new RegularUser("u1", "user", "password", "user@example.com");

        User adminRoundTrip = gson.fromJson(gson.toJson(admin, User.class), User.class);
        User regularRoundTrip = gson.fromJson(gson.toJson(regular, User.class), User.class);

        assertInstanceOf(Admin.class, adminRoundTrip);
        assertEquals("admin-1", adminRoundTrip.getUserId());
        assertInstanceOf(RegularUser.class, regularRoundTrip);
        assertEquals("u1", regularRoundTrip.getUserId());
    }

    @Test
    void userTypeAdapterFallsBackToRoleWhenTypeMissing() {
        User admin = gson.fromJson("""
                {
                  "userId": "admin-1",
                  "username": "admin",
                  "password": "password",
                  "email": "admin@example.com",
                  "role": "ADMIN"
                }
                """, User.class);

        assertInstanceOf(Admin.class, admin);
    }

    private void assertRoundTrip(Item item, Class<? extends Item> expectedType) {
        Item roundTrip = gson.fromJson(gson.toJson(item, Item.class), Item.class);

        assertInstanceOf(expectedType, roundTrip);
        assertEquals(item.getItemId(), roundTrip.getItemId());
        assertEquals(item.getName(), roundTrip.getName());
        assertEquals(item.getImages(), roundTrip.getImages());
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        for (var method : target.getClass().getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                method.setAccessible(true);
                return method.invoke(target, args);
            }
        }
        throw new IllegalArgumentException("Method not found: " + methodName);
    }
}
