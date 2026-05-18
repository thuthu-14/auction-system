package server.handler;

import common.Message;
import common.MessageType;
import server.exception.ServerExceptionHandler;
import server.service.ImageService;
import server.storage.ImageUploadManager;
import util.LoggerUtil;

import java.io.IOException;

public class MediaMessageProcessor {
    private final ClientSessionContext context;
    private final ImageService imageService = new ImageService();

    public MediaMessageProcessor(ClientSessionContext context) {
        this.context = context;
    }

    public void handleUploadImage(Message message) throws IOException {
        try {
            String imageUrl = imageService.uploadImage(context.getCurrentUser(), message.getData());
            Message response = new Message();
            response.setType(MessageType.UPLOAD_IMAGE_RESPONSE);
            response.setData(imageUrl);
            response.setSenderId("SERVER");
            response.setStatus("SUCCESS");
            context.sendMessage(response);
        } catch (Exception e) {
            ServerExceptionHandler.handle("Upload image", e);
            context.sendError(e.getMessage());
        }
    }
    /**
     * Download ảnh từ server về client
     */
    public void handleDownloadImage(Message message) throws IOException {
        try {
            // [1] Validate request
            if (message == null || message.getData() == null) {
                throw new IllegalArgumentException("Yêu cầu ảnh không hợp lệ");
            }

            String imageId = message.getData().toString();
            LoggerUtil.info("Downloading image: " + imageId);

            // [2] Lấy dữ liệu ảnh từ ImageUploadManager (đọc từ DB)
            byte[] imageData = ImageUploadManager.loadImage(imageId);

            // [3] Tạo response
            Message response = new Message();
            response.setType(MessageType.DOWNLOAD_IMAGE_RESPONSE);
            response.setData(imageData);
            response.setSenderId("SERVER");
            response.setStatus("SUCCESS");

            // [4] Gửi lại cho client
            context.sendMessage(response);
            LoggerUtil.info("✓ Image downloaded: " + imageId + " | Size: " + (imageData.length / 1024) + "KB");

        } catch (Exception e) {
            LoggerUtil.error("Download image failed: " + e.getMessage());
            ServerExceptionHandler.handle("Download image", e);
            context.sendError("Không thể tải ảnh: " + e.getMessage());
        }
    }
}
