package server.service;

import java.util.Map;

public final class PlaceBidRequest {
    private final String auctionId;
    private final double amount;

    private PlaceBidRequest(String auctionId, double amount) {
        this.auctionId = auctionId;
        this.amount = amount;
    }

    public static PlaceBidRequest from(Object data) {
        Map<String, Object> payload = RequestPayloadUtil.objectMap(data);
        return new PlaceBidRequest(
                RequestPayloadUtil.requiredAuctionId(payload),
                RequestPayloadUtil.requiredDouble(payload, "amount")
        );
    }

    public String auctionId() {
        return auctionId;
    }

    public double amount() {
        return amount;
    }
}
