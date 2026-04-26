
package server;

import server.model.*;
import server.service.*;
import server.storage.*;
import server.exception.*;
import common.*;
import util.LoggerUtil;
import java.io.*;
import java.net.Socket;
import java.util.*;


public class ClientHandler implements Runnable {

    private Socket socket;
    private AuctionServer server;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private User currentUser;
    private String clientId;
    private volatile boolean isConnected;

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
                    LoggerUtil.error("ClassNotFoundException: " + e.getMessage());
                    sendError("Lỗi dữ liệu: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LoggerUtil.error("ClientHandler IO error (" + clientId + "): " + e.getMessage());
        } finally {
            closeConnection();
        }
    }


    private void handleMessage(Message message) throws IOException, ClassNotFoundException {
        try {
            LoggerUtil.debug("Received message: " + message.getType() + " from " + clientId);

            switch (message.getType()) {
                case LOGIN:
                    handleLogin(message);
                    break;
                case REGISTER:
                    handleRegister(message);
                    break;
                case CREATE_AUCTION:
                    handleCreateAuction(message);
                    break;
                case GET_AUCTIONS:
                    handleGetAuctions(message);
                    break;
                case GET_AUCTION_DETAIL:
                    handleGetAuctionDetail(message);
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
                case ADD_FUNDS:
                    handleAddFunds(message);
                    break;
                case WITHDRAW:
                    handleWithdraw(message);
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
                    LoggerUtil.warn("Unknown message type: " + message.getType());
            }
        } catch (Exception e) {
            LoggerUtil.error("Error handling message: " + e.getMessage());
            sendError("Lỗi server: " + e.getMessage());
        }
    }


    private void handleLogin(Message message) throws IOException, ClassNotFoundException {
        LoginRequest loginRequest = (LoginRequest) message.getData();

        try {
            User user = AuthService.login(loginRequest.getUsername(), loginRequest.getPassword());
            this.currentUser = user;

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Login successful");
            response.setData(user);
            sendMessage(response);

            LoggerUtil.info("User logged in: " + user.getUsername() + " (Role: " + user.getRole() + ")");

        } catch (AuthenticationException e) {
            sendError(e.getMessage());
        }
    }

    private void handleRegister(Message message) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<String, String> registerData = (Map<String, String>) message.getData();
        String username = registerData.get("username");
        String password = registerData.get("password");
        String email = registerData.get("email");

        try {
            User user = AuthService.register(username, password, email);
            this.currentUser = user;

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Registration successful");
            response.setData(user);
            sendMessage(response);

            LoggerUtil.info("User registered: " + username);

        } catch (AuthenticationException e) {
            sendError(e.getMessage());
        }
    }

    private void handleCreateAuction(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof RegularUser)) {
            sendError("Bạn phải đăng nhập!");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> auctionData = (Map<String, Object>) message.getData();

        try {
            String itemType = (String) auctionData.get("itemType");
            Item item = createItemFromData(itemType, auctionData);
            RegularUser seller = (RegularUser) currentUser;
            int duration = ((Number) auctionData.get("duration")).intValue();

            Auction auction = AuctionService.createAuction(seller, item, duration);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auction created");
            response.setData(auction);
            sendMessage(response);

            server.broadcastMessage(new Message(MessageType.UPDATE, auction, currentUser.getUserId()));

        } catch (PermissionDeniedException e) {
            sendError(e.getMessage());
        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }


    private void handleGetAuctions(Message message) throws IOException, ClassNotFoundException {
        try {
            List<Auction> auctions = AuctionService.getActiveAuctions();

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auctions retrieved");
            response.setData(new ArrayList<>(auctions));
            sendMessage(response);

        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }

    private void handleGetAuctionDetail(Message message) throws IOException, ClassNotFoundException {
        String auctionId = (String) message.getData();

        try {
            Auction auction = AuctionService.getAuctionById(auctionId);
            if (auction == null) {
                sendError("Auction không tìm thấy!");
                return;
            }

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auction detail retrieved");
            response.setData(auction);
            sendMessage(response);

        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }

    private void handlePlaceBid(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof RegularUser)) {
            sendError("Bạn phải đăng nhập!");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> bidData = (Map<String, Object>) message.getData();
        String auctionId = (String) bidData.get("auctionId");
        double amount = ((Number) bidData.get("amount")).doubleValue();

        try {
            Auction auction = AuctionDAO.getAuctionById(auctionId);
            if (auction == null) {
                throw new Exception("Auction not found");
            }

            RegularUser bidder = (RegularUser) currentUser;
            String previousHighestBidderId = auction.getHighestBidderId();

            Bid bid = BidService.placeBid(bidder, auction, amount);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Bid placed successfully");
            response.setData(bid);
            sendMessage(response);

            server.broadcastMessage(new Message(MessageType.UPDATE_PRICE_REALTIME, auction, currentUser.getUserId()));

            if (previousHighestBidderId != null &&
                    !previousHighestBidderId.equals(bidder.getUserId())) {
                server.broadcastMessage(new Message(MessageType.OUTBID_NOTIFICATION, auction, currentUser.getUserId()));
            }


        } catch (InvalidBidException | AuctionClosedException | PermissionDeniedException e) {
            sendError(e.getMessage());
        } catch (InsufficientFundsException e) {
            sendError("Bạn không có đủ tiền!");
        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }

    private void handleGetBidHistory(Message message) throws IOException, ClassNotFoundException {
        String auctionId = (String) message.getData();

        try {
            List<Bid> bids = BidService.getBidHistory(auctionId);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Bid history retrieved");
            response.setData(new ArrayList<>(bids));
            sendMessage(response);

        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }

    private void handleGetAllUsers(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof Admin)) {
            sendError("Chỉ Admin có quyền xem!");
            return;
        }

        try {
            List<User> users = UserService.getAllUsers();

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Users retrieved");
            response.setData(new ArrayList<>(users));
            sendMessage(response);

        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }

    private void handleBanUser(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof Admin)) {
            sendError("Chỉ Admin có quyền!");
            return;
        }

        String userId = (String) message.getData();

        try {
            UserService.banUser(userId);
            sendMessage(new Message(MessageType.SUCCESS, "SUCCESS", "User banned successfully"));

        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }

    private void handleDeleteAuctionAdmin(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof Admin)) {
            sendError("Chỉ Admin có quyền!");
            return;
        }

        String auctionId = (String) message.getData();

        try {
            AuctionService.deleteAuction(auctionId);
            sendMessage(new Message(MessageType.SUCCESS, "SUCCESS", "Auction deleted successfully"));

            server.broadcastMessage(new Message(MessageType.UPDATE, "Auction deleted", auctionId));

        } catch (Exception e) {
            sendError("❌" + e.getMessage());
        }
    }

    private void handleUpgradeSeller(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof RegularUser)) {
            sendError(" Bạn phải đăng nhập!");
            return;
        }

        try {
            RegularUser regularUser = (RegularUser) currentUser;

            if (regularUser.isSeller()) {
                sendError("Bạn đã là Seller rồi!");
                return;
            }

            UserService.upgradeSeller(currentUser.getUserId());
            regularUser.upgradeSeller();
            this.currentUser = regularUser;

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Upgraded to Seller successfully");
            response.setData(regularUser);
            sendMessage(response);

            LoggerUtil.info("✓ User upgraded to Seller: " + regularUser.getUsername());

        } catch (Exception e) {
            sendError("❌ " + e.getMessage());
        }
    }

    private void handleGetSellerAuctions(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof RegularUser)) {
            sendError(" Bạn phải đăng nhập!");
            return;
        }

        try {
            RegularUser regularUser = (RegularUser) currentUser;

            if (!regularUser.isSeller()) {
                sendError("Bạn không phải Seller!");
                return;
            }

            List<Auction> sellerAuctions = AuctionService.getAuctionsBySeller(regularUser.getUserId());

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Seller auctions retrieved");
            response.setData(new ArrayList<>(sellerAuctions));
            sendMessage(response);

            LoggerUtil.info("✓ Retrieved " + sellerAuctions.size() + " seller auctions");

        } catch (Exception e) {
            sendError("❌ " + e.getMessage());
        }
    }

    /**
     * Xử lý CREATE_SELLER_ITEM - Kiểm tra bid increment
     */
    private void handleCreateSellerItem(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof RegularUser)) {
            sendError(" Bạn phải đăng nhập!");
            return;
        }

        try {
            RegularUser seller = (RegularUser) currentUser;

            if (!seller.isSeller()) {
                sendError(" Bạn không phải Seller! Vui lòng nâng cấp trước.");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> auctionData = (Map<String, Object>) message.getData();

            String itemType = (String) auctionData.get("itemType");

            // Lấy category enum
            common.ItemCategory category = common.ItemCategory.valueOf(itemType);

            // Validate item data trước khi tạo
            String name = (String) auctionData.get("name");
            String description = (String) auctionData.get("description");
            double price = ((Number) auctionData.get("price")).doubleValue();
            int duration = ((Number) auctionData.get("duration")).intValue();

            // Kiểm tra name
            if (!util.ValidationUtil.isValidItemName(name)) {
                sendError(" Tên sản phẩm không hợp lệ (3-200 ký tự)");
                return;
            }

            // Kiểm tra description
            if (!util.ValidationUtil.isValidDescription(description)) {
                sendError(" Mô tả sản phẩm không hợp lệ (tối đa 1000 ký tự)");
                return;
            }

            // Kiểm tra starting price dựa trên category
            String priceError = util.ItemValidationUtil.getStartingPriceErrorMessage(category, price);
            if (priceError != null) {
                sendError(" " + priceError);
                return;
            }

            // Kiểm tra duration dựa trên category
            String durationError = util.ItemValidationUtil.getDurationErrorMessage(category, duration);
            if (durationError != null) {
                sendError(" " + durationError);
                return;
            }

            // Validate category-specific fields
            switch (itemType) {
                case "ELECTRONICS":
                    String brand = (String) auctionData.get("brand");
                    String warranty = (String) auctionData.get("warranty");
                    if (!util.ItemValidationUtil.isValidBrand(brand)) {
                        sendError(" Hãng sản xuất không hợp lệ");
                        return;
                    }
                    if (!util.ItemValidationUtil.isValidWarrantyPeriod(warranty)) {
                        sendError(" Thời gian bảo hành không hợp lệ");
                        return;
                    }
                    break;

                case "ART":
                    String creator = (String) auctionData.get("creator");
                    String artMaterial = (String) auctionData.get("material");
                    if (!util.ItemValidationUtil.isValidBrand(creator)) {
                        sendError(" Tên người tạo không hợp lệ");
                        return;
                    }
                    if (!util.ItemValidationUtil.isValidMaterial(artMaterial)) {
                        sendError(" Chất liệu không hợp lệ");
                        return;
                    }
                    break;

                case "VEHICLE":
                    String model = (String) auctionData.get("model");
                    int odometer = Integer.parseInt(String.valueOf(auctionData.get("odometer")).trim());
                    if (!util.ItemValidationUtil.isValidBrand(model)) {
                        sendError(" Đời xe không hợp lệ");
                        return;
                    }
                    if (!util.ItemValidationUtil.isValidOdometer(odometer)) {
                        sendError(" Số km không hợp lệ");
                        return;
                    }
                    break;

                case "FASHION":
                    String fashionBrand = (String) auctionData.get("brand");
                    String fashionMaterial = (String) auctionData.get("material");
                    if (!util.ItemValidationUtil.isValidBrand(fashionBrand)) {
                        sendError(" Hãng không hợp lệ");
                        return;
                    }
                    if (!util.ItemValidationUtil.isValidMaterial(fashionMaterial)) {
                        sendError(" Chất liệu không hợp lệ");
                        return;
                    }
                    break;

                case "JEWELRY":
                    String jewelryMaterial = (String) auctionData.get("material");
                    double weight = Double.parseDouble(String.valueOf(auctionData.get("weight")).trim());
                    if (!util.ItemValidationUtil.isValidMaterial(jewelryMaterial)) {
                        sendError(" Chất liệu không hợp lệ");
                        return;
                    }
                    if (!util.ItemValidationUtil.isValidWeight(weight)) {
                        sendError(" Trọng lượng không hợp lệ");
                        return;
                    }
                    break;

                default:
                    sendError(" Loại sản phẩm không hợp lệ");
                    return;
            }

            // Tạo item
            Item item = createItemFromData(itemType, auctionData);
            Auction auction = AuctionService.createAuction(seller, item, duration);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auction created successfully");
            response.setData(auction);
            sendMessage(response);

            server.broadcastMessage(new Message(MessageType.UPDATE, auction, currentUser.getUserId()));

            LoggerUtil.info("✓ Auction created by seller: " + auction.getAuctionId());

        } catch (PermissionDeniedException e) {
            sendError(e.getMessage());
        } catch (Exception e) {
            sendError("❌ " + e.getMessage());
        }
    }

    private void handleDeleteSellerItem(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null || !(currentUser instanceof RegularUser)) {
            sendError("Bạn phải đăng nhập!");
            return;
        }

        try {
            RegularUser seller = (RegularUser) currentUser;

            if (!seller.isSeller()) {
                sendError("Bạn không phải Seller!");
                return;
            }

            String auctionId = (String) message.getData();
            Auction auction = AuctionService.getAuctionById(auctionId);

            if (auction == null) {
                sendError(" Phiên đấu giá không tồn tại!");
                return;
            }

            if (!auction.getSellerId().equals(seller.getUserId())) {
                sendError(" Bạn không phải chủ của phiên đấu giá này!");
                return;
            }

            AuctionService.deleteAuction(auctionId);

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Auction deleted successfully");
            sendMessage(response);

            server.broadcastMessage(new Message(MessageType.UPDATE, "Auction deleted", auctionId));

            LoggerUtil.info("✓ Auction deleted by seller: " + auctionId);

        } catch (Exception e) {
            sendError("❌ " + e.getMessage());
        }
    }


    /**
     * Helper: Tạo Item từ data
     * Công thức chung cho mỗi category:
     * - ELECTRONICS: brand + warranty
     * - ART: creator + material
     * - VEHICLE: model + odometer
     * - FASHION: brand + material (NEW)
     * - JEWELRY: material + weight (NEW)
     */
    private Item createItemFromData(String itemType, Map<String, Object> data) throws Exception {
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        double price = ((Number) data.get("price")).doubleValue();
        String sellerId = currentUser.getUserId();

        switch (itemType) {
            case "ELECTRONICS":
                String brand = (String) data.get("brand");
                String warranty = (String) data.get("warranty");
                return new Electronics("", name, description, price, sellerId, brand, warranty);

            case "ART":
                String creator = (String) data.get("creator");
                String material = (String) data.get("material");
                return new Art("", name, description, price, sellerId, creator, material);

            case "VEHICLE":
                String model = (String) data.get("model");
                int odometer = ((Number) data.get("odometer")).intValue();
                return new Vehicle("", name, description, price, sellerId, model, odometer);

            // ← THÊM Fashion
            case "FASHION":
                String fashionBrand = (String) data.get("brand");
                String fashionMaterial = (String) data.get("material");
                return new Fashion("", name, description, price, sellerId, fashionBrand, fashionMaterial);

            // ← THÊM Jewelry
            case "JEWELRY":
                String jewelryMaterial = (String) data.get("material");
                double weight = ((Number) data.get("weight")).doubleValue();
                return new Jewelry("", name, description, price, sellerId, jewelryMaterial, weight);

            default:
                throw new Exception("Unknown item type: " + itemType);
        }
    }


    public synchronized void sendMessage(Message message) throws IOException {
        if (isConnected && socket.isConnected()) {
            oos.writeObject(message);
            oos.flush();
            LoggerUtil.debug("Sent message: " + message.getType() + " to " + clientId);
        }
    }

    private void sendError(String errorMsg) throws IOException {
        Message errorMessage = new Message(MessageType.ERROR, "ERROR", errorMsg);
        sendMessage(errorMessage);
    }


    private void handleAddFunds(Message message) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String username = (String) data.get("username");
        double amount = ((Number) data.get("amount")).doubleValue();

        if (amount <= 0) {
            sendError("Số tiền nạp phải > 0");
            return;
        }

        User user = UserDAO.getUserByUsername(username);
        if (user == null) {
            sendError("Không tìm thấy người dùng");
            return;
        }

        user.addFunds(amount);
        UserDAO.saveUser(user);

        if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
            currentUser = user;
        }

        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Nạp tiền thành công");
        response.setData(user);
        sendMessage(response);

        LoggerUtil.info("User added funds: " + username + " +" + amount);
    }

    private void handleWithdraw(Message message) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) message.getData();

        String username = (String) data.get("username");
        double amount = ((Number) data.get("amount")).doubleValue();

        if (amount <= 0) {
            sendError("Số tiền rút phải > 0");
            return;
        }

        User user = UserDAO.getUserByUsername(username);
        if (user == null) {
            sendError("Không tìm thấy người dùng");
            return;
        }

        if (!user.deductFunds(amount)) {
            sendError("Số dư không đủ");
            return;
        }

        UserDAO.saveUser(user);

        if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
            currentUser = user;
        }

        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Rút tiền thành công");
        response.setData(user);
        sendMessage(response);

        LoggerUtil.info("User withdrew funds: " + username + " -" + amount);
    }


    public void closeConnection() {
        try {
            isConnected = false;

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            server.removeClient(this);

            if (currentUser != null) {
                LoggerUtil.info("Connection closed for user: " + currentUser.getUsername());
            } else {
                LoggerUtil.info("Connection closed for client: " + clientId);
            }
        } catch (IOException e) {
            LoggerUtil.error("Error closing connection: " + e.getMessage());
        }
    }

}


