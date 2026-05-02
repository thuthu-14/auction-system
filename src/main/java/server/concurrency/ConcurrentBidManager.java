package server.concurrency;

import server.model.RegularUser;
import server.model.Auction;
import server.model.Bid;
import server.service.BidService;
import server.exception.*;
import util.LoggerUtil;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

    public Bid placeBidSafely(
            RegularUser bidder,
            Auction auction,
            double amount)
            throws InvalidBidException, PermissionDeniedException,
            AuctionClosedException, InsufficientFundsException,
            IOException, ClassNotFoundException {

        String auctionId = auction.getAuctionId();

        ReentrantReadWriteLock lock = auctionLocks.computeIfAbsent(
                auctionId,
                k -> new ReentrantReadWriteLock()
        );

        lock.writeLock().lock();

        try {
            LoggerUtil.debug("Write lock acquired for auction: " + auctionId);
            return BidService.placeBid(bidder, auction, amount);
        } finally {
            lock.writeLock().unlock();
            LoggerUtil.debug("Write lock released for auction: " + auctionId);
        }
    }

    public void removeLock(String auctionId) {
        auctionLocks.remove(auctionId);
        LoggerUtil.debug("Lock removed for auction: " + auctionId);
    }

    public int getLockCount() {
        return auctionLocks.size();
    }
}
