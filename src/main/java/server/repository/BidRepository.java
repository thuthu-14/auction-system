package server.repository;

import server.model.Bid;

import java.util.List;

public interface BidRepository {
    List<Bid> getAllBids() throws Exception;

    Bid getBidById(String bidId) throws Exception;

    List<Bid> getBidsByAuctionId(String auctionId) throws Exception;

    List<Bid> getBidsByBidderId(String bidderId) throws Exception;

    List<Bid> getBidsLinkedToUser(String userId) throws Exception;

    void saveBid(Bid bid) throws Exception;
}
