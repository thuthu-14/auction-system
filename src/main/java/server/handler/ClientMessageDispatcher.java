package server.handler;

import common.Message;
import common.MessageType;

import java.util.EnumMap;
import java.util.Map;

public class ClientMessageDispatcher {
    private final Map<MessageType, MessageAction> routes = new EnumMap<>(MessageType.class);

    public ClientMessageDispatcher(ClientMessageProcessor processor) {
        register(MessageType.LOGIN, processor::handleLogin);
        register(MessageType.REGISTER, processor::handleRegister);
        register(MessageType.CREATE_AUCTION, processor::handleCreateAuction);
        register(MessageType.GET_AUCTIONS, processor::handleGetAuctions);
        register(MessageType.GET_ALL_AUCTIONS, processor::handleGetAuctions);
        register(MessageType.GET_AUCTION_DETAIL, processor::handleGetAuctionDetail);
        register(MessageType.UPLOAD_IMAGE, processor::handleUploadImage);
        register(MessageType.PLACE_BID, processor::handlePlaceBid);
        register(MessageType.GET_BID_HISTORY, processor::handleGetBidHistory);
        register(MessageType.GET_USER_BIDS, processor::handleGetUserBids);
        register(MessageType.GET_SELLER_CONTACT, processor::handleGetSellerContact);
        register(MessageType.GET_ALL_USERS, processor::handleGetAllUsers);
        register(MessageType.ADD_FUNDS, processor::handleAddFunds);
        register(MessageType.WITHDRAW, processor::handleWithdraw);
        register(MessageType.BAN_USER, processor::handleBanUser);
        register(MessageType.UPDATE_USER_STATUS, processor::handleUpdateUserStatus);
        register(MessageType.DELETE_AUCTION_ADMIN, processor::handleDeleteAuctionAdmin);
        register(MessageType.CANCEL_AUCTION, processor::handleCancelAuction);
        register(MessageType.UPGRADE_SELLER, processor::handleUpgradeSeller);
        register(MessageType.GET_SELLER_AUCTIONS, processor::handleGetSellerAuctions);
        register(MessageType.CREATE_SELLER_ITEM, processor::handleCreateSellerItem);
        register(MessageType.DELETE_SELLER_ITEM, processor::handleDeleteSellerItem);
        register(MessageType.GET_NOTIFICATIONS, processor::handleGetNotifications);
        register(MessageType.MARK_NOTIFICATIONS_READ, processor::handleMarkNotificationsRead);
        register(MessageType.LINK_BANK, processor::handleLinkBank);
        register(MessageType.GET_TRANSACTIONS, processor::handleGetTransactions);
        register(MessageType.GET_BANK_ACCOUNT, processor::handleGetBankAccount);
    }

    public void dispatch(Message message) throws Exception {
        MessageAction action = routes.get(message.getType());
        if (action == null) {
            throw new IllegalArgumentException("Unknown message type: " + message.getType());
        }
        action.handle(message);
    }

    private void register(MessageType type, MessageAction action) {
        routes.put(type, action);
    }

    @FunctionalInterface
    private interface MessageAction {
        void handle(Message message) throws Exception;
    }
}
