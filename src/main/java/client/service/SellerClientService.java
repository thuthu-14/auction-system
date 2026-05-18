package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerClientService {
    private final DashboardClientService auctionExtractor = new DashboardClientService();

    public void upgradeSeller(MessageTransport transport, RegularUser user,
                              String shopName, String phone, String address, String email,
                              String password) throws Exception {
        ensureConnected(transport);
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", user.getUserId());
        payload.put("shopName", shopName);
        payload.put("phone", phone);
        payload.put("address", address);
        payload.put("email", email);
        payload.put("password", password);

        Message response = transport.sendAndReceive(new Message(MessageType.UPGRADE_SELLER, payload, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Upgrade seller failed");
        }
    }

    public List<Auction> fetchSellerAuctions(MessageTransport transport, User user) throws Exception {
        ensureConnected(transport);
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }
        Message response = transport.sendAndReceive(new Message(MessageType.GET_SELLER_AUCTIONS, null, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Cannot load seller auctions");
        }
        return auctionExtractor.extractAuctionList(response.getData());
    }

    public Auction fetchAuctionDetail(MessageTransport transport, User user, String auctionId) throws Exception {
        ensureConnected(transport);
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }
        Message response = transport.sendAndReceive(new Message(MessageType.GET_AUCTION_DETAIL, (Object) auctionId, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Cannot load auction detail");
        }
        if (response.getData() instanceof Auction auction) {
            return auction;
        }
        List<Auction> auctions = auctionExtractor.extractAuctionList(response.getData());
        return auctions.isEmpty() ? null : auctions.get(0);
    }

    public List<Bid> fetchBidHistory(MessageTransport transport, User user, String auctionId) throws Exception {
        ensureConnected(transport);
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }
        if (auctionId == null || auctionId.isBlank()) {
            throw new IllegalArgumentException("Missing auction id");
        }

        Message response = transport.sendAndReceive(new Message(MessageType.GET_BID_HISTORY, (Object) auctionId, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Cannot load bid history");
        }

        List<Bid> bids = new ArrayList<>();
        Object rawData = response.getData();
        if (rawData instanceof Bid bid) {
            bids.add(bid);
        } else if (rawData instanceof List<?> rawList) {
            for (Object item : rawList) {
                if (item instanceof Bid bid) {
                    bids.add(bid);
                }
            }
        }
        return bids;
    }

    public List<Bid> fetchBidHistoryWithFallback(MessageTransport transport, User user, String auctionId) throws Exception {
        return fetchBidHistory(transport, user, auctionId);
    }

    public List<Bid> loadLocalBidHistory(String auctionId) {
        return List.of();
    }

    public Auction cancelAuction(MessageTransport transport, User user, String auctionId) throws Exception {
        ensureConnected(transport);
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }
        if (auctionId == null || auctionId.isBlank() || "--".equals(auctionId)) {
            throw new IllegalArgumentException("Missing auction id");
        }

        Message response = transport.sendAndReceive(new Message(MessageType.CANCEL_AUCTION, (Object) auctionId, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Cannot cancel auction");
        }
        return response.getData() instanceof Auction auction ? auction : null;
    }

    public Map<String, String> fetchUserContact(MessageTransport transport, User user, String userId) throws Exception {
        ensureConnected(transport);
        if (user == null) {
            throw new IllegalStateException("Missing current user");
        }
        if (userId == null || userId.isBlank()) {
            return Map.of();
        }

        Message response = transport.sendAndReceive(new Message(MessageType.GET_SELLER_CONTACT, (Object) userId, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Cannot load user contact");
        }

        if (response.getData() instanceof Map<?, ?> rawMap) {
            Map<String, String> contact = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    contact.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            return contact;
        }
        return Map.of();
    }

    private void ensureConnected(MessageTransport transport) throws Exception {
        if (transport == null || !transport.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
    }

}
