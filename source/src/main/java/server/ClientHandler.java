package server;

import common.LoginRequest;
import common.Message;
import common.MessageType;
import server.model.Admin;
import server.model.RegularUser;
import server.model.User;
import util.LoggerUtil;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final AuctionServer server;
    private final ObjectInputStream ois;
    private final ObjectOutputStream oos;
    private volatile boolean isConnected;
    private final String clientId;

    private User currentUser;

    public ClientHandler(Socket socket, AuctionServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.clientId = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();

        this.oos = new ObjectOutputStream(socket.getOutputStream());
        this.oos.flush();
        this.ois = new ObjectInputStream(socket.getInputStream());

        this.isConnected = true;
        LoggerUtil.info("ClientHandler created for " + clientId);
    }

    @Override
    public void run() {
        try {
            while (isConnected && socket.isConnected()) {
                try {
                    Message message = (Message) ois.readObject();
                    if (message != null) {
                        handleMessage(message);
                    }
                } catch (EOFException e) {
                    LoggerUtil.info("Client disconnected (EOF): " + clientId);
                    isConnected = false;
                } catch (ClassNotFoundException e) {
                    LoggerUtil.error("Invalid message class: " + e.getMessage());
                    sendError("Loi du lieu gui len server");
                }
            }
        } catch (IOException e) {
            LoggerUtil.error("ClientHandler IO error (" + clientId + "): " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(Message message) throws IOException {
        try {
            LoggerUtil.debug("Received message: " + message.getType() + " from " + clientId);

            switch (message.getType()) {
                case LOGIN:
                    handleLogin(message);
                    break;
                case REGISTER:
                    handleRegister(message);
                    break;
                case GET_AUCTIONS:
                    handleGetAuctions(message);
                    break;
                case CREATE_AUCTION:
                    handleCreateAuction(message);
                    break;
                case PLACE_BID:
                    handlePlaceBid(message);
                    break;
                case GET_BID_HISTORY:
                    handleGetBidHistory(message);
                    break;
                case GET_ALL_USERS:
                    handleGetAllUsers(message);
                    break;
                case BAN_USER:
                    handleBanUser(message);
                    break;
                case DELETE_AUCTION_ADMIN:
                    handleDeleteAuctionAdmin(message);
                    break;
                case UPGRADE_SELLER:
                    handleUpgradeSeller(message);
                    break;
                case GET_SELLER_AUCTIONS:
                    handleGetSellerAuctions(message);
                    break;
                case CREATE_SELLER_ITEM:
                    handleCreateSellerItem(message);
                    break;
                case DELETE_SELLER_ITEM:
                    handleDeleteSellerItem(message);
                    break;
                default:
                    sendError("Unknown message type: " + message.getType());
                    break;
            }
        } catch (Exception e) {
            LoggerUtil.error("Error handling message: " + e.getMessage());
            sendError("Loi server: " + e.getMessage());
        }
    }

    // ===== AUTH =====
    private void handleLogin(Message message) throws IOException {
        LoginRequest req = (LoginRequest) message.getData();
        // TODO: goi AuthService.login(req.getUsername(), req.getPassword())
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Login handler scaffold OK");
        sendMessage(response);
    }

    private void handleRegister(Message message) throws IOException {
        // TODO: parse Map<String, String> va goi AuthService.register(...)
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Register handler scaffold OK");
        sendMessage(response);
    }

    // ===== USER / AUCTION =====
    private void handleGetAuctions(Message message) throws IOException {
        // TODO: goi AuctionService.getActiveAuctions()
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Get auctions scaffold OK");
        sendMessage(response);
    }

    private void handleCreateAuction(Message message) throws IOException {
        if (!requireRegularUser()) return;
        // TODO: parse data va goi AuctionService.createAuction(...)
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Create auction scaffold OK");
        sendMessage(response);
    }

    private void handlePlaceBid(Message message) throws IOException {
        if (!requireRegularUser()) return;
        // TODO: goi ConcurrentBidManager/BidService de dat gia an toan
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Place bid scaffold OK");
        sendMessage(response);
    }

    private void handleGetBidHistory(Message message) throws IOException {
        // TODO: goi BidService.getBidHistory(...)
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Get bid history scaffold OK");
        sendMessage(response);
    }

    private void handleUpgradeSeller(Message message) throws IOException {
        if (!requireRegularUser()) return;
        // TODO: goi UserService.upgradeSeller(...)
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Upgrade seller scaffold OK");
        sendMessage(response);
    }

    private void handleGetSellerAuctions(Message message) throws IOException {
        if (!requireRegularUser()) return;
        // TODO: goi AuctionService.getAuctionsBySeller(...)
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Get seller auctions scaffold OK");
        sendMessage(response);
    }

    private void handleCreateSellerItem(Message message) throws IOException {
        if (!requireRegularUser()) return;
        // TODO: validate item + create auction
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Create seller item scaffold OK");
        sendMessage(response);
    }

    private void handleDeleteSellerItem(Message message) throws IOException {
        if (!requireRegularUser()) return;
        // TODO: check owner + delete auction
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Delete seller item scaffold OK");
        sendMessage(response);
    }

    // ===== ADMIN =====
    private void handleGetAllUsers(Message message) throws IOException {
        if (!requireAdmin()) return;
        // TODO: goi UserService.getAllUsers()
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Get all users scaffold OK");
        sendMessage(response);
    }

    private void handleBanUser(Message message) throws IOException {
        if (!requireAdmin()) return;
        // TODO: goi UserService.banUser(...)
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Ban user scaffold OK");
        sendMessage(response);
    }

    private void handleDeleteAuctionAdmin(Message message) throws IOException {
        if (!requireAdmin()) return;
        // TODO: goi AuctionService.deleteAuction(...)
        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Delete auction admin scaffold OK");
        sendMessage(response);
    }

    // ===== GUARDS =====
    private boolean requireRegularUser() throws IOException {
        if (!(currentUser instanceof RegularUser)) {
            sendError("Ban phai dang nhap voi tai khoan user!");
            return false;
        }
        return true;
    }

    private boolean requireAdmin() throws IOException {
        if (!(currentUser instanceof Admin)) {
            sendError("Chi Admin co quyen!");
            return false;
        }
        return true;
    }

    // ===== IO =====
    public synchronized void sendMessage(Message message) throws IOException {
        if (isConnected && socket.isConnected()) {
            oos.writeObject(message);
            oos.flush();
        }
    }

    private void sendError(String errorMsg) throws IOException {
        Message error = new Message(MessageType.ERROR, "ERROR", errorMsg);
        sendMessage(error);
    }

    public void closeConnection() {
        try {
            isConnected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            server.removeClient(this);
            LoggerUtil.info("Connection closed: " + clientId);
        } catch (IOException e) {
            LoggerUtil.error("Error closing connection: " + e.getMessage());
        }
    }
}
