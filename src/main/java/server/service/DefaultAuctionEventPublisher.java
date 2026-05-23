package server.service;

import server.model.Auction;
import server.observer.AuctionManager;

public class DefaultAuctionEventPublisher implements AuctionEventPublisher {
    @Override
    public void notifyAuctionCreated(Auction auction) {
        AuctionManager.getInstance().notifyAuctionCreated(auction);
    }

    @Override
    public void notifyAuctionFinished(Auction auction) {
        AuctionManager.getInstance().notifyAuctionFinished(auction);
    }

    @Override
    public void notifyAuctionCancelled(Auction auction) {
        AuctionManager.getInstance().notifyAuctionCancelled(auction);
    }
}
