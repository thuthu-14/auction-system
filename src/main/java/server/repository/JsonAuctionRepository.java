package server.repository;

import server.model.Auction;
import server.storage.AuctionDAO;

import java.util.List;

public class JsonAuctionRepository implements AuctionRepository {
    @Override
    public List<Auction> getAllAuctions() throws Exception {
        return AuctionDAO.getAllAuctions();
    }

    @Override
    public List<Auction> getActiveAuctions() throws Exception {
        return AuctionDAO.getActiveAuctions();
    }

    @Override
    public Auction getAuctionById(String auctionId) throws Exception {
        return AuctionDAO.getAuctionById(auctionId);
    }

    @Override
    public List<Auction> getAuctionsBySellerId(String sellerId) throws Exception {
        return AuctionDAO.getAuctionsBySellerId(sellerId);
    }

    @Override
    public void createAuction(Auction auction) throws Exception {
        AuctionDAO.createAuction(auction);
    }

    @Override
    public void saveAuction(Auction auction) throws Exception {
        AuctionDAO.saveAuction(auction);
    }

    @Override
    public void deleteAuction(String auctionId) throws Exception {
        AuctionDAO.deleteAuction(auctionId);
    }
}
