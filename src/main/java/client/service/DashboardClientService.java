package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.Auction;

import java.util.ArrayList;
import java.util.List;

public class DashboardClientService implements AuctionQueryClient {
    @Override
    public List<Auction> fetchAllAuctions(MessageTransport transport) throws Exception {
        if (transport == null || !transport.isConnected()) {
            throw new java.io.IOException("Socket is not connected");
        }

        Message response = transport.sendAndReceive(new Message(MessageType.GET_ALL_AUCTIONS, (Object) null, "client"));
        if (response == null || !"SUCCESS".equals(response.getStatus())) {
            throw new java.io.IOException(response != null && response.getMessage() != null
                    ? response.getMessage()
                    : "Cannot load auctions");
        }
        return extractAuctionList(response.getData());
    }

    public List<Auction> extractAuctionList(Object rawData) {
        List<Auction> auctions = new ArrayList<>();
        if (rawData instanceof Auction auction) {
            auctions.add(auction);
            return auctions;
        }
        if (rawData instanceof List<?> rawList) {
            for (Object item : rawList) {
                if (item instanceof Auction auction) {
                    auctions.add(auction);
                }
            }
        }
        return auctions;
    }
}
