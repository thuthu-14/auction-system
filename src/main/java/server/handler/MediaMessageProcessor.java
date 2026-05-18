package server.handler;

import common.Message;
import common.MessageType;
import server.exception.ServerExceptionHandler;
import server.service.ImageService;

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
}
