package client.service;

import client.network.MessageTransport;
import server.model.Auction;

import java.util.List;

public interface AuctionQueryClient {
    List<Auction> fetchAllAuctions(MessageTransport transport) throws Exception;
}
