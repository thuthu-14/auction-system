package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.Auction;
import server.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminClientService implements AdminClient {

    public List<User> fetchAllUsers(MessageTransport transport, User adminUser) throws Exception {
        Message response = sendAdminRequest(transport, MessageType.GET_ALL_USERS, null, adminUser);
        if (response.getData() instanceof List<?> rawList) {
            List<User> users = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof User user) {
                    users.add(user);
                }
            }
            return users;
        }
        return new ArrayList<>();
    }

    public void updateUserStatus(MessageTransport transport, User adminUser, String userId, boolean active) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("active", active);
        sendAdminRequest(transport, MessageType.UPDATE_USER_STATUS, data, adminUser);
    }

    public List<Auction> fetchAllAuctions(MessageTransport transport, User adminUser) throws Exception {
        Message response = sendAdminRequest(transport, MessageType.GET_ALL_AUCTIONS, null, adminUser);
        if (response.getData() instanceof List<?> rawList) {
            List<Auction> auctions = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Auction auction) {
                    auctions.add(auction);
                }
            }
            return auctions;
        }
        return new ArrayList<>();
    }

    private Message sendAdminRequest(MessageTransport transport, MessageType type, Object data, User adminUser) throws Exception {
        if (transport == null || !transport.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }
        String sender = adminUser != null ? adminUser.getUsername() : "ADMIN";
        Message response = transport.sendAndReceive(new Message(type, data, sender));
        if (response == null) {
            throw new java.io.IOException("No response from server");
        }
        if (!"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response.getMessage() != null ? response.getMessage() : "Server request failed");
        }
        return response;
    }
}
