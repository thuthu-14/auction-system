package server.observer;

import server.model.Auction;
import util.LoggerUtil;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static AuctionManager instance;
    private List<AuctionObserver> observers;
    private Map<String, Auction> auctions;

    private AuctionManager() {
        this.observers = new CopyOnWriteArrayList<>();
        this.auctions = new ConcurrentHashMap<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void subscribe(AuctionObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
            LoggerUtil.debug("Observer subscribed. Total observers: " + observers.size());
        }
    }

    public void unsubscribe(AuctionObserver observer) {
        observers.remove(observer);
        LoggerUtil.debug("Observer unsubscribed. Total observers: " + observers.size());
    }

    public void notifyAuctionCreated(Auction auction) {
        for (AuctionObserver observer : observers) {
            observer.update(auction, "AUCTION_CREATED");
        }
        LoggerUtil.info("Notified: Auction created - " + auction.getAuctionId());
    }

    public void notifyAuctionUpdated(Auction auction) {
        for (AuctionObserver observer : observers) {
            observer.update(auction, "AUCTION_UPDATED");
        }
    }

    public void notifyBidPlaced(Auction auction) {
        for (AuctionObserver observer : observers) {
            observer.update(auction, "BID_PLACED");
        }
        LoggerUtil.info("Notified: Bid placed on auction - " + auction.getAuctionId());
    }

    public void notifyAuctionFinished(Auction auction) {
        for (AuctionObserver observer : observers) {
            observer.update(auction, "AUCTION_FINISHED");
        }
        LoggerUtil.info("Notified: Auction finished - " + auction.getAuctionId());
    }

    public void notifyPriceUpdate(Auction auction) {
        for (AuctionObserver observer : observers) {
            observer.update(auction, "PRICE_UPDATED");
        }
    }

    public void addAuction(Auction auction) {
        auctions.put(auction.getAuctionId(), auction);
    }

    public Auction getAuction(String auctionId) {
        return auctions.get(auctionId);
    }

    public void removeAuction(String auctionId) {
        auctions.remove(auctionId);
    }

    public Collection<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }
}
