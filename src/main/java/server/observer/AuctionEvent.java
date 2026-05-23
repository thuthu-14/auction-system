package server.observer;

import server.model.Bid;

public class AuctionEvent {
    public static final String AUCTION_CREATED = "AUCTION_CREATED";
    public static final String AUCTION_UPDATED = "AUCTION_UPDATED";
    public static final String BID_PLACED = "BID_PLACED";
    public static final String AUCTION_FINISHED = "AUCTION_FINISHED";
    public static final String AUCTION_CANCELLED = "AUCTION_CANCELLED";
    public static final String PRICE_UPDATED = "PRICE_UPDATED";

    private final String type;
    private final Bid bid;
    private final String previousHighestBidderId;

    public AuctionEvent(String type) {
        this(type, null, null);
    }

    public AuctionEvent(String type, Bid bid, String previousHighestBidderId) {
        this.type = type;
        this.bid = bid;
        this.previousHighestBidderId = previousHighestBidderId;
    }

    public String getType() {
        return type;
    }

    public Bid getBid() {
        return bid;
    }

    public String getPreviousHighestBidderId() {
        return previousHighestBidderId;
    }
}
