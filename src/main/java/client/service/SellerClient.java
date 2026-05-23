package client.service;

import client.network.MessageTransport;
import server.model.Auction;
import server.model.Bid;
import server.model.RegularUser;
import server.model.User;

import java.util.List;
import java.util.Map;

public interface SellerClient {
    void upgradeSeller(MessageTransport transport, RegularUser user, String shopName, String phone,
                       String address, String email, String password) throws Exception;

    List<Auction> fetchSellerAuctions(MessageTransport transport, User user) throws Exception;

    Auction fetchAuctionDetail(MessageTransport transport, User user, String auctionId) throws Exception;

    List<Bid> fetchBidHistory(MessageTransport transport, User user, String auctionId) throws Exception;

    List<Bid> fetchBidHistoryWithFallback(MessageTransport transport, User user, String auctionId) throws Exception;

    List<Bid> loadLocalBidHistory(String auctionId);

    Auction cancelAuction(MessageTransport transport, User user, String auctionId) throws Exception;

    Map<String, String> fetchUserContact(MessageTransport transport, User user, String userId) throws Exception;
}
