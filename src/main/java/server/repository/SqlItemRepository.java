package server.repository;

import server.model.Item;
import server.storage.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import server.repository.ImageRepository;

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

        // Link ảnh vào item này và set sort_index
        if (images != null && !images.isEmpty()) {
            int index = 0;
            for (String imageId : images) {
                if (imageId != null && !imageId.isBlank()) {
                    try {
                        // Cập nhật item_id từ "" -> itemId thực và set sort_index
                        ImageRepository.linkImageToItem(imageId, itemId, index);
                        index++;
                    } catch (Exception e) {
                        // Bỏ qua lỗi individual image
                    }
                }
            }
        }
    }

    private List<String> loadImages(String itemId) throws Exception {
        List<String> list = new ArrayList<>();
        try {
            list = ImageRepository.getItemImages(itemId);
        } catch (Exception e) {
            // Bỏ qua - trả về list rỗng nếu lỗi
        }
        return list;
    }
}