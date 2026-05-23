package server.storage;

import server.exception.ServerErrorType;
import server.exception.ServerExceptionHandler;
import server.repository.ImageRepository;
import util.LoggerUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class ImageUploadManager {

    // Cấu hình upload
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_IMAGE_WIDTH = 1200;
    private static final int MAX_IMAGE_HEIGHT = 1200;
    private static final Set<String> ALLOWED_FORMATS = Set.of("jpg", "jpeg", "png", "gif");

    // Cache ảnh để tránh đọc DB nhiều lần
    private static final Map<String, byte[]> imageCache =
            Collections.synchronizedMap(new LinkedHashMap<String, byte[]>(16, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > 100; // Giữ 100 ảnh gần đây
                }
            });

    static {
        LoggerUtil.info("✓ Image upload manager initialized (SQLite mode)");
    }

    /**
     * Lưu file ảnh: Nén + Validate + Lưu vào DB → trả về imageId
     */
    public static String saveImage(String itemId, byte[] imageBytes, String originalFilename) throws IOException {
        // [1] Kiểm tra kích thước file
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Dữ liệu ảnh rỗng");
        }
        if (imageBytes.length > MAX_FILE_SIZE) {
            throw new IOException("File quá lớn: " + (imageBytes.length / 1024 / 1024)
                    + "MB (giới hạn: 5MB)");
        }

        // [2] Validate magic bytes (kiểm tra file thực sự là ảnh)
        if (!isValidImageFile(imageBytes)) {
            throw new IOException("File không phải ảnh thực");
        }

        // [3] Validate extension
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_FORMATS.contains(extension)) {
            throw new IOException("Định dạng không hỗ trợ: " + extension);
        }

        try {
            // [4] Đọc ảnh
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new IOException("Không thể đọc file ảnh");
            }

            // [5] Nén ảnh (resize nếu cần + chuyển sang JPEG)
            BufferedImage compressedImage = compressImage(originalImage);

            // [6] Lưu ảnh nén
            byte[] compressedBytes = imageToByte(compressedImage, "jpg");

            // [7] 🆕 THAY: LƯU VÀO DB THAY VÌ DISK
            String imageId = ImageRepository.saveImage(itemId, compressedBytes);

            LoggerUtil.info("✓ Image saved to DB (compressed): " + imageId
                    + " | Original: " + (imageBytes.length / 1024) + "KB → Compressed: "
                    + (compressedBytes.length / 1024) + "KB");

            return imageId;

        } catch (IOException | IllegalArgumentException e) {
            throw new IOException("Lỗi xử lý ảnh: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IOException("Lỗi lưu ảnh vào DB: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy ảnh từ cache hoặc DB
     */
    public static byte[] loadImage(String imageId) throws IOException {
        // Kiểm tra cache trước
        if (imageCache.containsKey(imageId)) {
            LoggerUtil.info("✓ Image loaded from cache: " + imageId);
            return imageCache.get(imageId);
        }

        // Nếu không có cache thì đọc từ DB
        try {
            byte[] data = ImageRepository.getImage(imageId);
            imageCache.put(imageId, data); // Lưu vào cache
            LoggerUtil.info("✓ Image loaded from DB: " + imageId);
            return data;
        } catch (Exception e) {
            throw new IOException("Không thể tải ảnh: " + imageId + " | " + e.getMessage(), e);
        }
    }

    /**
     * Xóa ảnh từ DB và cache
     */
    public static void deleteImage(String imageId) throws IOException {
        try {
            ImageRepository.deleteImage(imageId);
            imageCache.remove(imageId); // Xóa khỏi cache
            LoggerUtil.info("✓ Image deleted: " + imageId);
        } catch (Exception e) {
            throw new IOException("Không thể xóa ảnh: " + imageId + " | " + e.getMessage(), e);
        }
    }

    /**
     * Xóa tất cả ảnh của item
     */
    public static void deleteItemImages(String itemId) throws IOException {
        try {
            ImageRepository.deleteItemImages(itemId);
            LoggerUtil.info("✓ Item images deleted: " + itemId);
        } catch (Exception e) {
            throw new IOException("Không thể xóa ảnh của item: " + itemId + " | " + e.getMessage(), e);
        }
    }

    /**
     * Nén ảnh: Resize nếu cần + chuyển sang JPEG
     */
    private static BufferedImage compressImage(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();

        // [1] Nếu ảnh quá lớn → resize
        if (width > MAX_IMAGE_WIDTH || height > MAX_IMAGE_HEIGHT) {
            double ratio = Math.min((double) MAX_IMAGE_WIDTH / width,
                    (double) MAX_IMAGE_HEIGHT / height);
            int newWidth = (int) (width * ratio);
            int newHeight = (int) (height * ratio);

            BufferedImage resized = new BufferedImage(newWidth, newHeight,
                    BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
            g2d.dispose();

            LoggerUtil.info("  Resized: " + width + "x" + height + " → " + newWidth + "x" + newHeight);
            return resized;
        }

        // [2] Nếu là PNG với transparency → chuyển sang RGB
        if (original.getType() != BufferedImage.TYPE_INT_RGB) {
            BufferedImage rgb = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = rgb.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            g2d.drawImage(original, 0, 0, null);
            g2d.dispose();
            return rgb;
        }

        return original;
    }

    /**
     * Chuyển BufferedImage sang byte[] (JPEG format)
     */
    private static byte[] imageToByte(BufferedImage image, String format)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    /**
     * Kiểm tra file signature (magic bytes)
     */
    private static boolean isValidImageFile(byte[] bytes) {
        if (bytes.length < 4) return false;

        // JPEG: FF D8 FF
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) return true;

        // PNG: 89 50 4E 47
        if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50) return true;

        // GIF: 47 49 46
        if (bytes[0] == 0x47 && bytes[1] == 0x49) return true;

        return false;
    }

    /**
     * Lấy extension từ filename
     */
    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Method để clear cache khi cần
     */
    public static void clearCache() {
        imageCache.clear();
        LoggerUtil.info("✓ Image cache cleared");
    }

    /**
     * Lấy thông tin cache
     */
    public static String getCacheInfo() {
        return "Cache: " + imageCache.size() + " images";
    }
}