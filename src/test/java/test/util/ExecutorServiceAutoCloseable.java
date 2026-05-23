package test.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorServiceAutoCloseable implements AutoCloseable {
    private final ExecutorService executor;

    public ExecutorServiceAutoCloseable(ExecutorService executor) {
        this.executor = executor;
    }

    public ExecutorService executor() {
        return executor;
    }

    @Override
    public void close() throws Exception {
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }
}

