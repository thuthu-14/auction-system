package client.logic;

import server.model.Auction;

@FunctionalInterface
public interface AuctionFilterStrategy {
    boolean accepts(Auction auction);

    default AuctionFilterStrategy and(AuctionFilterStrategy other) {
        return auction -> accepts(auction) && (other == null || other.accepts(auction));
    }
}
