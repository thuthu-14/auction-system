package client.service;

import client.network.MessageTransport;
import common.Message;
import server.model.Bid;
import server.model.User;

import java.util.List;

public interface AuctionBidClient {
    Message placeBid(MessageTransport transport, User user, String auctionId, double amount) throws Exception;

    Message fetchBidHistory(MessageTransport transport, User user, String auctionId) throws Exception;

    List<Bid> fetchBidHistoryWithFallback(MessageTransport transport, User user, String auctionId) throws Exception;

    List<Bid> loadLocalBidHistory(String auctionId);

    List<Bid> extractBidList(Object rawData);
}
