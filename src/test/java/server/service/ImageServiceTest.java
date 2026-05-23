package server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

public class ImageServiceTest {

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void uploadImage_callsImageUploadManager_and_returnsId() throws Exception {
        try (var mocked = mockStatic(server.storage.ImageUploadManager.class)) {
            mocked.when(() -> server.storage.ImageUploadManager.saveImage(anyString(), any(), anyString()))
                    .thenReturn("IMG1");

            ImageService service = new ImageService();
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("imageBytes", new byte[]{(byte)0xFF, (byte)0xD8, 0x00});
            payload.put("filename", "photo.jpg");
            payload.put("itemId", "ITX");

            String id = service.uploadImage(new server.model.RegularUser("u","u","p","e@x.com"), payload);
            assertEquals("IMG1", id);
        }
    }
}


