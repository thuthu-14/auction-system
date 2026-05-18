package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AuctionBidClientService;
import client.service.ImageDownloadService;
import com.google.gson.JsonObject;
import common.AuctionStatus;
import common.ItemCategory;
import common.Message;
import common.MessageType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Art;
import server.model.Auction;
import server.model.Bid;
import server.model.Electronics;
import server.model.Fashion;
import server.model.Item;
import server.model.Jewelry;
import server.model.User;
import server.model.Vehicle;
import util.DateTimeUtil;
import util.LoggerUtil;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionDetailController {
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final Map<String, List<Bid>> BID_HISTORY_CACHE = new ConcurrentHashMap<>();
    private static final Duration BID_HISTORY_REFRESH_INTERVAL = Duration.millis(500);

    @FXML private ImageView mainImageView;
    @FXML private HBox thumbnailBox;
    @FXML private GridPane dynamicDetailsGrid;
    @FXML private Label descriptionLabel;
    @FXML private Label productNameLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label hourLabel;
    @FXML private Label minuteLabel;
    @FXML private Label secondLabel;
    @FXML private TextField bidAmountField;
    @FXML private ScrollPane bidHistoryScrollPane;
    @FXML private LineChart<String, Number> bidHistoryChart;
    @FXML private CategoryAxis bidTurnAxis;
    @FXML private NumberAxis bidPriceAxis;
    @FXML private Button confirmBidBtn;
    @FXML private VBox bidActionBox;
    @FXML private Label endedAuctionLabel;

    private double currentPrice;
    private double startingPrice;
    private double minimumBidIncrement = 500000;
    private String currentAuctionId;
    private String currentItemId;
    private Timeline countdownTimer;
    private Timeline bidHistoryRefreshTimer;
    private final AtomicBoolean bidHistoryRequestInFlight = new AtomicBoolean(false);
    private boolean endedMode;
    private boolean initialBidHistoryPositioned;
    private boolean bidPriceAxisPinInstalled;
    private boolean fallbackBidHistoryRendered;
    private String lastBidHistorySignature;
    private final AuctionBidClientService bidClientService = new AuctionBidClientService();
    private final Map<String, DetailRenderer> detailRenderers = createDetailRenderers();

    @FunctionalInterface
    private interface DetailRenderer {
        int render(JsonObject productData, int rowIndex);
    }

    public void loadAuctionData(Auction auction) {
        stopBidHistoryRefresh();
        initialBidHistoryPositioned = false;
        fallbackBidHistoryRendered = false;
        lastBidHistorySignature = null;
        if (auction == null) {
            clearView();
            return;
        }

        Item item = auction.getItem();
        loadMainImage(item);

        JsonObject productData = new JsonObject();
        productData.addProperty("auctionId", auction.getAuctionId());
        productData.addProperty("sellerName", auction.getSellerName());
        productData.addProperty("currentPrice", auction.getCurrentPrice());

        if (item != null) {
            productData.addProperty("name", item.getName());
            productData.addProperty("description", item.getDescription());
            productData.addProperty("startingPrice", item.getStartingPrice());
            productData.addProperty("itemId", item.getItemId());
            productData.addProperty("type", item.getCategory() != null ? item.getCategory().name() : "");

            if (item instanceof Electronics e) {
                productData.addProperty("brand", e.getBrand());
                productData.addProperty("warrantyPeriod", e.getWarrantyPeriod());
            } else if (item instanceof Vehicle v) {
                productData.addProperty("model", v.getModel());
                productData.addProperty("odometer", v.getOdometer());
            } else if (item instanceof Fashion f) {
                productData.addProperty("brand", f.getBrand());
                productData.addProperty("material", f.getMaterial());
            } else if (item instanceof Jewelry j) {
                productData.addProperty("material", j.getMaterial());
                productData.addProperty("weight", j.getWeight());
            } else if (item instanceof Art a) {
                productData.addProperty("creator", a.getCreator());
                productData.addProperty("material", a.getMaterial());
            }
        }

        JsonObject rootData = new JsonObject();
        rootData.addProperty("auctionId", auction.getAuctionId());
        rootData.addProperty("currentPrice", auction.getCurrentPrice());
        rootData.addProperty("reservePrice", auction.getReservePrice());
        rootData.addProperty("minimumBidIncrement", auction.getMinimumBidIncrement());
        rootData.addProperty("sellerName", auction.getSellerName());
        rootData.add("item", productData);

        endedMode = endedMode || isAuctionEnded(auction);
        loadAuctionData(rootData);
        loadBidHistoryGraph();
        startBidHistoryRefresh();
        startCountdown(auction.getEndTime());
        applyEndedMode();
    }

    public void setEndedMode(boolean endedMode) {
        this.endedMode = endedMode;
        applyEndedMode();
    }

    public void loadAuctionData(JsonObject itemData) {
        if (itemData == null) {
            clearView();
            return;
        }

        JsonObject productData = getObject(itemData, "item", itemData);
        if (productNameLabel != null) {
            productNameLabel.setText(getString(productData, "name", "Chưa có tên sản phẩm"));
        }
        if (descriptionLabel != null) {
            descriptionLabel.setText(getString(productData, "description", "Chưa có mô tả"));
        }

        String incomingAuctionId = getString(itemData, "auctionId", "");
        boolean sameAuction = incomingAuctionId != null && incomingAuctionId.equals(currentAuctionId);
        currentAuctionId = incomingAuctionId;
        currentItemId = getString(productData, "itemId", "");
        startingPrice = getDouble(productData, "startingPrice", 0);
        double incomingCurrentPrice = getDouble(itemData, "currentPrice", startingPrice);
        double cachedHighestPrice = getCachedHighestPrice(currentAuctionId);
        currentPrice = sameAuction
                ? Math.max(Math.max(currentPrice, incomingCurrentPrice), cachedHighestPrice)
                : Math.max(incomingCurrentPrice, cachedHighestPrice);
        minimumBidIncrement = getDouble(itemData, "minimumBidIncrement", 0);
        if (minimumBidIncrement <= 0) {
            minimumBidIncrement = resolveBidIncrement(getString(productData, "type", ""));
        }

        if (currentPriceLabel != null) {
            currentPriceLabel.setText(formatVnd(currentPrice));
        }
        if (bidAmountField != null) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }

        renderProductDetails(itemData, productData);
    }

    @FXML private void handleDecreaseBid() {
        try {
            double currentBid = Double.parseDouble(bidAmountField.getText().replaceAll("[^0-9]", ""));
            double minAllowedBid = currentPrice + minimumBidIncrement;
            if (currentBid - minimumBidIncrement >= minAllowedBid) {
                bidAmountField.setText(formatPlainNumber(currentBid - minimumBidIncrement));
            }
        } catch (Exception e) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
    }

    @FXML private void handleIncreaseBid() {
        try {
            double currentBid = Double.parseDouble(bidAmountField.getText().replaceAll("[^0-9]", ""));
            bidAmountField.setText(formatPlainNumber(currentBid + minimumBidIncrement));
        } catch (Exception e) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
    }

    @FXML private void handleConfirmBid() {
        if (endedMode) {
            showAlert(Alert.AlertType.WARNING, "Đã kết thúc", "Phiên đấu giá này đã kết thúc.");
            return;
        }

        double amount = parseBidAmount();
        double minimumAllowed = currentPrice + minimumBidIncrement;
        if (amount < minimumAllowed) {
            showAlert(Alert.AlertType.WARNING, "Giá đặt thấp", "Tối thiểu: " + formatVnd(minimumAllowed));
            return;
        }

        User currentUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        if (currentUser == null || socket == null || !socket.isConnected()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng đăng nhập lại!");
            return;
        }
        if (currentUser.getWallet() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Chưa thể đấu giá", "Bạn cần liên kết ngân hàng và nạp tiền vào ví trước khi đấu giá.");
            return;
        }

        confirmBidBtn.setDisable(true);
        confirmBidBtn.setText("ĐANG XỬ LÝ...");
        new Thread(() -> {
            try {
                Message response = bidClientService.placeBid(socket, currentUser, currentAuctionId, amount);

                Platform.runLater(() -> {
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        applyLatestBidPrice(amount);
                        if (response.getData() instanceof Bid bid) {
                            mergeBidIntoHistory(bid);
                        } else {
                            refreshBidHistoryFromServer(currentAuctionId);
                        }
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Bạn đã xác nhận đấu giá thành công!");
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Thất bại", response != null ? response.getMessage() : "Lỗi server");
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Place bid failed: " + e.getMessage())));
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Mất kết nối server"));
            } finally {
                Platform.runLater(() -> {
                    confirmBidBtn.setDisable(endedMode);
                    confirmBidBtn.setText("XÁC NHẬN ĐẤU GIÁ");
                });
            }
        }, "PlaceBidThread").start();
    }

    public void startCountdown(long endTime) {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }

        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (!DateTimeUtil.isExpired(endTime)) {
                long remainingMs = endTime - System.currentTimeMillis();
                String[] parts = DateTimeUtil.formatTimestamp(remainingMs).split(":");
                Platform.runLater(() -> {
                    if (parts.length >= 3) {
                        if (hourLabel != null) hourLabel.setText(parts[0]);
                        if (minuteLabel != null) minuteLabel.setText(parts[1]);
                        if (secondLabel != null) secondLabel.setText(parts[2]);
                    }
                });
            } else {
                Platform.runLater(() -> {
                    if (hourLabel != null) hourLabel.setText("00");
                    if (minuteLabel != null) minuteLabel.setText("00");
                    if (secondLabel != null) secondLabel.setText("00");
                    setEndedMode(true);
                });
                if (countdownTimer != null) {
                    countdownTimer.stop();
                }
            }
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void loadBidHistoryGraph() {
        configureBidHistoryGraph();
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            showFallbackCurrentPriceGraph();
            return;
        }

        List<Bid> localBids = bidClientService.loadLocalBidHistory(currentAuctionId);
        if (!localBids.isEmpty()) {
            updateBidHistoryGraph(localBids, shouldAutoScrollInitialBidHistory());
        }

        List<Bid> cachedBids = BID_HISTORY_CACHE.get(currentAuctionId);
        if (cachedBids != null && !cachedBids.isEmpty()) {
            updateBidHistoryGraph(cachedBids, shouldAutoScrollInitialBidHistory());
        }
        refreshBidHistoryFromServer(currentAuctionId, true);
    }

    private void configureBidHistoryGraph() {
        if (bidHistoryChart == null) {
            return;
        }

        if (bidHistoryScrollPane != null) {
            bidHistoryScrollPane.setFitToWidth(false);
            bidHistoryScrollPane.setFitToHeight(true);
            bidHistoryScrollPane.setPannable(true);
            bidHistoryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            bidHistoryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }

        bidHistoryChart.setAnimated(false);
        bidHistoryChart.setCreateSymbols(true);
        bidHistoryChart.setLegendVisible(false);
        bidHistoryChart.setHorizontalGridLinesVisible(true);
        bidHistoryChart.setVerticalGridLinesVisible(true);
        bidHistoryChart.setAlternativeRowFillVisible(false);
        bidHistoryChart.setAlternativeColumnFillVisible(false);
        bidHistoryChart.setHorizontalZeroLineVisible(false);
        bidHistoryChart.setVerticalZeroLineVisible(false);

        if (bidTurnAxis != null) {
            bidTurnAxis.setAnimated(false);
            bidTurnAxis.setTickMarkVisible(true);
        }
        if (bidPriceAxis != null) {
            bidPriceAxis.setAnimated(false);
            bidPriceAxis.setAutoRanging(false);
            bidPriceAxis.setForceZeroInRange(false);
            bidPriceAxis.setMinorTickVisible(true);
            bidPriceAxis.setMinorTickCount(4);
        }
        installPinnedBidPriceAxis();
    }

    private void installPinnedBidPriceAxis() {
        if (bidPriceAxisPinInstalled || bidHistoryScrollPane == null || bidHistoryChart == null || bidPriceAxis == null) {
            return;
        }

        bidPriceAxisPinInstalled = true;
        ChangeListener<Object> axisPositionUpdater = (observable, oldValue, newValue) -> updatePinnedBidPriceAxis();
        bidHistoryScrollPane.hvalueProperty().addListener(axisPositionUpdater);
        bidHistoryScrollPane.viewportBoundsProperty().addListener(axisPositionUpdater);
        bidHistoryChart.widthProperty().addListener(axisPositionUpdater);
        bidHistoryChart.sceneProperty().addListener((observable, oldScene, newScene) -> Platform.runLater(this::updatePinnedBidPriceAxis));
        Platform.runLater(this::updatePinnedBidPriceAxis);
    }

    private void updatePinnedBidPriceAxis() {
        if (bidHistoryScrollPane == null || bidHistoryChart == null || bidPriceAxis == null) {
            return;
        }

        Bounds viewportBounds = bidHistoryScrollPane.getViewportBounds();
        double scrollableWidth = Math.max(0, bidHistoryChart.getBoundsInLocal().getWidth() - viewportBounds.getWidth());
        bidPriceAxis.setTranslateX(bidHistoryScrollPane.getHvalue() * scrollableWidth);

        bidPriceAxis.toFront();
        Node axisNode = bidHistoryChart.lookup(".axis:left");
        if (axisNode != null) {
            axisNode.toFront();
        }
    }

    private void startBidHistoryRefresh() {
        stopBidHistoryRefresh();
        bidHistoryRefreshTimer = new Timeline(new KeyFrame(BID_HISTORY_REFRESH_INTERVAL, event -> {
            if (currentAuctionId != null && !currentAuctionId.isBlank()) {
                consumeRealtimeBidMessages();
                refreshBidHistoryFromServer(currentAuctionId, false);
            }
        }));
        bidHistoryRefreshTimer.setCycleCount(Timeline.INDEFINITE);
        bidHistoryRefreshTimer.play();
    }

    private void stopBidHistoryRefresh() {
        if (bidHistoryRefreshTimer != null) {
            bidHistoryRefreshTimer.stop();
            bidHistoryRefreshTimer = null;
        }
    }

    private void refreshBidHistoryFromServer(String requestedAuctionId) {
        refreshBidHistoryFromServer(requestedAuctionId, false);
    }

    private void refreshBidHistoryFromServer(String requestedAuctionId, boolean allowFallbackWhenEmpty) {
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        User currentUser = NavigationManager.getInstance().getCurrentUser();
        if (socket == null || !socket.isConnected()) {
            List<Bid> localBids = bidClientService.loadLocalBidHistory(requestedAuctionId);
            if (!localBids.isEmpty()) {
                Platform.runLater(() -> updateBidHistoryGraph(localBids, false));
            } else if (allowFallbackWhenEmpty) {
                Platform.runLater(this::keepExistingOrShowFallback);
            }
            return;
        }
        if (!bidHistoryRequestInFlight.compareAndSet(false, true)) {
            return;
        }

        new Thread(() -> {
            try {
                Message response = bidClientService.fetchBidHistory(socket, currentUser, requestedAuctionId);
                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    List<Bid> bids = bidClientService.extractBidList(response.getData());
                    if (bids.isEmpty()) {
                        bids = bidClientService.loadLocalBidHistory(requestedAuctionId);
                    }
                    List<Bid> loadedBids = bids;
                    LoggerUtil.info("Bid history loaded for " + requestedAuctionId + ": " + bids.size() + " bids");
                    Platform.runLater(() -> {
                        if (requestedAuctionId.equals(currentAuctionId)) {
                            List<Bid> mergedBids = mergeWithCachedHistory(requestedAuctionId, loadedBids);
                            if (!mergedBids.isEmpty() || allowFallbackWhenEmpty || !shouldKeepExistingBidGraph()) {
                                updateBidHistoryGraph(mergedBids, allowFallbackWhenEmpty && shouldAutoScrollInitialBidHistory());
                            }
                        }
                    });
                } else if (allowFallbackWhenEmpty) {
                    List<Bid> localBids = bidClientService.loadLocalBidHistory(requestedAuctionId);
                    Platform.runLater(() -> {
                        if (!localBids.isEmpty()) {
                            updateBidHistoryGraph(localBids, allowFallbackWhenEmpty && shouldAutoScrollInitialBidHistory());
                        } else {
                            keepExistingOrShowFallback();
                        }
                    });
                }
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Bid history refresh failed: " + e.getMessage())));
                if (allowFallbackWhenEmpty) {
                    List<Bid> localBids = bidClientService.loadLocalBidHistory(requestedAuctionId);
                    Platform.runLater(() -> {
                        if (!localBids.isEmpty()) {
                            updateBidHistoryGraph(localBids, allowFallbackWhenEmpty && shouldAutoScrollInitialBidHistory());
                        } else {
                            keepExistingOrShowFallback();
                        }
                    });
                }
            } finally {
                bidHistoryRequestInFlight.set(false);
            }
        }, "BidHistoryRefreshThread").start();
    }

    private void consumeRealtimeBidMessages() {
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        if (socket == null) {
            return;
        }

        Message asyncMessage;
        while ((asyncMessage = socket.pollAsyncMessage()) != null) {
            if (asyncMessage.getType() != MessageType.UPDATE_PRICE_REALTIME) {
                continue;
            }

            Object data = asyncMessage.getData();
            if (data instanceof Bid bid && currentAuctionId != null && currentAuctionId.equals(bid.getAuctionId())) {
                mergeBidIntoHistory(bid, false);
            } else if (data instanceof Auction auction && currentAuctionId != null && currentAuctionId.equals(auction.getAuctionId())) {
                applyLatestBidPrice(auction.getCurrentPrice());
            }
        }
    }

    private void updateBidHistoryGraph(List<Bid> bids) {
        updateBidHistoryGraph(bids, false);
    }

    private void updateBidHistoryGraph(List<Bid> bids, boolean scrollToLatest) {
        if (bidHistoryChart == null) {
            return;
        }

        List<Bid> sortedBids = new ArrayList<>(bids != null ? bids : List.of());
        sortedBids.sort(Comparator.comparingLong(Bid::getBidTime));
        double firstValue = startingPrice > 0 ? startingPrice : currentPrice;
        String newSignature = buildBidHistorySignature(sortedBids, firstValue);
        if (newSignature.equals(lastBidHistorySignature)) {
            if (bidHistoryScrollPane != null && scrollToLatest) {
                Platform.runLater(() -> bidHistoryScrollPane.setHvalue(1.0));
            }
            return;
        }
        if (sortedBids.isEmpty() && fallbackBidHistoryRendered) {
            return;
        }
        if (sortedBids.isEmpty() && shouldKeepExistingBidGraph()) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá theo lượt đặt");

        XYChart.Data<String, Number> basePoint = new XYChart.Data<>("Giá đầu", firstValue);
        basePoint.setNode(createBidPoint("Giá đầu\n" + formatVnd(firstValue)));
        series.getData().add(basePoint);
        basePoint.setXValue("\u0110i\u1ec3m kh\u1edfi \u0111\u1ea7u");
        basePoint.setNode(createBidPoint("\u0110i\u1ec3m kh\u1edfi \u0111\u1ea7u\n" + formatVnd(firstValue)));
        fallbackBidHistoryRendered = sortedBids.isEmpty();

        if (sortedBids.isEmpty() && series.getData().isEmpty()) {
            XYChart.Data<String, Number> startingPoint = new XYChart.Data<>("Giá đầu", firstValue);
            startingPoint.setNode(createBidPoint("Giá đầu\n" + formatVnd(firstValue)));
            series.getData().add(startingPoint);
            fallbackBidHistoryRendered = true;
        } else {
            for (int i = 0; i < sortedBids.size(); i++) {
                Bid bid = sortedBids.get(i);
                String turnLabel = "Lần thứ " + (i + 1);
                XYChart.Data<String, Number> bidPoint = new XYChart.Data<>(turnLabel, bid.getAmount());
                bidPoint.setNode(createBidPoint(turnLabel
                        + "\nNgười đặt: " + safeText(bid.getBidderName(), "Không rõ")
                        + "\nGiá: " + formatVnd(bid.getAmount())));
                series.getData().add(bidPoint);
            }
        }

        if (bidTurnAxis != null) {
            bidTurnAxis.getCategories().setAll(series.getData().stream()
                    .map(XYChart.Data::getXValue)
                    .toList());
        }
        bidHistoryChart.getData().setAll(series);
        bidHistoryChart.setPrefWidth(calculateBidChartWidth(series.getData().size()));
        bidHistoryChart.setMinWidth(calculateBidChartWidth(series.getData().size()));
        bidHistoryChart.setMaxWidth(Region.USE_PREF_SIZE);
        updateBidPriceAxis(sortedBids, firstValue);
        lastBidHistorySignature = newSignature;

        if (!sortedBids.isEmpty()) {
            syncPriceFromBidHistory(sortedBids);
        }
        if (currentAuctionId != null && !currentAuctionId.isBlank()) {
            BID_HISTORY_CACHE.put(currentAuctionId, sortedBids);
        }
        if (bidHistoryScrollPane != null && scrollToLatest) {
            Platform.runLater(() -> bidHistoryScrollPane.setHvalue(1.0));
        }
    }

    private String buildBidHistorySignature(List<Bid> sortedBids, double firstValue) {
        StringBuilder signature = new StringBuilder();
        signature.append(Math.round(firstValue)).append('|');
        for (Bid bid : sortedBids != null ? sortedBids : List.<Bid>of()) {
            if (bid == null) {
                continue;
            }
            signature.append(safeText(bid.getBidId(), ""))
                    .append(':')
                    .append(safeText(bid.getAuctionId(), ""))
                    .append(':')
                    .append(safeText(bid.getBidderId(), ""))
                    .append(':')
                    .append(bid.getBidTime())
                    .append(':')
                    .append(Math.round(bid.getAmount()))
                    .append('|');
        }
        return signature.toString();
    }

    private boolean shouldKeepExistingBidGraph() {
        if (currentAuctionId != null) {
            List<Bid> cachedBids = BID_HISTORY_CACHE.get(currentAuctionId);
            if (cachedBids != null && !cachedBids.isEmpty()) {
                return true;
            }
        }
        return bidHistoryChart != null
                && !bidHistoryChart.getData().isEmpty()
                && !bidHistoryChart.getData().get(0).getData().isEmpty()
                && bidHistoryChart.getData().get(0).getData().size() > 1;
    }

    private void showFallbackCurrentPriceGraph() {
        if (bidHistoryChart == null) {
            return;
        }
        if (fallbackBidHistoryRendered) {
            return;
        }

        double firstValue = startingPrice > 0 ? startingPrice : currentPrice;
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá theo lượt đặt");
        XYChart.Data<String, Number> startingPoint = new XYChart.Data<>("Giá đầu", firstValue);
        startingPoint.setNode(createBidPoint("Giá đầu\n" + formatVnd(firstValue)));
        series.getData().add(startingPoint);
        bidHistoryChart.getData().setAll(series);
        bidHistoryChart.setPrefWidth(calculateBidChartWidth(series.getData().size()));
        bidHistoryChart.setMinWidth(calculateBidChartWidth(series.getData().size()));
        bidHistoryChart.setMaxWidth(Region.USE_PREF_SIZE);
        updateBidPriceAxis(List.of(), firstValue);
        fallbackBidHistoryRendered = true;
        lastBidHistorySignature = buildBidHistorySignature(List.of(), firstValue);
    }

    private double calculateBidChartWidth(int pointCount) {
        return Math.max(900, 180 + Math.max(pointCount, 1) * 90.0);
    }

    private StackPane createBidPoint(String tooltipText) {
        Circle outer = new Circle(6);
        outer.setStyle("-fx-fill: #f15a24;");
        Circle inner = new Circle(3);
        inner.setStyle("-fx-fill: white;");
        StackPane point = new StackPane(outer, inner);
        point.setMinSize(18, 18);
        point.setPrefSize(18, 18);
        point.setMaxSize(18, 18);
        Tooltip.install(point, new Tooltip(tooltipText));
        return point;
    }

    private void keepExistingOrShowFallback() {
        if (!shouldKeepExistingBidGraph()) {
            showFallbackCurrentPriceGraph();
        }
    }

    private boolean shouldAutoScrollInitialBidHistory() {
        if (initialBidHistoryPositioned) {
            return false;
        }
        initialBidHistoryPositioned = true;
        return true;
    }

    private void mergeBidIntoHistory(Bid bid) {
        mergeBidIntoHistory(bid, false);
    }

    private void mergeBidIntoHistory(Bid bid, boolean scrollToLatest) {
        if (bid == null || currentAuctionId == null || currentAuctionId.isBlank()) {
            return;
        }

        List<Bid> cachedBids = new ArrayList<>(BID_HISTORY_CACHE.getOrDefault(currentAuctionId, List.of()));
        cachedBids.removeIf(existingBid -> isSameBid(existingBid, bid));
        cachedBids.add(bid);
        cachedBids.sort(Comparator.comparingLong(Bid::getBidTime));
        BID_HISTORY_CACHE.put(currentAuctionId, cachedBids);
        fallbackBidHistoryRendered = false;
        updateBidHistoryGraph(cachedBids, scrollToLatest);
    }

    private List<Bid> mergeWithCachedHistory(String auctionId, List<Bid> serverBids) {
        List<Bid> mergedBids = new ArrayList<>();
        if (auctionId != null) {
            mergedBids.addAll(BID_HISTORY_CACHE.getOrDefault(auctionId, List.of()));
        }
        for (Bid bid : serverBids != null ? serverBids : List.<Bid>of()) {
            mergedBids.removeIf(existingBid -> isSameBid(existingBid, bid));
            mergedBids.add(bid);
        }
        mergedBids.sort(Comparator.comparingLong(Bid::getBidTime));
        if (auctionId != null && !auctionId.isBlank() && !mergedBids.isEmpty()) {
            BID_HISTORY_CACHE.put(auctionId, mergedBids);
        }
        return mergedBids;
    }

    private boolean isSameBid(Bid existingBid, Bid newBid) {
        if (existingBid == null || newBid == null) {
            return false;
        }
        if (existingBid.getBidId() != null && newBid.getBidId() != null) {
            return existingBid.getBidId().equals(newBid.getBidId());
        }
        return existingBid.getAuctionId() != null
                && existingBid.getAuctionId().equals(newBid.getAuctionId())
                && existingBid.getBidderId() != null
                && existingBid.getBidderId().equals(newBid.getBidderId())
                && existingBid.getBidTime() == newBid.getBidTime()
                && Double.compare(existingBid.getAmount(), newBid.getAmount()) == 0;
    }

    private String safeText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private void updateBidPriceAxis(List<Bid> sortedBids, double firstValue) {
        if (bidPriceAxis == null) {
            return;
        }

        double minValue = firstValue;
        double maxValue = firstValue;
        for (Bid bid : sortedBids) {
            minValue = Math.min(minValue, bid.getAmount());
            maxValue = Math.max(maxValue, bid.getAmount());
        }

        double spread = Math.max(maxValue - minValue, Math.max(maxValue * 0.02, minimumBidIncrement));
        double tickUnit = choosePriceTickUnit(spread);
        double padding = Math.max(tickUnit * 2, spread * 0.2);
        double lowerBound = Math.max(0, Math.floor((minValue - padding) / tickUnit) * tickUnit);
        double upperBound = Math.ceil((maxValue + padding) / tickUnit) * tickUnit;
        if (upperBound <= lowerBound) {
            upperBound = lowerBound + tickUnit * 6;
        }

        bidPriceAxis.setLowerBound(lowerBound);
        bidPriceAxis.setUpperBound(upperBound);
        bidPriceAxis.setTickUnit(tickUnit);
        Platform.runLater(this::updatePinnedBidPriceAxis);
    }

    private double choosePriceTickUnit(double spread) {
        double target = Math.max(spread / 7, 1);
        double[] units = {10_000, 25_000, 50_000, 100_000, 250_000, 500_000, 1_000_000, 2_500_000, 5_000_000, 10_000_000};
        for (double unit : units) {
            if (target <= unit) {
                return unit;
            }
        }
        return 25_000_000;
    }

    private void applyLatestBidPrice(double amount) {
        if (amount <= currentPrice) {
            return;
        }

        currentPrice = amount;
        if (currentPriceLabel != null) {
            currentPriceLabel.setText(formatVnd(currentPrice));
        }
        if (bidAmountField != null) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
    }

    private void syncPriceFromBidHistory(List<Bid> sortedBids) {
        if (sortedBids == null || sortedBids.isEmpty()) {
            return;
        }

        double latestBidAmount = sortedBids.get(sortedBids.size() - 1).getAmount();
        currentPrice = Math.max(currentPrice, latestBidAmount);
        if (currentPriceLabel != null) {
            currentPriceLabel.setText(formatVnd(currentPrice));
        }
        if (bidAmountField != null) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
    }

    private double getCachedHighestPrice(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return 0;
        }

        List<Bid> cachedBids = BID_HISTORY_CACHE.get(auctionId);
        if (cachedBids == null || cachedBids.isEmpty()) {
            return 0;
        }

        double highest = 0;
        for (Bid bid : cachedBids) {
            if (bid != null) {
                highest = Math.max(highest, bid.getAmount());
            }
        }
        return highest;
    }

    private void loadMainImage(Item item) {
        if (mainImageView == null || item == null || item.getImages() == null || item.getImages().isEmpty()) {
            if (thumbnailBox != null) {
                thumbnailBox.getChildren().clear();
            }
            return;
        }

        renderImageGallery(item.getImages());
    }

    private void renderImageGallery(List<String> imageIds) {
        if (mainImageView == null || imageIds == null || imageIds.isEmpty()) {
            return;
        }

        if (thumbnailBox != null) {
            thumbnailBox.getChildren().clear();
            thumbnailBox.setAlignment(Pos.CENTER_LEFT);
        }

        List<StackPane> thumbnailFrames = new ArrayList<>();

        // Load first image
        String firstImageId = imageIds.get(0);
        loadAndDisplayImage(firstImageId, mainImageView, true);

        if (thumbnailBox == null) {
            return;
        }

        // Create thumbnails
        for (int i = 0; i < imageIds.size(); i++) {
            final int index = i;
            final String imageId = imageIds.get(i);

            StackPane thumbnailFrame = createThumbnailFrame(imageId, index, thumbnailFrames);
            thumbnailFrames.add(thumbnailFrame);
            thumbnailBox.getChildren().add(thumbnailFrame);
        }
    }

    private StackPane createThumbnailFrame(String imageId, int index, List<StackPane> thumbnailFrames) {
        ImageView thumbnailView = new ImageView();
        thumbnailView.setFitWidth(78);
        thumbnailView.setFitHeight(78);
        thumbnailView.setPreserveRatio(true);
        thumbnailView.setSmooth(true);

        StackPane thumbnailFrame = new StackPane(thumbnailView);
        thumbnailFrame.setPrefSize(88, 88);
        thumbnailFrame.setMinSize(88, 88);
        thumbnailFrame.setMaxSize(88, 88);
        thumbnailFrame.setStyle(getThumbnailStyle(index == 0));

        // Load thumbnail
        new Thread(() -> {
            try {
                ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
                if (socket == null || !socket.isConnected()) return;

                byte[] imageBytes = ImageDownloadService.downloadImage(socket, imageId);
                Image thumbnailImage = new Image(new java.io.ByteArrayInputStream(imageBytes), 90, 90, true, true);

                Platform.runLater(() -> {
                    thumbnailView.setImage(thumbnailImage);
                    thumbnailFrame.setOnMouseEntered(event -> {
                        loadAndDisplayImage(imageId, mainImageView, false);
                        // Update style trực tiếp thay vì gọi selectThumbnail
                        thumbnailFrame.setStyle(getThumbnailStyle(true));
                    });
                });
            } catch (Exception e) {
                LoggerUtil.error("Failed to load thumbnail " + imageId + ": " + e.getMessage());
            }
        }, "LoadThumbnailThread-" + index).start();

        return thumbnailFrame;
    }

    private void loadAndDisplayImage(String imageId, ImageView imageView, boolean isFirst) {
        new Thread(() -> {
            try {
                ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
                if (socket == null || !socket.isConnected()) {
                    LoggerUtil.error("Socket not connected");
                    return;
                }

                byte[] imageBytes = ImageDownloadService.downloadImage(socket, imageId);
                Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
                Platform.runLater(() -> imageView.setImage(image));

            } catch (Exception e) {
                LoggerUtil.error("Failed to load image " + imageId + ": " + e.getMessage());
            }
        }, "LoadImageThread").start();
    }

    private void selectThumbnail(List<StackPane> thumbnailFrames, StackPane selectedFrame) {
        for (StackPane frame : thumbnailFrames) {
            frame.setStyle(getThumbnailStyle(frame == selectedFrame));
        }
    }

    private String getThumbnailStyle(boolean selected) {
        String borderColor = selected ? "#ef4444" : "#e5e7eb";
        int borderWidth = selected ? 3 : 1;
        return "-fx-background-color: white;"
                + "-fx-border-color: " + borderColor + ";"
                + "-fx-border-width: " + borderWidth + ";"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-padding: 4;"
                + "-fx-cursor: hand;";
    }



    private void renderProductDetails(JsonObject itemData, JsonObject productData) {
        if (dynamicDetailsGrid == null) {
            return;
        }

        dynamicDetailsGrid.getChildren().clear();
        int rowIndex = 0;
        rowIndex = addDetailRowIfPresent("Người bán", itemData, "sellerName", rowIndex);
        rowIndex = addDetailRowIfPresent("Mã sản phẩm", productData, "itemId", rowIndex);
        rowIndex = addDetailRowIfPresent("Giá khởi điểm", formatVnd(getDouble(productData, "startingPrice", 0)), rowIndex);

        DetailRenderer renderer = detailRenderers.get(getString(productData, "type", ""));
        if (renderer != null) {
            renderer.render(productData, rowIndex);
        }
    }

    private Map<String, DetailRenderer> createDetailRenderers() {
        Map<String, DetailRenderer> renderers = new java.util.HashMap<>();
        renderers.put("VEHICLE", (data, row) -> {
            row = addDetailRowIfPresent("Đời xe", data, "model", row);
            return addDetailRowIfPresent("Số km", getInt(data, "odometer", 0) + " km", row);
        });
        renderers.put("FASHION", (data, row) -> {
            row = addDetailRowIfPresent("Thương hiệu", data, "brand", row);
            return addDetailRowIfPresent("Chất liệu", data, "material", row);
        });
        renderers.put("ART", (data, row) -> {
            row = addDetailRowIfPresent("Tác giả", data, "creator", row);
            return addDetailRowIfPresent("Chất liệu", data, "material", row);
        });
        renderers.put("JEWELRY", (data, row) -> {
            row = addDetailRowIfPresent("Chất liệu", data, "material", row);
            return addDetailRowIfPresent("Khối lượng", getDouble(data, "weight", 0) + " gr", row);
        });
        renderers.put("ELECTRONICS", (data, row) -> {
            row = addDetailRowIfPresent("Thương hiệu", data, "brand", row);
            return addDetailRowIfPresent("Bảo hành", data, "warrantyPeriod", row);
        });
        return renderers;
    }

    private int addDetailRowIfPresent(String title, JsonObject data, String key, int rowIndex) {
        return addDetailRowIfPresent(title, getString(data, key, ""), rowIndex);
    }

    private int addDetailRowIfPresent(String title, String value, int rowIndex) {
        if (value == null || value.isBlank() || value.startsWith("0")) {
            return rowIndex;
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px; -fx-font-weight: bold;");
        dynamicDetailsGrid.add(titleLabel, 0, rowIndex);
        dynamicDetailsGrid.add(valueLabel, 1, rowIndex);
        return rowIndex + 1;
    }

    private double parseBidAmount() {
        try {
            return Double.parseDouble(bidAmountField.getText().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return currentPrice + minimumBidIncrement;
        }
    }

    private void clearView() {
        stopBidHistoryRefresh();
        if (productNameLabel != null) productNameLabel.setText("Không có dữ liệu");
        if (dynamicDetailsGrid != null) dynamicDetailsGrid.getChildren().clear();
        if (bidHistoryChart != null) bidHistoryChart.getData().clear();
        lastBidHistorySignature = null;
    }

    private void applyEndedMode() {
        if (bidActionBox != null) {
            bidActionBox.setVisible(!endedMode);
            bidActionBox.setManaged(!endedMode);
        }
        if (endedAuctionLabel != null) {
            endedAuctionLabel.setVisible(endedMode);
            endedAuctionLabel.setManaged(endedMode);
        }
        if (confirmBidBtn != null && endedMode) {
            confirmBidBtn.setDisable(true);
        }
    }

    private boolean isAuctionEnded(Auction auction) {
        if (auction == null) {
            return true;
        }

        AuctionStatus status = auction.getStatus();
        return auction.getTimeRemainingSeconds() <= 0
                || status == AuctionStatus.CLOSED
                || status == AuctionStatus.FINISHED
                || status == AuctionStatus.CANCELLED;
    }

    private String formatVnd(double amount) {
        return VND_FORMATTER.format(Math.round(amount)) + " VND";
    }

    private String formatPlainNumber(double amount) {
        return VND_FORMATTER.format(Math.round(amount));
    }

    private static String getString(JsonObject object, String key, String defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : defaultValue;
    }

    private static int getInt(JsonObject object, String key, int defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsInt() : defaultValue;
    }

    private static double getDouble(JsonObject object, String key, double defaultValue) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsDouble() : defaultValue;
    }

    private static JsonObject getObject(JsonObject object, String key, JsonObject defaultValue) {
        return object.has(key) && object.get(key).isJsonObject() ? object.getAsJsonObject(key) : defaultValue;
    }

    private double resolveBidIncrement(String type) {
        try {
            return ItemCategory.valueOf(type).getMinimumBidIncrement();
        } catch (Exception e) {
            return 500000;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Scene scene = bidHistoryChart != null ? bidHistoryChart.getScene() : null;
        if (scene == null || !(scene.getRoot() instanceof StackPane root)) {
            client.util.DialogUtil.showAlert(type, title, null, content, bidHistoryChart);
            return;
        }

        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);
        overlay.setAlignment(Pos.CENTER);
        overlay.setStyle("-fx-background-color: rgba(17, 24, 39, 0.28);");

        String accent = switch (type) {
            case ERROR -> "#dc2626";
            case WARNING -> "#f59e0b";
            case INFORMATION -> "#16a34a";
            default -> "#2563eb";
        };

        VBox dialog = new VBox(8);
        dialog.setAlignment(Pos.CENTER_LEFT);
        dialog.setPadding(new Insets(14, 18, 14, 18));
        dialog.setPrefWidth(300);
        dialog.setMaxWidth(300);
        dialog.setMaxHeight(Region.USE_PREF_SIZE);
        dialog.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 8;" +
                "-fx-border-radius: 8;" +
                "-fx-border-color: #e5e7eb;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.20), 18, 0, 0, 6);"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: " + accent + ";");

        Label contentLabel = new Label(content);
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #111827;");

        Button okButton = new Button("OK");
        okButton.setDefaultButton(true);
        okButton.setStyle(
                "-fx-background-color: " + accent + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: 700;" +
                "-fx-background-radius: 6;" +
                "-fx-font-size: 12px;" +
                "-fx-padding: 5 16;"
        );
        okButton.setOnAction(event -> root.getChildren().remove(overlay));

        dialog.getChildren().addAll(titleLabel, contentLabel, okButton);
        overlay.getChildren().add(dialog);
        root.getChildren().add(overlay);
        okButton.requestFocus();
    }
}

