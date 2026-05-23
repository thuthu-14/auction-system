package server.concurrency;

import common.Constants;
import server.exception.ServerErrorType;
import server.exception.ServerExceptionHandler;
import util.LoggerUtil;
import java.util.Objects;
import java.util.concurrent.*;

public class ThreadPoolManager {

    private static ThreadPoolManager instance;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;

    private ThreadPoolManager() {
        this.executorService = Executors.newFixedThreadPool(Constants.THREAD_POOL_SIZE);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(Constants.THREAD_POOL_SIZE);
        LoggerUtil.info("ThreadPoolManager initialized with " + Constants.THREAD_POOL_SIZE + " threads");
    }

    public static synchronized ThreadPoolManager getInstance() {
        if (instance == null) {
            instance = new ThreadPoolManager();
        }
        return instance;
    }

    public void execute(Runnable task) {
        executorService.execute(Objects.requireNonNull(task, "task"));
    }

    public ScheduledFuture<?> scheduleAtFixedRate(
            Runnable task,
            long initialDelay,
            long period,
            TimeUnit unit) {
        return scheduledExecutorService.scheduleAtFixedRate(
                Objects.requireNonNull(task, "task"),
                initialDelay,
                period,
                Objects.requireNonNull(unit, "unit")
        );
    }

    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduledExecutorService.schedule(
                Objects.requireNonNull(task, "task"),
                delay,
                Objects.requireNonNull(unit, "unit")
        );
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
            ServerExceptionHandler.handle(ServerErrorType.SYSTEM, "Shutdown thread pools", e);
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return executorService.isShutdown() && scheduledExecutorService.isShutdown();
    }
}
