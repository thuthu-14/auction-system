package server.service;

import server.model.User;
import server.storage.ImageUploadManager;
import util.LoggerUtil;

import java.util.Map;

public class ImageService {
    /**
     * Upload ảnh với validation đầy đủ
     */
    public String uploadImage(User user, Object data) throws Exception {
        // [1] Kiểm tra user đã login
        if (user == null) {
            throw new IllegalStateException("Bạn phải đăng nhập để upload ảnh");
        }

        // [2] Validate dữ liệu
        if (!(data instanceof Map<?, ?> payload)) {
            throw new IllegalArgumentException("Dữ liệu ảnh không hợp lệ");
        }

        // [3] Lấy imageBytes
        Object imageBytes = payload.get("imageBytes");
        if (!(imageBytes instanceof byte[] bytes) || bytes.length == 0) {
            throw new IllegalArgumentException("Dữ liệu ảnh không được để trống");
        }

        // [4] Lấy filename
        Object filename = payload.get("filename");
        String filenameStr = filename != null ? filename.toString() : "image.jpg";

        // [5] 🆕 THÊM: Lấy itemId (nếu có)
        Object itemIdObj = payload.get("itemId");
        String itemId = itemIdObj != null ? itemIdObj.toString() : "";

        // [6] Upload ảnh (lưu vào DB)
        LoggerUtil.info("Uploading image: " + filenameStr + " | User: " + user.getUsername() + " | Item: " + itemId);
        String imageId = ImageUploadManager.saveImage(itemId, bytes, filenameStr);
        LoggerUtil.info("Upload success: " + imageId);

        return imageId;
    }
}