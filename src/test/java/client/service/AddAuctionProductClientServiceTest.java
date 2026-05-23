package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;
import server.model.RegularUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AddAuctionProductClientServiceTest {
    private final AddAuctionProductClientService service = new AddAuctionProductClientService();
    private final RegularUser user = new RegularUser("U1", "seller", "pw", "seller@example.com");

    @Test
    void categoryLookupNormalizesVietnameseLabels() {
        assertTrue(service.categoryLabels().contains("Đồ điện tử"));
        assertNotNull(service.findCategoryForm("do dien tu"));
        assertNotNull(service.findCategoryForm("ĐỒ ĐIỆN TỬ"));
        assertNull(service.findCategoryForm("unknown"));
    }

    @Test
    void publishSendsCreateSellerItemMessage() throws Exception {
        Map<String, Object> payload = Map.of("name", "Item");
        FakeMessageTransport transport = new FakeMessageTransport()
                .respondWith(new Message(MessageType.CREATE_SELLER_ITEM, "SUCCESS", "created"));

        Message response = service.publish(transport, user, payload);

        assertEquals("created", response.getMessage());
        Message sent = transport.lastSent();
        assertEquals(MessageType.CREATE_SELLER_ITEM, sent.getType());
        assertEquals(payload, sent.getData());
        assertEquals("seller", sent.getSenderId());
    }

    @Test
    void publishRejectsDisconnectedTransportOrMissingUser() {
        assertThrows(IOException.class,
                () -> service.publish(new FakeMessageTransport().disconnected(), user, Map.of()));
        assertThrows(IOException.class,
                () -> service.publish(new FakeMessageTransport(), null, Map.of()));
    }

    @Test
    void buildPayloadValidatesBaseFieldsBeforeUploading() {
        AddAuctionProductClientService.ValidationException missingName = assertThrows(
                AddAuctionProductClientService.ValidationException.class,
                () -> service.buildPayload(new FakeMessageTransport(), user, " ", "desc", "Khác", List.of("x"), null));
        assertEquals("Thiếu thông tin", missingName.getTitle());

        AddAuctionProductClientService.ValidationException missingImage = assertThrows(
                AddAuctionProductClientService.ValidationException.class,
                () -> service.buildPayload(new FakeMessageTransport(), user, "Item", "desc", "Khác", List.of(), null));
        assertEquals("Thiếu ảnh", missingImage.getTitle());
    }
}
