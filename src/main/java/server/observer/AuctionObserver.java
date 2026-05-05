package server.observer;

import server.model.Auction;

public interface AuctionObserver {

    void update(Auction auction, String updateType);
}
