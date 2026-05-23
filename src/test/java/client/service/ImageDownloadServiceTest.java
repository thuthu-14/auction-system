package client.service;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageDownloadServiceTest {

    @Test
    void downloadImageSendsDownloadRequestAndReturnsBytes() throws Exception {
        byte[] bytes = {1, 2, 3};
        Message response = new Message(MessageType.DOWNLOAD_IMAGE, bytes, "server");
        FakeMessageTransport transport = new FakeMessageTransport().respondWith(response);

        byte[] result = ImageDownloadService.downloadImage(transport, "IMG1");

        assertArrayEquals(bytes, result);
        Message sent = transport.lastSent();
        assertEquals(MessageType.DOWNLOAD_IMAGE, sent.getType());
        assertEquals("IMG1", sent.getData());
    }

    @Test
    void downloadImageRejectsDisconnectedTransportAndBlankId() {
        assertThrows(Exception.class,
                () -> ImageDownloadService.downloadImage(new FakeMessageTransport().disconnected(), "IMG1"));

        assertThrows(Exception.class,
                () -> ImageDownloadService.downloadImage(new FakeMessageTransport(), " "));
    }

    @Test
    void downloadImageRejectsFailedMissingInvalidOrEmptyResponse() {
        assertThrows(Exception.class,
                () -> ImageDownloadService.downloadImage(new FakeMessageTransport(), "IMG1"));

        Message failed = new Message(MessageType.DOWNLOAD_IMAGE, "ERROR", "not found");
        Exception serverError = assertThrows(Exception.class,
                () -> ImageDownloadService.downloadImage(new FakeMessageTransport().respondWith(failed), "IMG1"));
        assertEquals("not found", serverError.getMessage());

        Message wrongData = new Message(MessageType.DOWNLOAD_IMAGE, "not bytes", "server");
        assertThrows(Exception.class,
                () -> ImageDownloadService.downloadImage(new FakeMessageTransport().respondWith(wrongData), "IMG1"));

        Message emptyData = new Message(MessageType.DOWNLOAD_IMAGE, new byte[0], "server");
        assertThrows(Exception.class,
                () -> ImageDownloadService.downloadImage(new FakeMessageTransport().respondWith(emptyData), "IMG1"));
    }
}
