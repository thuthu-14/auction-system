// src/main/java/client/network/AuctionListener.java
package client.network;

import server.model.Auction;
import server.model.Bid;

/**
 * AuctionListener - Interface cho các listener theo dõi thay đổi auction
 * Design Pattern: Observer
 * Mục đích: Notify UI khi auction thay đổi (real-time update)
 */
public interface AuctionListener {

    /**
     * Được gọi khi auction được cập nhật
     */
    void onAuctionUpdated(Auction auction);

    /**
     * Được gọi khi bid được đặt
     */
    void onBidPlaced(Bid bid);

    /**
     * Được gọi khi giá được cập nhật
     */
    void onPriceUpdated(Auction auction);

    /**
     * Được gọi khi auction kết thúc
     */
    void onAuctionFinished(Auction auction);

    /**
     * Được gọi khi có lỗi
     */
    void onError(String errorMessage);

    /**
     * Được gọi khi nhận notification
     */
    void onNotification(String message);

    /**
     * Được gọi khi bị vượt qua (outbid)
     */
    void onOutbid(Auction auction);

    /**
     * Được gọi khi auction được gia hạn (anti-sniping)
     */
    void onAuctionExtended(Auction auction);
}
