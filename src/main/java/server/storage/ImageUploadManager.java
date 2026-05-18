package server.storage;

import server.exception.ServerErrorType;
import server.exception.ServerExceptionHandler;
import util.LoggerUtil;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class ImageUploadManager {
    private static final String UPLOAD_DIR = "data/uploads";

    static {
        // Tự động tạo folder nếu chưa có
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            LoggerUtil.info("✓ Upload directory ready: " + UPLOAD_DIR);
        } catch (IOException e) {
            ServerExceptionHandler.handle(ServerErrorType.DATA, "Create upload directory", e);
        }
    }

    /**
     * Lưu file ảnh từ bytes → trả về relative URL
     * @param imageBytes Dữ liệu ảnh (bytes)
     * @param originalFilename Tên file gốc (ví dụ: "photo.jpg")
     * @return Relative URL (ví dụ: "uploads/uuid-12345.jpg")
     */
    public static String saveImage(byte[] imageBytes, String originalFilename) throws IOException {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("Image data is empty");
        }

        // Lấy extension từ filename gốc
        String extension = getFileExtension(originalFilename);

        // Tạo tên file duy nhất
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
        Path filePath = Paths.get(UPLOAD_DIR, uniqueFilename);

        // Lưu file
        Files.write(filePath, imageBytes);

        LoggerUtil.info("✓ Image saved: " + uniqueFilename);

        // Trả về relative path để client có thể download
        return "uploads/" + uniqueFilename;
    }

    /**
     * Lấy file ảnh từ disk
     */
    public static byte[] loadImage(String relativePath) throws IOException {
        Path filePath = Paths.get(relativePath);
        if (!Files.exists(filePath)) {
            throw new IOException("Image not found: " + relativePath);
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * Xóa file ảnh
     */
    public static void deleteImage(String relativePath) throws IOException {
        Path filePath = Paths.get(relativePath);
        Files.deleteIfExists(filePath);
        LoggerUtil.info("✓ Image deleted: " + relativePath);
    }

    private static String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
