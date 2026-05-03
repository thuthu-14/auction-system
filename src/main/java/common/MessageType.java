package common;

import java.io.Serializable;

public enum MessageType implements Serializable {
    LOGIN, REGISTER, LOGOUT,

    // --- AUCTION QUẢN LÝ ---
    CREATE_AUCTION,
    GET_AUCTIONS,
    GET_ALL_AUCTIONS,      // ← THÊM: Để Buyer lấy danh sách sản phẩm trên Dashboard
    UPDATE_AUCTION,
    DELETE_AUCTION,
    GET_AUCTION_DETAIL,
    GET_SELLER_AUCTIONS,

    // --- ITEM (SELLER) ---
    CREATE_SELLER_ITEM,
    UPDATE_SELLER_ITEM,
    DELETE_SELLER_ITEM,

    // --- BIDDING ---
    PLACE_BID,
    GET_BID_HISTORY,

    // --- USER & ADMIN ---
    UPGRADE_SELLER,
    GET_ALL_USERS,
    BAN_USER,
    DELETE_AUCTION_ADMIN,

    // --- HỆ THỐNG & NOTIFY ---
    SUCCESS,
    ERROR,
    UPDATE,
    NOTIFICATION,
    OUTBID_NOTIFICATION,
    AUCTION_FINISHED_NOTIFICATION,
    AUCTION_EXTENDED_NOTIFICATION,
    UPDATE_PRICE_REALTIME,

    // --- VÍ TIỀN ---
    ADD_FUNDS,
    WITHDRAW,

    // --- CẬP NHẬT TRẠNG THÁI ---
    SELLER_AUCTIONS_UPDATED,
    GET_NOTIFICATIONS,
    MARK_NOTIFICATIONS_READ
}