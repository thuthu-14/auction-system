package server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulerServiceTest {

    @Test
    public void startAndStopScheduler_usesThreadPoolManager() {
        try (var mocked = org.mockito.Mockito.mockStatic(server.concurrency.ThreadPoolManager.class)) {
            server.concurrency.ThreadPoolManager mockMgr = org.mockito.Mockito.mock(server.concurrency.ThreadPoolManager.class);
            mocked.when(server.concurrency.ThreadPoolManager::getInstance).thenReturn(mockMgr);
            var dummyFuture = org.mockito.Mockito.mock(java.util.concurrent.ScheduledFuture.class);
            org.mockito.Mockito.when(
                    mockMgr.scheduleAtFixedRate(
                            org.mockito.Mockito.any(),
                            org.mockito.Mockito.anyLong(),
                            org.mockito.Mockito.anyLong(),
                            org.mockito.Mockito.any()))
                    .thenReturn(dummyFuture);

            // start scheduler should call scheduleAtFixedRate
            SchedulerService.startScheduler();
            // stop scheduler should attempt to cancel
            SchedulerService.stopScheduler();

            org.mockito.Mockito.verify(mockMgr).scheduleAtFixedRate(
                    org.mockito.Mockito.any(),
                    org.mockito.Mockito.anyLong(),
                    org.mockito.Mockito.anyLong(),
                    org.mockito.Mockito.any());
        }
    }
}
