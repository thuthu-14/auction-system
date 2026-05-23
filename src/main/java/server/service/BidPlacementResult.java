package server.service;

import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;

public class BidPlacementResult {
    private final Bid bid;
    private final Auction auction;
    private final RegularUser bidder;
    private final String previousHighestBidderId;

    public BidPlacementResult(Bid bid, Auction auction, RegularUser bidder, String previousHighestBidderId) {
        this.bid = bid;
        this.auction = auction;
        this.bidder = bidder;
        this.previousHighestBidderId = previousHighestBidderId;
    }

    public Bid getBid() {
        return bid;
    }

    public Auction getAuction() {
        return auction;
    }

    public RegularUser getBidder() {
        return bidder;
    }

    public String getPreviousHighestBidderId() {
        return previousHighestBidderId;
    }
}
