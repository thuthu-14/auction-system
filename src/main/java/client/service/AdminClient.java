package client.service;

import client.network.MessageTransport;
import server.model.Auction;
import server.model.User;

import java.util.List;

public interface AdminClient {
    List<User> fetchAllUsers(MessageTransport transport, User adminUser) throws Exception;

    void updateUserStatus(MessageTransport transport, User adminUser, String userId, boolean active) throws Exception;

    List<Auction> fetchAllAuctions(MessageTransport transport, User adminUser) throws Exception;
}
