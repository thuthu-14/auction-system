package server.storage;

import server.model.Bid;
import util.JsonUtil;
import util.SerializationUtil;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;

public class BidDAO {

    public static List<Bid> getAllBids() throws IOException, ClassNotFoundException {
        JsonUtil.createFileIfNotExists(DataManager.JSON_BIDS);
        List<Bid> bids = JsonUtil.loadListFromJson(DataManager.JSON_BIDS, Bid.class);
        return bids != null ? bids : new ArrayList<>();
    }

    public static Bid getBidById(String bidId) throws IOException, ClassNotFoundException {
        List<Bid> bids = getAllBids();
        for (Bid bid : bids) {
            if (bid.getBidId().equals(bidId)) {
                return bid;
            }
        }
        return null;
    }

    public static List<Bid> getBidsByAuctionId(String auctionId) throws IOException, ClassNotFoundException {
        List<Bid> bids = getAllBids();
        List<Bid> result = new ArrayList<>();
        for (Bid bid : bids) {
            if (bid.getAuctionId().equals(auctionId)) {
                result.add(bid);
            }
        }
        return result;
    }

    public static List<Bid> getBidsByBidderId(String bidderId) throws IOException, ClassNotFoundException {
        List<Bid> bids = getAllBids();
        List<Bid> result = new ArrayList<>();
        for (Bid bid : bids) {
            if (bid.getBidderId().equals(bidderId)) {
                result.add(bid);
            }
        }
        return result;
    }

    public static void saveBid(Bid bid) throws IOException, ClassNotFoundException {
        List<Bid> bids = getAllBids();
        bids.add(bid);

        JsonUtil.saveToJson(DataManager.JSON_BIDS, bids);
        SerializationUtil.serialize(DataManager.DAT_BIDS, bids);

        LoggerUtil.info("Bid saved: " + bid.getBidId());
    }

    public static void deleteBid(String bidId) throws IOException, ClassNotFoundException {
        List<Bid> bids = getAllBids();
        bids.removeIf(b -> b.getBidId().equals(bidId));

        JsonUtil.saveToJson(DataManager.JSON_BIDS, bids);
        SerializationUtil.serialize(DataManager.DAT_BIDS, bids);

        LoggerUtil.info("Bid deleted: " + bidId);
    }
}
