package server.repository;

import server.model.Auction;

import java.util.List;

public interface AuctionRepository {
    List<Auction> getAllAuctions() throws Exception;

    List<Auction> getActiveAuctions() throws Exception;

    Auction getAuctionById(String auctionId) throws Exception;

    List<Auction> getAuctionsBySellerId(String sellerId) throws Exception;

    void createAuction(Auction auction) throws Exception;

    void saveAuction(Auction auction) throws Exception;

    void deleteAuction(String auctionId) throws Exception;
}
