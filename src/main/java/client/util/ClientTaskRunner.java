package client.util;

import util.LoggerUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class ClientTaskRunner {
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(new ClientThreadFactory());

    private ClientTaskRunner() {
    }

    public static void run(Runnable task) {
        if (task == null) {
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LoggerUtil.error("Client background task failed: " + e.getMessage());
            }
        });
    }

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }

    private static final class ClientThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "ClientTask-" + THREAD_COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
