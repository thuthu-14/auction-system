package server.repository;

import server.model.Bid;
import server.storage.BidDAO;

import java.util.List;

public class JsonBidRepository implements BidRepository {
    @Override
    public List<Bid> getAllBids() throws Exception {
        return BidDAO.getAllBids();
    }

    @Override
    public Bid getBidById(String bidId) throws Exception {
        return BidDAO.getBidById(bidId);
    }

    @Override
    public List<Bid> getBidsByAuctionId(String auctionId) throws Exception {
        return BidDAO.getBidsByAuctionId(auctionId);
    }

    @Override
    public List<Bid> getBidsByBidderId(String bidderId) throws Exception {
        return BidDAO.getBidsByBidderId(bidderId);
    }

    @Override
    public void saveBid(Bid bid) throws Exception {
        BidDAO.saveBid(bid);
    }
}
