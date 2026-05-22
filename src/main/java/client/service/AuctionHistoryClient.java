package client.service;

import client.network.MessageTransport;
import server.model.Auction;
import server.model.Bid;
import server.model.User;

import java.util.List;
import java.util.Map;

public interface AuctionHistoryClient {
    List<Bid> fetchUserBids(MessageTransport transport, User user) throws Exception;

    Auction fetchAuction(MessageTransport transport, User user, String auctionId);

    Map<String, String> fetchSellerContact(MessageTransport transport, User user, Auction auction);

    List<Bid> loadLocalBids(User user, boolean filterByCurrentUser);

    Map<String, Auction> loadLocalAuctionsById();

    Map<String, String> findLocalSellerContact(Auction auction);

    boolean isBidFromUser(Bid bid, User user);
}
