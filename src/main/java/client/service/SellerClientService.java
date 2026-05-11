package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.Auction;
import server.model.RegularUser;
import server.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerClientService {
    private final DashboardClientService auctionExtractor = new DashboardClientService();

    public void upgradeSeller(MessageTransport transport, RegularUser user,
                              String shopName, String phone, String address, String email) throws Exception {
        ensureConnected(transport);
        Map<String, String> payload = new HashMap<>();
        payload.put("userId", user.getUserId());
        payload.put("shopName", shopName);
        payload.put("phone", phone);
        payload.put("address", address);
        payload.put("email", email);

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
        Message response = transport.sendAndReceive(new Message(MessageType.GET_AUCTION_DETAIL, auctionId, user.getUsername()));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Cannot load auction detail");
        }
        List<Auction> auctions = auctionExtractor.extractAuctionList(response.getData());
        return auctions.isEmpty() ? null : auctions.get(0);
    }

    private void ensureConnected(MessageTransport transport) throws Exception {
        if (transport == null || !transport.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
    }
}
