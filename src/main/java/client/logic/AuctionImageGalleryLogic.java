package client.logic;

import server.model.Item;

import java.util.List;

public class AuctionImageGalleryLogic {
    public List<String> imageIdsFor(Item item) {
        if (item == null || item.getImages() == null) {
            return List.of();
        }

        return item.getImages().stream()
                .filter(imageId -> imageId != null && !imageId.isBlank())
                .toList();
    }

    public String firstImageId(List<String> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) {
            return "";
        }

        return imageIds.get(0);
    }
}
