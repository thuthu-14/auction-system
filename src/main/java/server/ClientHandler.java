
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
                case GET_ALL_AUCTIONS:
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
                case GET_USER_BIDS:
                    handleGetUserBids();
                    break;
                case GET_SELLER_CONTACT:
                    handleGetSellerContact(message);
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
                case GET_NOTIFICATIONS:
                    handleGetNotifications(message);
                    break;
                case MARK_NOTIFICATIONS_READ:
                    handleMarkNotificationsRead(message);
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

    private void handleGetAuctionDetail(Message message) {
        try {
            // 1. Kiểm tra dữ liệu đầu vào
            if (message.getData() == null) {
                sendError("ID phiên đấu giá không hợp lệ (null)");
                return;
            }

            String auctionId = message.getData().toString();
            LoggerUtil.info("Server đang tìm chi tiết cho phiên: " + auctionId);

            // 2. Lấy dữ liệu từ AuctionService
            Auction auction = AuctionService.getAuctionById(auctionId);

            if (auction != null) {
                // 3. Tạo phản hồi SUCCESS
                // Cấu trúc: Message(MessageType type, Object data, String senderId)
                Message response = new Message(MessageType.SUCCESS, auction, "SERVER");

                // Ép trạng thái SUCCESS để Client nhận diện đúng
                response.setStatus("SUCCESS");

                sendMessage(response);
                LoggerUtil.info("✓ Đã gửi dữ liệu phiên " + auctionId + " về Client.");
            } else {
                LoggerUtil.warn("! Không tìm thấy phiên ID: " + auctionId + " trong database.");
                sendError("Không tìm thấy phiên đấu giá này trên hệ thống!");
            }

        } catch (Exception e) {
            // Log lỗi chi tiết ra console của Server để debug
            e.printStackTrace();

            // Gửi thông báo lỗi về cho Client một cách an toàn
            try {
                sendError("Lỗi hệ thống Server: " + e.getMessage());
            } catch (Exception ex) {
                LoggerUtil.error("Lỗi nghiêm trọng: Không thể gửi thông báo lỗi qua Socket");
            }
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
            // SỬA LỖI 1: Gọi qua AuctionService thay vì AuctionDAO để đồng bộ RAM và File
            Auction auction = AuctionService.getAuctionById(auctionId);

            if (auction == null) {
                throw new Exception("Auction not found");
            }

            RegularUser bidder = (RegularUser) currentUser;
            String previousHighestBidderId = auction.getHighestBidderId();

            // Thực hiện đặt giá
            Bid bid = BidService.placeBid(bidder, auction, amount);

            // Tạo gói tin phản hồi
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Bid placed successfully");
            response.setData(bid);

            // SỬA LỖI 2: Ép cứng trạng thái SUCCESS để Client đọc được if("SUCCESS".equals(status))
            response.setStatus("SUCCESS");

            sendMessage(response);

            // Phát sóng Real-time cho mọi người đang xem
            server.broadcastMessage(new Message(MessageType.UPDATE_PRICE_REALTIME, auction, currentUser.getUserId()));
            server.broadcastMessage(new Message(MessageType.UPDATE_PRICE_REALTIME, bid, currentUser.getUserId()));

            // =========================================================
            // ĐÂY LÀ PHẦN QUAN TRỌNG NHẤT ĐÃ ĐƯỢC BỔ SUNG:
            // Lưu thông báo vào file JSON cho người bị vượt mặt
            // =========================================================
            if (previousHighestBidderId != null && !previousHighestBidderId.equals(bidder.getUserId())) {

                // 1. Tạo một object thông báo
                Notification outbidNoti = new Notification(
                        previousHighestBidderId,
                        "OUTBID",
                        "Bạn đã bị vượt giá!",
                        "Sản phẩm " + auction.getItem().getName() + " đã có người đặt mức giá mới: " + amount + "đ",
                        "Vừa xong",
                        "Đấu giá lại",
                        auctionId
                );

                // 2. Gọi NotificationDAO để ghi thông báo này xuống file notifications.json
                NotificationDAO.addNotification(outbidNoti);

                // 3. Vẫn phát sóng bình thường cho những ai đang online ngay lúc đó
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

    private void handleGetUserBids() throws IOException, ClassNotFoundException {
        if (currentUser == null) {
            sendError("Bạn phải đăng nhập!");
            return;
        }

        try {
            List<Bid> bids = BidService.getUserBids(currentUser.getUserId());
            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "User bids retrieved");
            response.setData(new ArrayList<>(bids));
            sendMessage(response);
        } catch (Exception e) {
            sendError("Không thể tải lịch sử đặt giá: " + e.getMessage());
        }
    }

    private void handleGetSellerContact(Message message) throws IOException, ClassNotFoundException {
        if (currentUser == null) {
            sendError("Bạn phải đăng nhập!");
            return;
        }

        String sellerId = message.getData() != null ? message.getData().toString() : "";
        if (sellerId.isBlank()) {
            sendError("Không tìm thấy người bán!");
            return;
        }

        try {
            User seller = UserService.getUserById(sellerId);
            if (seller == null) {
                sendError("Không tìm thấy người bán!");
                return;
            }

            Map<String, String> contact = new HashMap<>();
            contact.put("name", nonBlank(seller.getUsername(), "Người bán"));
            contact.put("email", nonBlank(seller.getEmail(), "Chưa cập nhật"));
            contact.put("phone", "Chưa cập nhật");

            if (seller instanceof RegularUser regularSeller) {
                contact.put("name", nonBlank(regularSeller.getShopName(), seller.getUsername()));
                contact.put("email", nonBlank(regularSeller.getShopEmail(), seller.getEmail()));
                contact.put("phone", nonBlank(regularSeller.getShopPhone(), "Chưa cập nhật"));
            }

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Seller contact retrieved");
            response.setData(contact);
            sendMessage(response);
        } catch (Exception e) {
            sendError("Không thể tải thông tin liên hệ: " + e.getMessage());
        }
    }

    private String nonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
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
            sendError("Bạn phải đăng nhập!");
            return;
        }

        try {
            RegularUser regularUser = (RegularUser) currentUser;

            if (regularUser.isSeller()) {
                sendError("Bạn đã là Seller rồi!");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, String> sellerData = (Map<String, String>) message.getData();

            String shopName = sellerData.get("shopName");
            String shopPhone = sellerData.get("phone");
            String shopAddress = sellerData.get("address");
            String shopEmail = sellerData.get("email");

            UserService.upgradeSeller(currentUser.getUserId(), shopName, shopPhone, shopAddress, shopEmail);
            regularUser.upgradeSeller(shopName, shopPhone, shopAddress, shopEmail);
            this.currentUser = regularUser;

            Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Upgraded to Seller successfully");
            response.setData(regularUser);
            sendMessage(response);

            LoggerUtil.info("✓ User upgraded to Seller: " + regularUser.getUsername() + " - Shop: " + shopName);

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

            // 1. SỬA LỖI "NAME IS NULL" TẠI ĐÂY (Lấy type linh hoạt)
            String itemType = (String) auctionData.get("itemType");
            if (itemType == null) itemType = (String) auctionData.get("Type");
            if (itemType == null) itemType = (String) auctionData.get("type");

            if (itemType == null) {
                sendError(" Thiếu loại sản phẩm (Category).");
                return;
            }
            itemType = itemType.toUpperCase();

            // Ép kiểu Enum an toàn
            common.ItemCategory category;
            try {
                category = common.ItemCategory.valueOf(itemType);
            } catch (IllegalArgumentException e) {
                sendError(" Loại sản phẩm không hợp lệ: " + itemType);
                return;
            }

            String name = (String) auctionData.get("name");
            String description = (String) auctionData.get("description");

            // 2. LẤY GIÁ TIỀN VÀ THỜI GIAN LINH HOẠT TỪ CLIENT
            Number priceNum = (Number) auctionData.get("price");
            if (priceNum == null) priceNum = (Number) auctionData.get("startingPrice");
            if (priceNum == null) { sendError(" Thiếu giá khởi điểm"); return; }
            double price = priceNum.doubleValue();

            Number durationNum = (Number) auctionData.get("duration");
            if (durationNum == null) { sendError(" Thiếu thời gian đấu giá"); return; }
            int duration = durationNum.intValue();

            // Validate chung
            if (!util.ValidationUtil.isValidItemName(name)) {
                sendError(" Tên sản phẩm không hợp lệ (3-200 ký tự)"); return;
            }
            if (!util.ValidationUtil.isValidDescription(description)) {
                sendError(" Mô tả sản phẩm không hợp lệ (tối đa 1000 ký tự)"); return;
            }

            String priceError = util.ItemValidationUtil.getStartingPriceErrorMessage(category, price);
            if (priceError != null) { sendError(" " + priceError); return; }

            String durationError = util.ItemValidationUtil.getDurationErrorMessage(category, duration);
            if (durationError != null) { sendError(" " + durationError); return; }

            // Validate riêng từng loại
            switch (itemType) {
                case "ELECTRONICS":
                    String brand = (String) auctionData.get("brand");
                    String warranty = (String) auctionData.get("warranty");
                    if (warranty == null) warranty = (String) auctionData.get("warrantyPeriod");
                    if (!util.ItemValidationUtil.isValidBrand(brand)) { sendError(" Hãng sản xuất không hợp lệ"); return; }
                    if (!util.ItemValidationUtil.isValidWarrantyPeriod(warranty)) { sendError(" Thời gian bảo hành không hợp lệ"); return; }
                    break;
                case "ART":
                    String creator = (String) auctionData.get("creator");
                    String artMaterial = (String) auctionData.get("material");
                    if (!util.ItemValidationUtil.isValidBrand(creator)) { sendError(" Tên người tạo không hợp lệ"); return; }
                    if (!util.ItemValidationUtil.isValidMaterial(artMaterial)) { sendError(" Chất liệu không hợp lệ"); return; }
                    break;
                case "VEHICLE":
                    String model = (String) auctionData.get("model");
                    int odometer = Integer.parseInt(String.valueOf(auctionData.get("odometer")).trim());
                    if (!util.ItemValidationUtil.isValidBrand(model)) { sendError(" Đời xe không hợp lệ"); return; }
                    if (!util.ItemValidationUtil.isValidOdometer(odometer)) { sendError(" Số km không hợp lệ"); return; }
                    break;
                case "FASHION":
                    String fashionBrand = (String) auctionData.get("brand");
                    String fashionMaterial = (String) auctionData.get("material");
                    if (!util.ItemValidationUtil.isValidBrand(fashionBrand)) { sendError(" Hãng không hợp lệ"); return; }
                    if (!util.ItemValidationUtil.isValidMaterial(fashionMaterial)) { sendError(" Chất liệu không hợp lệ"); return; }
                    break;
                case "JEWELRY":
                    String jewelryMaterial = (String) auctionData.get("material");
                    double weight = Double.parseDouble(String.valueOf(auctionData.get("weight")).trim());
                    if (!util.ItemValidationUtil.isValidMaterial(jewelryMaterial)) { sendError(" Chất liệu không hợp lệ"); return; }
                    if (!util.ItemValidationUtil.isValidWeight(weight)) { sendError(" Trọng lượng không hợp lệ"); return; }
                    break;
                default:
                    sendError(" Loại sản phẩm không hợp lệ");
                    return;
            }

            // Mọi thứ OK -> Tạo sản phẩm
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
            e.printStackTrace(); // In lỗi ra Console Server để dễ tìm
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

    private Item createItemFromData(String itemType, Map<String, Object> data) throws Exception {
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        String sellerId = currentUser.getUserId();

        // 1. Lấy giá tiền an toàn (hỗ trợ cả "price" và "startingPrice")
        Number priceNum = (Number) data.get("price");
        if (priceNum == null) priceNum = (Number) data.get("startingPrice");
        double price = (priceNum != null) ? priceNum.doubleValue() : 0.0;

        // 2. Lấy danh sách ảnh an toàn (tránh lỗi null nếu không có ảnh)
        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) data.get("images");
        if (images == null) {
            images = new java.util.ArrayList<>();
        }

        // 3. Xử lý từng loại linh hoạt và an toàn
        switch (itemType) {
            case "ELECTRONICS":
                String brand = (String) data.get("brand");
                String warranty = (String) data.get("warranty");
                if (warranty == null) warranty = (String) data.get("warrantyPeriod"); // Phòng hờ
                return new Electronics("", name, description, price, sellerId, brand, warranty, images);

            case "ART":
                String creator = (String) data.get("creator");
                String materialArt = (String) data.get("material");
                return new Art("", name, description, price, sellerId, creator, materialArt, images);

            case "VEHICLE":
                String model = (String) data.get("model");
                int odometer = 0;
                if (data.get("odometer") != null) {
                    odometer = Integer.parseInt(String.valueOf(data.get("odometer")).trim());
                }
                return new Vehicle("", name, description, price, sellerId, model, odometer, images);

            case "FASHION":
                String fashionBrand = (String) data.get("brand");
                String fashionMaterial = (String) data.get("material");
                return new Fashion("", name, description, price, sellerId, fashionBrand, fashionMaterial, images);

            case "JEWELRY":
                String jewelryMaterial = (String) data.get("material");
                double weight = 0.0;
                if (data.get("weight") != null) {
                    weight = Double.parseDouble(String.valueOf(data.get("weight")).trim());
                }
                return new Jewelry("", name, description, price, sellerId, jewelryMaterial, weight, images);

            default:
                throw new Exception("Unknown item type: " + itemType);
        }
    }

    public synchronized void sendMessage(Message message) throws IOException {
        if (isConnected && socket.isConnected()) {
            oos.reset();
            oos.writeObject(message);
            oos.flush();
            LoggerUtil.debug("Sent message: " + message.getType() + " to " + clientId);
        }
    }

    private void sendError(String errorMsg) throws IOException {
        Message errorMessage = new Message(MessageType.ERROR, "ERROR", errorMsg);
        sendMessage(errorMessage);
    }

    // ==========================================
    // TỐI ƯU HÓA: XỬ LÝ NẠP / RÚT TIỀN (VÍ)
    // ==========================================

    private void handleAddFunds(Message message) throws IOException, ClassNotFoundException {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) message.getData();
        String username = (String) data.get("username");
        double amount = ((Number) data.get("amount")).doubleValue();

        if (amount <= 0) {
            sendError("Số tiền nạp phải > 0");
            return;
        }

        // Cố gắng tìm User từ Database
        User user = UserDAO.getUserByUsername(username);

        // Fallback an toàn: Nếu Database trễ nhịp, lấy ngay phiên bản hiện tại trên bộ nhớ đệm
        if (user == null && currentUser != null && currentUser.getUsername().equals(username)) {
            user = currentUser;
        }

        if (user == null) {
            sendError("Không tìm thấy người dùng trong hệ thống");
            return;
        }

        // Thực hiện cộng tiền
        user.addFunds(amount);
        UserDAO.saveUser(user); // Cập nhật xuống cơ sở dữ liệu

        // Cập nhật lại phiên làm việc (Session)
        if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
            this.currentUser = user;
        }

        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Nạp tiền thành công");
        response.setData(user); // Gửi chính xác object User mới về cho Client
        sendMessage(response);

        LoggerUtil.info("✓ User added funds: " + username + " +" + amount);
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

        // Cố gắng tìm User từ Database
        User user = UserDAO.getUserByUsername(username);

        // Fallback an toàn
        if (user == null && currentUser != null && currentUser.getUsername().equals(username)) {
            user = currentUser;
        }

        if (user == null) {
            sendError("Không tìm thấy người dùng trong hệ thống");
            return;
        }

        // Thực hiện trừ tiền
        if (!user.deductFunds(amount)) {
            sendError("Số dư ví không đủ để thực hiện rút tiền");
            return;
        }

        UserDAO.saveUser(user); // Lưu lại thông tin ví mới xuống DB

        // Cập nhật phiên làm việc
        if (currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
            this.currentUser = user;
        }

        Message response = new Message(MessageType.SUCCESS, "SUCCESS", "Rút tiền thành công");
        response.setData(user);
        sendMessage(response);

        LoggerUtil.info("✓ User withdrew funds: " + username + " -" + amount);
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

    private void handleGetNotifications(Message message) throws IOException {
        // LUÔN LẤY userId từ tài khoản đang đăng nhập trên Server
        String userId = currentUser.getUserId();

        List<Notification> list = NotificationDAO.getNotificationsByUser(userId);

        Message response = new Message(MessageType.GET_NOTIFICATIONS, new ArrayList<>(list), "SERVER");
        sendMessage(response);
    }

    private void handleMarkNotificationsRead(Message message) throws IOException {
        String userId = currentUser.getUserId();
        NotificationDAO.markAllAsRead(userId);

        Message response = new Message(MessageType.MARK_NOTIFICATIONS_READ, "SUCCESS", "Đã đọc tất cả");
        sendMessage(response);
    }
}
