package server.service;

import server.model.User;
import server.storage.ImageUploadManager;

import java.util.Map;

public class ImageService {
    public String uploadImage(User user, Object data) throws Exception {
        if (user == null) {
            throw new IllegalStateException("Ban phai dang nhap");
        }
        if (!(data instanceof Map<?, ?> payload)) {
            throw new IllegalArgumentException("Du lieu anh khong hop le");
        }

        Object imageBytes = payload.get("imageBytes");
        if (!(imageBytes instanceof byte[] bytes) || bytes.length == 0) {
            throw new IllegalArgumentException("Du lieu anh trong");
        }

        Object filename = payload.get("filename");
        return ImageUploadManager.saveImage(bytes, filename != null ? filename.toString() : null);
    }
}
