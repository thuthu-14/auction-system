package server.storage;

import server.model.Auction;
import common.AuctionStatus;
import util.JsonUtil;
import util.SerializationUtil;
import util.LoggerUtil;
import java.io.IOException;
import java.util.*;

public class AuctionDAO {

    public static List<Auction> getAllAuctions() throws IOException, ClassNotFoundException {
        JsonUtil.createFileIfNotExists(DataManager.JSON_AUCTIONS);
        List<Auction> auctions = JsonUtil.loadListFromJson(DataManager.JSON_AUCTIONS, Auction.class);
        return auctions != null ? auctions : new ArrayList<>();
    }

    public static Auction getAuctionById(String auctionId) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        for (Auction auction : auctions) {
            if (auction.getAuctionId().equals(auctionId)) {
                return auction;
            }
        }
        return null;
    }

    public static List<Auction> getAuctionsBySellerId(String sellerId) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        List<Auction> result = new ArrayList<>();
        for (Auction auction : auctions) {
            if (auction.getSellerId().equals(sellerId)) {
                result.add(auction);
            }
        }
        return result;
    }

    public static List<Auction> getActiveAuctions() throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        List<Auction> result = new ArrayList<>();
        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
                result.add(auction);
            }
        }
        return result;
    }

    public static void createAuction(Auction auction) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        auctions.add(auction);

        JsonUtil.saveToJson(DataManager.JSON_AUCTIONS, auctions);
        SerializationUtil.serialize(DataManager.DAT_AUCTIONS, auctions);

        LoggerUtil.info("Auction created: " + auction.getAuctionId());
    }

    public static void saveAuction(Auction auction) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();

        auctions.removeIf(a -> a.getAuctionId().equals(auction.getAuctionId()));

        auctions.add(auction);

        JsonUtil.saveToJson(DataManager.JSON_AUCTIONS, auctions);
        SerializationUtil.serialize(DataManager.DAT_AUCTIONS, auctions);

        LoggerUtil.info("Auction updated: " + auction.getAuctionId());
    }

    public static void deleteAuction(String auctionId) throws IOException, ClassNotFoundException {
        List<Auction> auctions = getAllAuctions();
        auctions.removeIf(a -> a.getAuctionId().equals(auctionId));

        JsonUtil.saveToJson(DataManager.JSON_AUCTIONS, auctions);
        SerializationUtil.serialize(DataManager.DAT_AUCTIONS, auctions);

        LoggerUtil.info("Auction deleted: " + auctionId);
    }
}
