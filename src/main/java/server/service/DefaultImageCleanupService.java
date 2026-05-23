package server.service;

import server.storage.ImageUploadManager;

public class DefaultImageCleanupService implements ImageCleanupService {
    @Override
    public void deleteItemImages(String itemId) throws Exception {
        ImageUploadManager.deleteItemImages(itemId);
    }
}
