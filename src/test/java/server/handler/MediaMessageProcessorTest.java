package server.handler;

import common.Message;
import common.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import server.model.User;
import server.service.ImageService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MediaMessageProcessorTest {

    @Mock
    ClientSessionContext context;

    @Mock
    ImageService imageService;

    private AutoCloseable mocksClose;

    @BeforeEach
    void init() {
        mocksClose = MockitoAnnotations.openMocks(this);
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() throws Exception {
        if (mocksClose != null) {
            mocksClose.close();
        }
    }

    @Test
    void handleUploadImageSendsUploadResponse() throws Exception {
        MediaMessageProcessor processor = new MediaMessageProcessor(context, imageService);
        User user = mock(User.class);
        when(context.getCurrentUser()).thenReturn(user);
        when(imageService.uploadImage(user, "payload")).thenReturn("IMG1");

        processor.handleUploadImage(new Message(MessageType.UPLOAD_IMAGE, (Object) "payload", "client"));

        ArgumentCaptor<Message> responseCaptor = ArgumentCaptor.forClass(Message.class);
        verify(context).sendMessage(responseCaptor.capture());
        Message response = responseCaptor.getValue();
        assertEquals(MessageType.UPLOAD_IMAGE_RESPONSE, response.getType());
        assertEquals("IMG1", response.getData());
        assertEquals("SERVER", response.getSenderId());
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void handleUploadImageErrorSendsError() throws Exception {
        MediaMessageProcessor processor = new MediaMessageProcessor(context, imageService);
        when(imageService.uploadImage(any(), any())).thenThrow(new IllegalArgumentException("bad image"));

        processor.handleUploadImage(new Message(MessageType.UPLOAD_IMAGE, (Object) "payload", "client"));

        verify(context).sendError("bad image");
        verify(context, never()).sendMessage(any());
    }

    @Test
    void handleDownloadImageRejectsNullMessage() throws Exception {
        MediaMessageProcessor processor = new MediaMessageProcessor(context, imageService);

        processor.handleDownloadImage(null);

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(context).sendError(errorCaptor.capture());
        assertTrue(errorCaptor.getValue().contains("Kh"));
    }

    @Test
    void handleDownloadImageRejectsMissingData() throws Exception {
        MediaMessageProcessor processor = new MediaMessageProcessor(context, imageService);

        processor.handleDownloadImage(new Message(MessageType.DOWNLOAD_IMAGE, (Object) null, "client"));

        verify(context).sendError(contains(":"));
        verify(context, never()).sendMessage(any());
    }
}
