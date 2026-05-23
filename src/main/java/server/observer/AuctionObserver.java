package server.observer;

import server.model.Auction;

public interface AuctionObserver {

    void update(Auction auction, AuctionEvent event);
}
