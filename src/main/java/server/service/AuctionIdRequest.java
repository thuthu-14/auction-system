package server.service;

public final class AuctionIdRequest {
    private final String auctionId;

    private AuctionIdRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public static AuctionIdRequest from(Object data) {
        return new AuctionIdRequest(RequestPayloadUtil.requiredAuctionId(data));
    }

    public String auctionId() {
        return auctionId;
    }
}
