package server.concurrency;

import common.Constants;
import util.LoggerUtil;
import java.util.concurrent.*;

public class ThreadPoolManager {

    private static ThreadPoolManager instance;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    private ThreadPoolManager() {
        this.executorService = Executors.newFixedThreadPool(Constants.THREAD_POOL_SIZE);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(2);
        LoggerUtil.info("ThreadPoolManager initialized with " + Constants.THREAD_POOL_SIZE + " threads");
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public void execute(Runnable task) {
        if (task != null) {
            executorService.execute(task);
        }
    }

    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable task,
            long initialDelay,
            long period,
            TimeUnit unit) {
        if (task != null) {
            return scheduledExecutorService.scheduleAtFixedRate(task, initialDelay, period, unit);
        }
        return null;
    }

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        if (task != null) {
            return scheduledExecutorService.schedule(task, delay, unit);
        }
        return null;
    }

    public void shutdown() {
        try {
            executorService.shutdownNow();
            scheduledExecutorService.shutdownNow();

            if (executorService.awaitTermination(5, TimeUnit.SECONDS) &&
                    scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                LoggerUtil.info("Thread pools shut down successfully");
            } else {
                LoggerUtil.warn("Thread pools did not terminate gracefully");
            }
        } catch (InterruptedException e) {
            LoggerUtil.error("Error shutting down thread pools: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return executorService.isShutdown() && scheduledExecutorService.isShutdown();
    }
}
