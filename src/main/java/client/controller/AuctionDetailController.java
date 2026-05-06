package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import com.google.gson.JsonObject;
import common.ItemCategory;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import navigation.NavigationManager;
import server.model.*;

// THÊM IMPORT CHO THỜI GIAN
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import util.DateTimeUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AuctionDetailController {
    private static final NumberFormat VND_FORMATTER =
            NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML private ImageView mainImageView;
    @FXML private GridPane dynamicDetailsGrid;
    @FXML private Label descriptionLabel;
    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label hourLabel;
    @FXML private Label minuteLabel;
    @FXML private Label secondLabel;
    @FXML private TextField bidAmountField;
    @FXML private LineChart<String, Number> bidHistoryChart;

    @FXML private Button decreaseBidBtn;
    @FXML private Button increaseBidBtn;
    @FXML private Button confirmBidBtn;

    private double currentPrice;
    private double startingPrice;
    private double minimumBidIncrement = 500000;
    private String currentAuctionId;

    // THÊM BIẾN ĐẾM NGƯỢC
    private Timeline countdownTimer;

    /**
     * Hàm hiển thị thông tin phiên đấu giá
     */
    public void loadAuctionData(Auction auction) {
        if (auction == null) {
            clearView();
            return;
        }

        Item item = auction.getItem();

        // 1. CHỈ XỬ LÝ LOAD ẢNH CHỌN TỪ MÁY (ĐƯỜNG DẪN TUYỆT ĐỐI)
        if (mainImageView != null && item != null) {
            List<String> imageList = item.getImages();
            if (imageList != null && !imageList.isEmpty()) {
                String rawPath = imageList.get(0); // Đường dẫn từ máy: C:\abc\image.png

                try {
                    // Chuyển đổi đường dẫn Windows sang định dạng URL mà JavaFX hiểu được
                    // Ví dụ: C:\Users\Admin -> file:/C:/Users/Admin
                    String formattedPath = rawPath.replace("\\", "/");
                    if (!formattedPath.startsWith("file:")) {
                        formattedPath = "file:/" + formattedPath;
                    }

                    // Load ảnh (true để load ngầm, không làm treo giao diện)
                    Image img = new Image(formattedPath, true);
                    mainImageView.setImage(img);

                } catch (Exception e) {
                    System.err.println("Lỗi load ảnh từ máy: " + e.getMessage());
                }
            }
        }

        // 2. LOGIC HIỂN THỊ DỮ LIỆU
        JsonObject json = new JsonObject();
        json.addProperty("auctionId", auction.getAuctionId());
        json.addProperty("sellerName", auction.getSellerName());
        json.addProperty("currentPrice", auction.getCurrentPrice());

        if (item != null) {
            json.addProperty("name", item.getName());
            json.addProperty("description", item.getDescription());
            json.addProperty("startingPrice", item.getStartingPrice());
            json.addProperty("itemId", item.getItemId());
            json.addProperty("type", item.getCategory() != null ? item.getCategory().name() : "");

            if (item instanceof Electronics e) {
                json.addProperty("brand", e.getBrand());
                json.addProperty("warrantyPeriod", e.getWarrantyPeriod());
            } else if (item instanceof Vehicle v) {
                json.addProperty("model", v.getModel());
                json.addProperty("odometer", v.getOdometer());
            } else if (item instanceof Fashion f) {
                json.addProperty("brand", f.getBrand());
                json.addProperty("material", f.getMaterial());
            } else if (item instanceof Jewelry j) {
                json.addProperty("material", j.getMaterial());
                json.addProperty("weight", j.getWeight());
            } else if (item instanceof Art a) {
                json.addProperty("creator", a.getCreator());
                json.addProperty("material", a.getMaterial());
            }
        }

        JsonObject rootData = new JsonObject();
        rootData.addProperty("auctionId", auction.getAuctionId());
        rootData.addProperty("currentPrice", auction.getCurrentPrice());
        rootData.addProperty("sellerName", auction.getSellerName());
        rootData.add("item", json);

        loadAuctionData(rootData);
        loadBidHistoryChart();

        // --- CHỈ SỬA Ở ĐÂY: GỌI HÀM TIMELINE ---
        startCountdown(auction.getEndTime());
    }

    // --- CÁC HÀM BỔ TRỢ KHÁC GIỮ NGUYÊN ---
    public void loadAuctionData(JsonObject itemData) {
        if (itemData == null) {
            clearView();
            return;
        }
        JsonObject productData = getObject(itemData, "item", itemData);
        productNameLabel.setText(getString(productData, "name", "Chưa có tên sản phẩm"));
        descriptionLabel.setText(getString(productData, "description", "Chưa có mô tả"));
        currentAuctionId = getString(itemData, "auctionId", "");
        startingPrice = getDouble(productData, "startingPrice", 0);
        if (currentPriceLabel != null) {
            currentPrice = getDouble(itemData, "currentPrice", startingPrice);
            currentPriceLabel.setText(formatVnd(currentPrice));
        }
        String type = getString(productData, "type", "");
        minimumBidIncrement = resolveBidIncrement(type);
        if (bidAmountField != null) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
        dynamicDetailsGrid.getChildren().clear();
        int rowIndex = 0;
        rowIndex = addDetailRowIfPresent("Người bán", itemData, "sellerName", rowIndex);
        rowIndex = addDetailRowIfPresent("Mã sản phẩm", productData, "itemId", rowIndex);
        rowIndex = addDetailRowIfPresent("Giá khởi điểm", formatVnd(getDouble(productData, "startingPrice", 0)), rowIndex);
        switch (type) {
            case "VEHICLE" -> {
                rowIndex = addDetailRowIfPresent("Đời xe", productData, "model", rowIndex);
                rowIndex = addDetailRowIfPresent("Số km", getInt(productData, "odometer", 0) + " km", rowIndex);
            }
            case "FASHION" -> {
                rowIndex = addDetailRowIfPresent("Thương hiệu", productData, "brand", rowIndex);
                rowIndex = addDetailRowIfPresent("Chất liệu", productData, "material", rowIndex);
            }
            case "ART" -> {
                rowIndex = addDetailRowIfPresent("Tác giả", productData, "creator", rowIndex);
                rowIndex = addDetailRowIfPresent("Chất liệu", productData, "material", rowIndex);
            }
            case "JEWELRY" -> {
                rowIndex = addDetailRowIfPresent("Chất liệu", productData, "material", rowIndex);
                rowIndex = addDetailRowIfPresent("Khối lượng", getDouble(productData, "weight", 0) + " gr", rowIndex);
            }
            case "ELECTRONICS" -> {
                rowIndex = addDetailRowIfPresent("Thương hiệu", productData, "brand", rowIndex);
                rowIndex = addDetailRowIfPresent("Bảo hành", productData, "warrantyPeriod", rowIndex);
            }
        }
    }

    @FXML private void handleDecreaseBid() {
        try {
            String currentText = bidAmountField.getText().replaceAll("[^0-9]", "");
            double currentBid = Double.parseDouble(currentText);
            double minAllowedBid = currentPrice + minimumBidIncrement;
            if (currentBid - minimumBidIncrement >= minAllowedBid) {
                currentBid -= minimumBidIncrement;
                bidAmountField.setText(formatPlainNumber(currentBid));
            }
        } catch (Exception e) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
    }

    @FXML private void handleIncreaseBid() {
        try {
            String currentText = bidAmountField.getText().replaceAll("[^0-9]", "");
            double currentBid = Double.parseDouble(currentText);
            currentBid += minimumBidIncrement;
            bidAmountField.setText(formatPlainNumber(currentBid));
        } catch (Exception e) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
    }

    @FXML private void handleConfirmBid() {
        double amount = parseBidAmount();
        double minimumAllowed = currentPrice + minimumBidIncrement;
        if (amount < minimumAllowed) {
            showAlert(Alert.AlertType.WARNING, "Giá đặt thấp", "Tối thiểu: " + formatVnd(minimumAllowed));
            return;
        }
        User currentUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        if (currentUser == null || socket == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại!");
            return;
        }
        confirmBidBtn.setDisable(true);
        confirmBidBtn.setText("ĐANG XỬ LÝ...");
        new Thread(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("auctionId", currentAuctionId);
                payload.put("amount", amount);
                Message request = new Message(MessageType.PLACE_BID, payload, currentUser.getUsername());
                socket.sendMessage(request);
                Message response = socket.receiveMessage();
                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Bạn đã đặt giá thành công!");
                        currentPrice = amount;
                        currentPriceLabel.setText(formatVnd(currentPrice));
                        bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
                        if (response.getData() instanceof Bid bid) {
                            appendBidToChart(bid);
                        }
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Thất bại", response != null ? response.getMessage() : "Lỗi server");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Mất kết nối server"));
            } finally {
                Platform.runLater(() -> {
                    confirmBidBtn.setDisable(false);
                    confirmBidBtn.setText("XÁC NHẬN ĐẶT GIÁ");
                });
            }
        }).start();
    }

    // =========================================
    // ĐÃ SỬA LẠI: LINK VỚI DATETIMEUTIL VÀ THÊM TIMELINE ĐỂ ĐẾM NGƯỢC REAL-TIME
    // =========================================
    public void startCountdown(long endTime) {
        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (!DateTimeUtil.isExpired(endTime)) {
                long remainingMs = endTime - System.currentTimeMillis();

                // DateTimeUtil.formatTimestamp trả về chuỗi "HH:mm:ss"
                String timeStr = DateTimeUtil.formatTimestamp(remainingMs);
                String[] parts = timeStr.split(":"); // Cắt chuỗi để gán vào 3 label riêng biệt

                Platform.runLater(() -> {
                    if (hourLabel != null) hourLabel.setText(parts[0]);
                    if (minuteLabel != null) minuteLabel.setText(parts[1]);
                    if (secondLabel != null) secondLabel.setText(parts[2]);
                });
            } else {
                Platform.runLater(() -> {
                    if (hourLabel != null) hourLabel.setText("00");
                    if (minuteLabel != null) minuteLabel.setText("00");
                    if (secondLabel != null) secondLabel.setText("00");
                    if (confirmBidBtn != null) confirmBidBtn.setDisable(true); // Khoá nút khi hết giờ
                });
                if (countdownTimer != null) countdownTimer.stop();
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }
    // =========================================

    private int addDetailRowIfPresent(String title, JsonObject data, String key, int rowIndex) { return addDetailRowIfPresent(title, getString(data, key, ""), rowIndex); }
    private int addDetailRowIfPresent(String title, String value, int rowIndex) {
        if (value == null || value.isBlank() || value.startsWith("0")) return rowIndex;
        Label tLabel = new Label(title);
        tLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px;");
        Label vLabel = new Label(value);
        vLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px; -fx-font-weight: bold;");
        dynamicDetailsGrid.add(tLabel, 0, rowIndex);
        dynamicDetailsGrid.add(vLabel, 1, rowIndex);
        return rowIndex + 1;
    }

    private void loadBidHistoryChart() {
        if (bidHistoryChart == null) {
            return;
        }

        bidHistoryChart.setAnimated(false);
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            updateBidHistoryChart(List.of());
            return;
        }

        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        User currentUser = NavigationManager.getInstance().getCurrentUser();
        if (socket == null || currentUser == null) {
            updateBidHistoryChart(List.of());
            return;
        }

        new Thread(() -> {
            try {
                Message request = new Message(MessageType.GET_BID_HISTORY, currentAuctionId, currentUser.getUsername());
                socket.sendMessage(request);
                Message response = socket.receiveMessage();

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    @SuppressWarnings("unchecked")
                    List<Bid> bids = (List<Bid>) response.getData();
                    Platform.runLater(() -> updateBidHistoryChart(bids != null ? bids : List.of()));
                } else {
                    Platform.runLater(() -> updateBidHistoryChart(List.of()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> updateBidHistoryChart(List.of()));
            }
        }).start();
    }

    private void updateBidHistoryChart(List<Bid> bids) {
        if (bidHistoryChart == null) {
            return;
        }

        List<Bid> sortedBids = new ArrayList<>(bids != null ? bids : List.of());
        sortedBids.sort(Comparator.comparingLong(Bid::getBidTime));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá theo lượt bid");

        double firstValue = startingPrice > 0 ? startingPrice : currentPrice;
        series.getData().add(new XYChart.Data<>("Giá đầu", firstValue));

        for (int i = 0; i < sortedBids.size(); i++) {
            Bid bid = sortedBids.get(i);
            series.getData().add(new XYChart.Data<>("Lượt " + (i + 1), bid.getAmount()));
        }

        bidHistoryChart.getData().setAll(series);
    }

    private void appendBidToChart(Bid bid) {
        if (bidHistoryChart == null || bid == null) {
            return;
        }

        if (bidHistoryChart.getData().isEmpty()) {
            updateBidHistoryChart(List.of(bid));
            return;
        }

        XYChart.Series<String, Number> series = bidHistoryChart.getData().get(0);
        int nextBidIndex = Math.max(1, series.getData().size());
        series.getData().add(new XYChart.Data<>("Lượt " + nextBidIndex, bid.getAmount()));
    }

    private double parseBidAmount() {
        try { return Double.parseDouble(bidAmountField.getText().replaceAll("[^0-9]", "")); }
        catch (Exception e) { return currentPrice + minimumBidIncrement; }
    }
    private void clearView() {
        if (productNameLabel != null) productNameLabel.setText("Không có dữ liệu");
        if (dynamicDetailsGrid != null) dynamicDetailsGrid.getChildren().clear();
    }
    private String formatVnd(double amount) { return VND_FORMATTER.format(Math.round(amount)) + " VNĐ"; }
    private String formatPlainNumber(double amount) { return VND_FORMATTER.format(Math.round(amount)); }
    private static String getString(JsonObject o, String k, String d) { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsString() : d; }
    private static int getInt(JsonObject o, String k, int d) { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsInt() : d; }
    private static double getDouble(JsonObject o, String k, double d) { return (o.has(k) && !o.get(k).isJsonNull()) ? o.get(k).getAsDouble() : d; }
    private static JsonObject getObject(JsonObject o, String k, JsonObject d) { return (o.has(k) && o.get(k).isJsonObject()) ? o.getAsJsonObject(k) : d; }
    private double resolveBidIncrement(String type) {
        try { return ItemCategory.valueOf(type).getMinimumBidIncrement(); }
        catch (Exception e) { return 500000; }
    }
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
