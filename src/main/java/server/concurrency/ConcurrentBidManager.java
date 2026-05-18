package server.concurrency;

import util.LoggerUtil;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Objects;

public class ConcurrentBidManager {

    private ConcurrentHashMap<String, ReentrantReadWriteLock> auctionLocks;
    private static ConcurrentBidManager instance;

    private ConcurrentBidManager() {
        this.auctionLocks = new ConcurrentHashMap<>();
    }

    public static synchronized ConcurrentBidManager getInstance() {
        if (instance == null) {
            instance = new ConcurrentBidManager();
        }
        return instance;
    }

    public <T> T withAuctionWriteLock(String auctionId, Callable<T> action) throws Exception {
        Objects.requireNonNull(action, "action");
        ReentrantReadWriteLock lock = auctionLocks.computeIfAbsent(
                requireAuctionId(auctionId),
                k -> new ReentrantReadWriteLock()
        );

        lock.writeLock().lock();

        try {
            LoggerUtil.debug("Write lock acquired for auction: " + auctionId);
            return action.call();
        } finally {
            lock.writeLock().unlock();
            LoggerUtil.debug("Write lock released for auction: " + auctionId);
        }
    }

    public <T> T withAuctionReadLock(String auctionId, Callable<T> action) throws Exception {
        Objects.requireNonNull(action, "action");
        ReentrantReadWriteLock lock = auctionLocks.computeIfAbsent(
                requireAuctionId(auctionId),
                k -> new ReentrantReadWriteLock()
        );

        lock.readLock().lock();

        try {
            LoggerUtil.debug("Read lock acquired for auction: " + auctionId);
            return action.call();
        } finally {
            lock.readLock().unlock();
            LoggerUtil.debug("Read lock released for auction: " + auctionId);
        }
    }

    public void removeLock(String auctionId) {
        auctionLocks.remove(requireAuctionId(auctionId));
        LoggerUtil.debug("Lock removed for auction: " + auctionId);
    }

    public int getLockCount() {
        return auctionLocks.size();
    }

    private String requireAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("auctionId is required");
        }
        return auctionId;
    }
}
