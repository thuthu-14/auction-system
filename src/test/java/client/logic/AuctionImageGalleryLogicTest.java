package client.logic;

import org.junit.jupiter.api.Test;
import server.model.OtherItem;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionImageGalleryLogicTest {
    private final AuctionImageGalleryLogic logic = new AuctionImageGalleryLogic();

    @Test
    void imageIdsForHandlesMissingImages() {
        assertTrue(logic.imageIdsFor(null).isEmpty());

        OtherItem item = new OtherItem();
        item.setImages(null);

        assertTrue(logic.imageIdsFor(item).isEmpty());
    }

    @Test
    void imageIdsForFiltersBlankIdsAndKeepsOrder() {
        OtherItem item = new OtherItem();
        item.setImages(Arrays.asList("IMG1", "", null, "  ", "IMG2"));

        assertEquals(List.of("IMG1", "IMG2"), logic.imageIdsFor(item));
    }

    @Test
    void firstImageIdReturnsFirstOrBlankFallback() {
        assertEquals("IMG1", logic.firstImageId(List.of("IMG1", "IMG2")));
        assertEquals("", logic.firstImageId(List.of()));
        assertEquals("", logic.firstImageId(null));
    }
}
