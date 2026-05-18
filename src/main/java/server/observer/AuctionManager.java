package server.observer;

import server.model.Auction;
import server.model.Bid;
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
        subscribe(new AuctionNotificationObserver());
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void subscribe(AuctionObserver observer) {
        if (observer != null && observers.stream().noneMatch(existing -> existing.getClass().equals(observer.getClass()))) {
            observers.add(observer);
            LoggerUtil.debug("Observer subscribed. Total observers: " + observers.size());
        }
    }

    public void unsubscribe(AuctionObserver observer) {
        observers.remove(observer);
        LoggerUtil.debug("Observer unsubscribed. Total observers: " + observers.size());
    }

    public void notifyAuctionCreated(Auction auction) {
        notifyObservers(auction, new AuctionEvent(AuctionEvent.AUCTION_CREATED));
        LoggerUtil.info("Notified: Auction created - " + auction.getAuctionId());
    }

    public void notifyAuctionUpdated(Auction auction) {
        notifyObservers(auction, new AuctionEvent(AuctionEvent.AUCTION_UPDATED));
    }

    public void notifyBidPlaced(Auction auction) {
        notifyBidPlaced(auction, null, null);
    }

    public void notifyBidPlaced(Auction auction, Bid bid, String previousHighestBidderId) {
        notifyObservers(auction, new AuctionEvent(AuctionEvent.BID_PLACED, bid, previousHighestBidderId));
        LoggerUtil.info("Notified: Bid placed on auction - " + auction.getAuctionId());
    }

    public void notifyAuctionFinished(Auction auction) {
        notifyObservers(auction, new AuctionEvent(AuctionEvent.AUCTION_FINISHED));
        LoggerUtil.info("Notified: Auction finished - " + auction.getAuctionId());
    }

    public void notifyAuctionCancelled(Auction auction) {
        notifyObservers(auction, new AuctionEvent(AuctionEvent.AUCTION_CANCELLED));
        LoggerUtil.info("Notified: Auction cancelled - " + auction.getAuctionId());
    }

    public void notifyPriceUpdate(Auction auction) {
        notifyObservers(auction, new AuctionEvent(AuctionEvent.PRICE_UPDATED));
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

    private void notifyObservers(Auction auction, AuctionEvent event) {
        if (auction == null || event == null) {
            return;
        }
        for (AuctionObserver observer : observers) {
            observer.update(auction, event);
        }
    }
}
