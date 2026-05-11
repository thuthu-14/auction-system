package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.Bid;
import server.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AuctionBidClientService {
    private final LocalAuctionDataService localDataService;

    public AuctionBidClientService() {
        this(new LocalAuctionDataService());
    }

    public AuctionBidClientService(LocalAuctionDataService localDataService) {
        this.localDataService = localDataService;
    }

    public Message placeBid(MessageTransport transport, User user, String auctionId, double amount) throws Exception {
        ensureConnected(transport);
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("auctionId", auctionId);
        payload.put("amount", amount);

        return transport.sendAndReceive(new Message(MessageType.PLACE_BID, payload, user.getUsername()));
    }

    public Message fetchBidHistory(MessageTransport transport, User user, String auctionId) throws Exception {
        ensureConnected(transport);
        String sender = user != null ? user.getUsername() : "CLIENT";
        return transport.sendAndReceive(new Message(MessageType.GET_BID_HISTORY, auctionId, sender));
    }

    public List<Bid> fetchBidHistoryWithFallback(MessageTransport transport, User user, String auctionId) throws Exception {
        if (transport == null || !transport.isConnected()) {
            return localDataService.loadBidsForAuction(auctionId);
        }

        Message response = fetchBidHistory(transport, user, auctionId);
        if (response != null && "SUCCESS".equals(response.getStatus())) {
            List<Bid> bids = extractBidList(response.getData());
            return !bids.isEmpty() ? bids : localDataService.loadBidsForAuction(auctionId);
        }
        return localDataService.loadBidsForAuction(auctionId);
    }

    public List<Bid> loadLocalBidHistory(String auctionId) {
        return localDataService.loadBidsForAuction(auctionId);
    }

    public List<Bid> extractBidList(Object rawData) {
        if (!(rawData instanceof List<?> rawList)) {
            return List.of();
        }

        List<Bid> bids = new ArrayList<>();
        for (Object item : rawList) {
            if (item instanceof Bid bid) {
                bids.add(bid);
            }
        }
        return bids;
    }

    private void ensureConnected(MessageTransport transport) throws IOException {
        if (transport == null || !transport.isConnected()) {
            throw new IOException("Socket is not connected");
        }
    }
}
