package server.service;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import server.model.User;
import server.observer.NotificationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class NotificationServiceTest {

    @Test
    public void getNotifications_delegatesToManager() throws Exception {
        try (MockedStatic<NotificationManager> nms = mockStatic(NotificationManager.class)) {
            NotificationManager mockMgr = mock(NotificationManager.class);
            when(mockMgr.getNotifications(any())).thenReturn(List.of());
            nms.when(NotificationManager::getInstance).thenReturn(mockMgr);
            NotificationService svc = new NotificationService();

            User u = new server.model.RegularUser("u","u","p","e@x.com");
            var res = svc.getNotifications(u);
            assertNotNull(res);
            verify(mockMgr).getNotifications(u);
        }
    }
}


