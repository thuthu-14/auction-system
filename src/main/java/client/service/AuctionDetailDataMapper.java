package client.service;

import com.google.gson.JsonObject;
import common.AuctionStatus;
import common.ItemCategory;
import server.model.Art;
import server.model.Auction;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Item;
import server.model.Jewelry;
import server.model.Vehicle;

public class AuctionDetailDataMapper {
    public JsonObject toDetailData(Auction auction) {
        JsonObject productData = new JsonObject();
        if (auction == null) {
            return productData;
        }

        Item item = auction.getItem();
        productData.addProperty("auctionId", auction.getAuctionId());
        productData.addProperty("sellerName", auction.getSellerName());
        productData.addProperty("currentPrice", auction.getCurrentPrice());

        if (item != null) {
            productData.addProperty("name", item.getName());
            productData.addProperty("description", item.getDescription());
            productData.addProperty("startingPrice", item.getStartingPrice());
            productData.addProperty("itemId", item.getItemId());
            productData.addProperty("type", item.getCategory() != null ? item.getCategory().name() : "");

            if (item instanceof Electronics electronics) {
                productData.addProperty("brand", electronics.getBrand());
                productData.addProperty("warrantyPeriod", electronics.getWarrantyPeriod());
            } else if (item instanceof Vehicle vehicle) {
                productData.addProperty("model", vehicle.getModel());
                productData.addProperty("odometer", vehicle.getOdometer());
            } else if (item instanceof Fashion fashion) {
                productData.addProperty("brand", fashion.getBrand());
                productData.addProperty("material", fashion.getMaterial());
            } else if (item instanceof Jewelry jewelry) {
                productData.addProperty("material", jewelry.getMaterial());
                productData.addProperty("weight", jewelry.getWeight());
            } else if (item instanceof Art art) {
                productData.addProperty("creator", art.getCreator());
                productData.addProperty("material", art.getMaterial());
            }
        }

        JsonObject rootData = new JsonObject();
        rootData.addProperty("auctionId", auction.getAuctionId());
        rootData.addProperty("currentPrice", auction.getCurrentPrice());
        rootData.addProperty("reservePrice", auction.getReservePrice());
        rootData.addProperty("minimumBidIncrement", auction.getMinimumBidIncrement());
        rootData.addProperty("sellerName", auction.getSellerName());
        rootData.add("item", productData);
        return rootData;
    }

    public boolean isAuctionEnded(Auction auction) {
        if (auction == null) {
            return true;
        }

        AuctionStatus status = auction.getStatus();
        return auction.getTimeRemainingSeconds() <= 0
                || status == AuctionStatus.CLOSED
                || status == AuctionStatus.FINISHED
                || status == AuctionStatus.CANCELLED;
    }

    public double resolveBidIncrement(String type) {
        try {
            return ItemCategory.valueOf(type).getMinimumBidIncrement();
        } catch (Exception e) {
            return 500000;
        }
    }
}
