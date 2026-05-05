package server.concurrency;

import util.LoggerUtil;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SyncLockManager {

    private static SyncLockManager instance;
    private ConcurrentHashMap<String, ReentrantReadWriteLock> locks;

    private SyncLockManager() {
        this.locks = new ConcurrentHashMap<>();
    }

    public static synchronized SyncLockManager getInstance() {
        if (instance == null) {
            instance = new SyncLockManager();
        }
        return instance;
    }

    public ReentrantReadWriteLock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantReadWriteLock());
    }

    public void readLock(String key) {
        getLock(key).readLock().lock();
    }

    public void readUnlock(String key) {
        getLock(key).readLock().unlock();
    }

    public void writeLock(String key) {
        getLock(key).writeLock().lock();
    }

    public void writeUnlock(String key) {
        getLock(key).writeLock().unlock();
    }

    public void removeLock(String key) {
        locks.remove(key);
        LoggerUtil.debug("Lock removed for key: " + key);
    }

    public int getLockCount() {
        return locks.size();
    }

    public void clear() {
        locks.clear();
        LoggerUtil.info("All locks cleared");
    }
}
