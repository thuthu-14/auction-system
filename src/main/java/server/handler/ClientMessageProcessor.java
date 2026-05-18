package server.handler;

import common.Message;
import server.AuctionServer;
import server.ClientHandler;
import server.model.User;

import java.io.IOException;

public class ClientMessageProcessor {
    private final ClientSessionContext context;
    private final AuthUserMessageProcessor authUserProcessor;
    private final AuctionBidMessageProcessor auctionBidProcessor;
    private final WalletMessageProcessor walletProcessor;
    private final NotificationMessageProcessor notificationProcessor;
    private final MediaMessageProcessor mediaProcessor;

    public ClientMessageProcessor(ClientHandler client, AuctionServer server) {
        this.context = new ClientSessionContext(client, server);
        this.authUserProcessor = new AuthUserMessageProcessor(context);
        this.auctionBidProcessor = new AuctionBidMessageProcessor(context);
        this.walletProcessor = new WalletMessageProcessor(context);
        this.notificationProcessor = new NotificationMessageProcessor(context);
        this.mediaProcessor = new MediaMessageProcessor(context);
    }

    public User getCurrentUser() {
        return context.getCurrentUser();
    }

    public void handleLogin(Message message) throws IOException, ClassNotFoundException {
        authUserProcessor.handleLogin(message);
    }

    public void handleRegister(Message message) throws IOException, ClassNotFoundException {
        authUserProcessor.handleRegister(message);
    }

    public void handleGetSellerContact(Message message) throws IOException, ClassNotFoundException {
        authUserProcessor.handleGetSellerContact(message);
    }

    public void handleGetAllUsers(Message message) throws IOException, ClassNotFoundException {
        authUserProcessor.handleGetAllUsers(message);
    }

    public void handleBanUser(Message message) throws IOException, ClassNotFoundException {
        authUserProcessor.handleBanUser(message);
    }

    public void handleUpdateUserStatus(Message message) throws IOException {
        authUserProcessor.handleUpdateUserStatus(message);
    }

    public void handleUpgradeSeller(Message message) throws IOException, ClassNotFoundException {
        authUserProcessor.handleUpgradeSeller(message);
    }

    public void handleCreateAuction(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleCreateAuction(message);
    }

    public void handleGetAuctions(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleGetAuctions(message);
    }

    public void handleGetAuctionDetail(Message message) throws IOException {
        auctionBidProcessor.handleGetAuctionDetail(message);
    }

    public void handlePlaceBid(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handlePlaceBid(message);
    }

    public void handleGetBidHistory(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleGetBidHistory(message);
    }

    public void handleGetUserBids(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleGetUserBids(message);
    }

    public void handleDeleteAuctionAdmin(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleDeleteAuctionAdmin(message);
    }

    public void handleCancelAuction(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleCancelAuction(message);
    }

    public void handleGetSellerAuctions(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleGetSellerAuctions(message);
    }

    public void handleCreateSellerItem(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleCreateSellerItem(message);
    }

    public void handleDeleteSellerItem(Message message) throws IOException, ClassNotFoundException {
        auctionBidProcessor.handleDeleteSellerItem(message);
    }

    public void handleAddFunds(Message message) throws IOException {
        walletProcessor.handleAddFunds(message);
    }

    public void handleWithdraw(Message message) throws IOException {
        walletProcessor.handleWithdraw(message);
    }

    public void handleLinkBank(Message message) throws IOException {
        walletProcessor.handleLinkBank(message);
    }

    public void handleGetTransactions(Message message) throws IOException {
        walletProcessor.handleGetTransactions(message);
    }

    public void handleGetBankAccount(Message message) throws IOException {
        walletProcessor.handleGetBankAccount(message);
    }

    public void handleGetNotifications(Message message) throws IOException {
        notificationProcessor.handleGetNotifications(message);
    }

    public void handleMarkNotificationsRead(Message message) throws IOException {
        notificationProcessor.handleMarkNotificationsRead(message);
    }

    public void handleUploadImage(Message message) throws IOException {
        mediaProcessor.handleUploadImage(message);
    }
}
