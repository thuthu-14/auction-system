package server.repository;

import server.storage.DatabaseManager;
import util.LoggerUtil;

import java.sql.*;
import java.util.*;

public class ImageRepository {

    /**
     * Lưu ảnh vào database
     */
    public static String saveImage(String itemId, byte[] imageData) throws SQLException {
        String imageId = "IMG" + System.currentTimeMillis();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO item_images (image_id, item_id, image_data, image_size, image_type, created_at) " +
                             "VALUES (?, ?, ?, ?, ?, ?)"
             )) {

            ps.setString(1, imageId);
            ps.setString(2, itemId);
            ps.setBytes(3, imageData);
            ps.setInt(4, imageData.length);
            ps.setString(5, "jpg");
            ps.setLong(6, System.currentTimeMillis());

            ps.executeUpdate();
            LoggerUtil.info("✓ Image saved to DB: " + imageId + " | Size: " + (imageData.length / 1024) + "KB");

            return imageId;

        } catch (SQLException e) {
            LoggerUtil.error("Failed to save image: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy ảnh từ database
     */
    public static byte[] getImage(String imageId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT image_data FROM item_images WHERE image_id = ?"
             )) {

            ps.setString(1, imageId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                byte[] imageData = rs.getBytes("image_data");
                LoggerUtil.info("✓ Image loaded from DB: " + imageId + " | Size: " + (imageData.length / 1024) + "KB");
                return imageData;
            }

            throw new SQLException("Image not found: " + imageId);

        } catch (SQLException e) {
            LoggerUtil.error("Failed to load image: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy tất cả ảnh của item
     */
    public static List<String> getItemImages(String itemId) throws SQLException {
        List<String> imageIds = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT image_id FROM item_images WHERE item_id = ? ORDER BY sort_index"
             )) {

            ps.setString(1, itemId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                imageIds.add(rs.getString("image_id"));
            }

            LoggerUtil.info("✓ Loaded " + imageIds.size() + " images for item: " + itemId);
            return imageIds;

        } catch (SQLException e) {
            LoggerUtil.error("Failed to load item images: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Xóa ảnh
     */
    public static void deleteImage(String imageId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM item_images WHERE image_id = ?"
             )) {

            ps.setString(1, imageId);
            int deleted = ps.executeUpdate();

            if (deleted > 0) {
                LoggerUtil.info("✓ Image deleted: " + imageId);
            }

        } catch (SQLException e) {
            LoggerUtil.error("Failed to delete image: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Xóa tất cả ảnh của item
     */
    public static void deleteItemImages(String itemId) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM item_images WHERE item_id = ?"
             )) {

            ps.setString(1, itemId);
            int deleted = ps.executeUpdate();

            LoggerUtil.info("✓ Deleted " + deleted + " images for item: " + itemId);

        } catch (SQLException e) {
            LoggerUtil.error("Failed to delete item images: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Lấy storage info
     */
    public static Map<String, Object> getStorageInfo() throws SQLException {
        Map<String, Object> info = new HashMap<>();

        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {

            // Tổng số ảnh
            ResultSet rs1 = st.executeQuery("SELECT COUNT(*) as count FROM item_images");
            int imageCount = rs1.next() ? rs1.getInt("count") : 0;

            // Tổng dung lượng
            ResultSet rs2 = st.executeQuery("SELECT SUM(image_size) as total FROM item_images");
            long totalSize = rs2.next() ? rs2.getLong("total") : 0;

            info.put("imageCount", imageCount);
            info.put("totalBytes", totalSize);
            info.put("totalMB", totalSize / (1024 * 1024));

            LoggerUtil.info("📊 Database Storage: " + imageCount + " images | " + (totalSize / 1024 / 1024) + "MB");

            return info;

        } catch (SQLException e) {
            LoggerUtil.error("Failed to get storage info: " + e.getMessage());
            throw e;
        }
    }
    /**
     * Link ảnh tới item và set sort_index
     */
    public static void linkImageToItem(String imageId, String itemId, int sortIndex) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE item_images SET item_id = ?, sort_index = ? WHERE image_id = ?"
             )) {
            ps.setString(1, itemId);
            ps.setInt(2, sortIndex);
            ps.setString(3, imageId);
            ps.executeUpdate();
            LoggerUtil.info("✓ Linked image " + imageId + " to item " + itemId);
        } catch (SQLException e) {
            LoggerUtil.error("Failed to link image to item: " + e.getMessage());
            throw e;
        }
    }
    /**
     * Cập nhật sort_index cho imageId
     */
    public static void updateImageSortIndex(String imageId, int sortIndex) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE item_images SET sort_index = ? WHERE image_id = ?"
             )) {
            ps.setInt(1, sortIndex);
            ps.setString(2, imageId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LoggerUtil.error("Failed to update image sort index: " + e.getMessage());
            throw e;
        }
    }
}