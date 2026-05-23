package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import util.LoggerUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageDownloadService {

    /**
     * Tải ảnh từ server về client
     * @param transport Socket kết nối tới server
     *
     * @return byte[] dữ liệu ảnh
     */
    public static byte[] downloadImage(MessageTransport transport, String imageId) throws Exception {
        // [1] Validate input
        if (transport == null || !transport.isConnected()) {
            throw new Exception("Mất kết nối máy chủ");
        }

        if (imageId == null || imageId.isBlank()) {
            throw new Exception("ID ảnh không hợp lệ");
        }

        // [2] Tạo request
        Message request = new Message();
        request.setType(MessageType.DOWNLOAD_IMAGE);
        request.setData(imageId);  // ← Gửi imageId (không phải path)

        LoggerUtil.info("Downloading image: " + imageId);

        // [3] Gửi request và nhận response
        Message response = transport.sendAndReceive(request);

        // [4] Xử lý response
        if (response == null) {
            throw new Exception("Không nhận được phản hồi từ server");
        }

        if (!"SUCCESS".equals(response.getStatus())) {
            throw new Exception(response.getMessage() != null
                    ? response.getMessage()
                    : "Server từ chối tải ảnh");
        }

        // [5] Lấy dữ liệu ảnh
        Object data = response.getData();
        if (!(data instanceof byte[] imageBytes)) {
            throw new Exception("Định dạng dữ liệu ảnh không hợp lệ");
        }

        if (imageBytes.length == 0) {
            throw new Exception("Dữ liệu ảnh trống");
        }

        LoggerUtil.info("✓ Image downloaded: " + imageId + " | Size: " + (imageBytes.length / 1024) + "KB");
        return imageBytes;
    }
}