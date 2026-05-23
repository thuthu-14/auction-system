package server.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ThreadPoolManagerTest {

    @AfterEach
    void resetSingleton() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        if (!manager.isShutdown()) {
            manager.shutdown();
        }

        Field instance = ThreadPoolManager.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    @Test
    void getInstanceReturnsSingleton() {
        ThreadPoolManager first = ThreadPoolManager.getInstance();
        ThreadPoolManager second = ThreadPoolManager.getInstance();

        assertSame(first, second);
        assertFalse(first.isShutdown());
    }

    @Test
    void executeRunsTask() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        CountDownLatch ran = new CountDownLatch(1);

        manager.execute(ran::countDown);

        assertTrue(ran.await(2, TimeUnit.SECONDS));
    }

    @Test
    void scheduleRunsTaskAfterDelay() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        CountDownLatch ran = new CountDownLatch(1);

        ScheduledFuture<?> future = manager.schedule(ran::countDown, 10, TimeUnit.MILLISECONDS);

        assertTrue(ran.await(2, TimeUnit.SECONDS));
        assertNull(future.get(2, TimeUnit.SECONDS));
    }

    @Test
    void scheduleAtFixedRateRunsRepeatedlyUntilCancelled() throws Exception {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();
        CountDownLatch ranTwice = new CountDownLatch(2);

        ScheduledFuture<?> future = manager.scheduleAtFixedRate(ranTwice::countDown,
                0,
                10,
                TimeUnit.MILLISECONDS);

        assertTrue(ranTwice.await(2, TimeUnit.SECONDS));
        future.cancel(true);
        assertTrue(future.isCancelled());
    }

    @Test
    void shutdownMarksBothExecutorsShutdown() {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();

        manager.shutdown();

        assertTrue(manager.isShutdown());
    }

    @Test
    void nullArgumentsThrow() {
        ThreadPoolManager manager = ThreadPoolManager.getInstance();

        assertThrows(NullPointerException.class, () -> manager.execute(null));
        assertThrows(NullPointerException.class,
                () -> manager.schedule(null, 1, TimeUnit.MILLISECONDS));
        assertThrows(NullPointerException.class,
                () -> manager.schedule(() -> { }, 1, null));
        assertThrows(NullPointerException.class,
                () -> manager.scheduleAtFixedRate(null, 1, 1, TimeUnit.MILLISECONDS));
        assertThrows(NullPointerException.class,
                () -> manager.scheduleAtFixedRate(() -> { }, 1, 1, null));
    }
}
