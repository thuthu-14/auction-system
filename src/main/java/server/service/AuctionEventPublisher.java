package server.service;

import server.model.Auction;

public interface AuctionEventPublisher {
    void notifyAuctionCreated(Auction auction);

    void notifyAuctionFinished(Auction auction);

    void notifyAuctionCancelled(Auction auction);
}
