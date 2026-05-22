package server.service;

import java.util.Map;

public final class CreateAuctionRequest {
    private final Map<String, Object> payload;

    private CreateAuctionRequest(Map<String, Object> payload) {
        this.payload = payload;
    }

    public static CreateAuctionRequest from(Object data) {
        return new CreateAuctionRequest(RequestPayloadUtil.objectMap(data));
    }

    public static CreateAuctionRequest from(Map<String, Object> payload) {
        return new CreateAuctionRequest(payload);
    }

    public Map<String, Object> payload() {
        return payload;
    }
}
