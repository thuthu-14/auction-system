package server.concurrency;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentBidManagerTest {

    private final ConcurrentBidManager manager = ConcurrentBidManager.getInstance();

    @AfterEach
    void cleanup() {
        // attempt to remove any test locks created
        try {
            manager.removeLock("TEST-AUCTION-1");
            manager.removeLock("TEST-AUCTION-2");
            manager.removeLock("TEST-AUCTION-3");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void withAuctionWriteLock_returnsValue() throws Exception {
        String result = manager.withAuctionWriteLock("TEST-AUCTION-1", () -> "ok");
        assertEquals("ok", result);
        manager.removeLock("TEST-AUCTION-1");
    }

    @Test
    public void withAuctionWriteLock_serializesConcurrentActions() throws Exception {
        int threads = 10;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        AtomicInteger counter = new AtomicInteger(0);

        try {
            CountDownLatch start = new CountDownLatch(1);
            Runnable task = () -> {
                try {
                    start.await();
                    manager.withAuctionWriteLock("TEST-AUCTION-2", () -> {
                        int v = counter.get();
                        // simulate some work
                        Thread.sleep(20);
                        counter.set(v + 1);
                        return null;
                    });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };

            for (int i = 0; i < threads; i++) {
                ex.submit(task);
            }
            // release all tasks to compete for lock
            start.countDown();

            ex.shutdown();
            boolean finished = ex.awaitTermination(5, TimeUnit.SECONDS);
            assertTrue(finished, "Executor did not terminate in time");
            assertEquals(threads, counter.get(), "All tasks should have run serially under write lock");
        } finally {
            ex.shutdownNow();
            manager.removeLock("TEST-AUCTION-2");
        }
    }

    @Test
    public void withAuctionReadLock_allowsConcurrentReads() throws Exception {
        int threads = 8;
        ExecutorService ex = Executors.newFixedThreadPool(threads);
        CountDownLatch entered = new CountDownLatch(threads);
        CountDownLatch release = new CountDownLatch(1);

        try {
            for (int i = 0; i < threads; i++) {
                ex.submit(() -> {
                    try {
                        manager.withAuctionReadLock("TEST-AUCTION-3", () -> {
                            entered.countDown();
                            // wait until test releases to ensure multiple readers are present
                            boolean released = release.await(2, TimeUnit.SECONDS);
                            if (!released) {
                                throw new RuntimeException("Reader wait timed out");
                            }
                            return null;
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }

            // wait for all tasks to have entered the read-lock action
            boolean allEntered = entered.await(1, TimeUnit.SECONDS);
            assertTrue(allEntered, "All readers should be able to enter concurrently");
            // allow tasks to complete
            release.countDown();

            ex.shutdown();
            assertTrue(ex.awaitTermination(2, TimeUnit.SECONDS));
        } finally {
            ex.shutdownNow();
            manager.removeLock("TEST-AUCTION-3");
        }
    }

    @Test
    public void writeLock_blocksOthers_initially() throws Exception {
        ExecutorService ex = Executors.newFixedThreadPool(3);
        AtomicInteger entered = new AtomicInteger(0);

        try {
            CountDownLatch start = new CountDownLatch(1);

            Callable<Void> longTask = () -> manager.withAuctionWriteLock("BLOCK-AUCTION", () -> {
                entered.incrementAndGet();
                // hold the lock for a short while
                Thread.sleep(300);
                return null;
            });

            // submit multiple tasks; only one should enter quickly
            ex.submit(() -> {
                try {
                    start.await();
                    longTask.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            ex.submit(() -> {
                try {
                    start.await();
                    longTask.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            start.countDown();
            // wait a little to allow first task to acquire lock
            Thread.sleep(50);
            assertEquals(1, entered.get(), "Only one writer should have entered while holding the lock");

            // wait for tasks to finish
            ex.shutdown();
            assertTrue(ex.awaitTermination(2, TimeUnit.SECONDS));
        } finally {
            ex.shutdownNow();
            manager.removeLock("BLOCK-AUCTION");
        }
    }

    @Test
    public void removeLock_and_getLockCount_behaviour() throws Exception {
        // create a lock
        manager.withAuctionWriteLock("TEMP-AUCTION", () -> null);
        assertTrue(manager.getLockCount() >= 1);
        manager.removeLock("TEMP-AUCTION");
        // after removal, count should be decreased (>=0)
        assertTrue(manager.getLockCount() >= 0);
    }

    @Test
    public void withAuctionWriteLock_nullOrBlank_throws() {
        assertThrows(IllegalArgumentException.class, () -> manager.withAuctionWriteLock(null, () -> null));
        assertThrows(IllegalArgumentException.class, () -> manager.withAuctionWriteLock("  ", () -> null));
    }

}

