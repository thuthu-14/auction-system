package server.repository;

import server.model.Item;
import server.storage.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlItemRepository implements ItemRepository {

    @Override
    public void saveItem(Item item) throws Exception {
        String sql = """
            INSERT OR REPLACE INTO items
            (item_id, type, name, description, starting_price, category, seller_id, created_at,
             brand, warranty_period, model, odometer, material, weight, creator)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, item.getItemId());
            ps.setString(2, ItemMapper.resolveType(item));
            ps.setString(3, item.getName());
            ps.setString(4, item.getDescription());
            ps.setDouble(5, item.getStartingPrice());
            ps.setString(6, item.getCategory().name());
            ps.setString(7, item.getSellerId());
            ps.setLong(8, item.getCreatedAt());

            // subtype fields
            ps.setString(9,  SqlItemUtil.getBrand(item));
            ps.setString(10, SqlItemUtil.getWarranty(item));
            ps.setString(11, SqlItemUtil.getModel(item));
            ps.setObject(12, SqlItemUtil.getOdometer(item));
            ps.setString(13, SqlItemUtil.getMaterial(item));
            ps.setObject(14, SqlItemUtil.getWeight(item));
            ps.setString(15, SqlItemUtil.getCreator(item));

            ps.executeUpdate();
        }

        saveImages(item.getItemId(), item.getImages());
    }

    public Item getItemById(String itemId) throws Exception {
        String sql = "SELECT * FROM items WHERE item_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                List<String> images = loadImages(itemId);
                return ItemMapper.fromResultSet(rs, images);
            }
        }
        return null;
    }

    private void saveImages(String itemId, List<String> images) throws Exception {
        if (itemId == null) return;

        try (Connection c = DatabaseManager.getConnection()) {
            c.setAutoCommit(false);

            try (PreparedStatement del = c.prepareStatement(
                    "DELETE FROM item_images WHERE item_id = ?")) {
                del.setString(1, itemId);
                del.executeUpdate();
            }

            if (images != null) {
                try (PreparedStatement ins = c.prepareStatement(
                        "INSERT INTO item_images (item_id, image_url, sort_index) VALUES (?, ?, ?)")) {
                    int idx = 0;
                    for (String url : images) {
                        if (url == null || url.isBlank()) {
                            continue;
                        }
                        ins.setString(1, itemId);
                        ins.setString(2, url);
                        ins.setInt(3, idx++);
                        ins.addBatch();
                    }
                    ins.executeBatch();
                }
            }

            c.commit();
        }
    }

    private List<String> loadImages(String itemId) throws Exception {
        List<String> list = new ArrayList<>();
        String sql = "SELECT image_url FROM item_images WHERE item_id = ? ORDER BY sort_index ASC";

        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("image_url"));
            }
        }
        return list;
    }
}
