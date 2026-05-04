package server.storage;

import server.model.Item;
import util.JsonUtil;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;

public class ItemDAO {

    public static List<Item> getAllItems() throws IOException, ClassNotFoundException {
        JsonUtil.createFileIfNotExists(DataManager.JSON_ITEMS);
        List<Item> items = JsonUtil.loadListFromJson(DataManager.JSON_ITEMS, Item.class);
        return items != null ? items : new ArrayList<>();
    }

    public static Item getItemById(String itemId) throws IOException, ClassNotFoundException {
        List<Item> items = getAllItems();
        for (Item item : items) {
            if (item.getItemId() != null && item.getItemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    public static List<Item> getItemsBySellerId(String sellerId) throws IOException, ClassNotFoundException {
        List<Item> items = getAllItems();
        List<Item> result = new ArrayList<>();
        for (Item item : items) {
            if (item.getSellerId() != null && item.getSellerId().equals(sellerId)) {
                result.add(item);
            }
        }
        return result;
    }

    public static void saveItem(Item item) throws IOException, ClassNotFoundException {
        List<Item> items = getAllItems();

        // Tránh lưu trùng Item nếu đã tồn tại
        items.removeIf(i -> i.getItemId() != null && i.getItemId().equals(item.getItemId()));
        items.add(item);

        // CHỈ LƯU JSON
        JsonUtil.saveToJson(DataManager.JSON_ITEMS, items);

        LoggerUtil.info("Item saved to JSON: " + item.getItemId());
    }

    public static void deleteItem(String itemId) throws IOException, ClassNotFoundException {
        List<Item> items = getAllItems();
        items.removeIf(i -> i.getItemId() != null && i.getItemId().equals(itemId));

        JsonUtil.saveToJson(DataManager.JSON_ITEMS, items);

        LoggerUtil.info("Item deleted from JSON: " + itemId);
    }
}