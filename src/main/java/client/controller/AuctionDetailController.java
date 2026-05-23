package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.controller.auctiondetail.AuctionBidHistoryGraphController;
import client.controller.auctiondetail.AuctionBidHistoryGraphRenderer;
import client.controller.auctiondetail.AuctionImageGalleryRenderer;
import client.controller.auctiondetail.AuctionRealtimeDispatcher;
import client.network.AuctionListener;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.logic.AuctionDetailLogic;
import client.service.AuctionBidClient;
import client.service.AuctionBidClientService;
import client.service.AuctionDetailDataMapper;
import client.util.AuctionDetailGridRenderer;
import client.util.JsonObjects;
import com.google.gson.JsonObject;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.Bid;
import server.model.Item;
import server.model.User;
import util.DateTimeUtil;
import util.LoggerUtil;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuctionDetailController implements AuctionListener {
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
    @FXML private ScrollPane auctionDetailRoot;
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
    private boolean bidPriceAxisPinInstalled;
    private final AuctionBidClient bidClientService;
    private final AuctionDetailDataMapper dataMapper;
    private final AuctionDetailGridRenderer detailGridRenderer;
    private final AuctionRealtimeDispatcher realtimeDispatcher = new AuctionRealtimeDispatcher(this);
    private final AuctionDetailLogic auctionDetailLogic;
    private AuctionImageGalleryRenderer imageGalleryRenderer;
    private AuctionBidHistoryGraphRenderer bidHistoryGraphRenderer;
    private AuctionBidHistoryGraphController bidHistoryGraphController;

    public AuctionDetailController() {
        this(new AuctionBidClientService(), new AuctionDetailDataMapper(), new AuctionDetailGridRenderer(), new AuctionDetailLogic());
    }

    AuctionDetailController(AuctionBidClient bidClientService,
                            AuctionDetailDataMapper dataMapper,
                            AuctionDetailGridRenderer detailGridRenderer,
                            AuctionDetailLogic auctionDetailLogic) {
        this.bidClientService = bidClientService;
        this.dataMapper = dataMapper;
        this.detailGridRenderer = detailGridRenderer;
        this.auctionDetailLogic = auctionDetailLogic;
    }

    public void loadAuctionData(Auction auction) {
        stopBidHistoryRefresh();
        bidHistoryGraphController().reset();
        if (auction == null) {
            clearView();
            return;
        }

        Item item = auction.getItem();
        imageGalleryRenderer().loadMainImage(item);

        endedMode = endedMode || dataMapper.isAuctionEnded(auction);
        loadAuctionData(dataMapper.toDetailData(auction));
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

        JsonObject productData = JsonObjects.getObject(itemData, "item", itemData);
        if (productNameLabel != null) {
        productNameLabel.setText(JsonObjects.getString(productData, "name", "Chưa có tên sản phẩm"));
        }
        if (descriptionLabel != null) {
        descriptionLabel.setText(JsonObjects.getString(productData, "description", "Chưa có mô tả"));
        }

        String incomingAuctionId = JsonObjects.getString(itemData, "auctionId", "");
        boolean sameAuction = incomingAuctionId != null && incomingAuctionId.equals(currentAuctionId);
        currentAuctionId = incomingAuctionId;
        currentItemId = JsonObjects.getString(productData, "itemId", "");
        startingPrice = JsonObjects.getDouble(productData, "startingPrice", 0);
        double incomingCurrentPrice = JsonObjects.getDouble(itemData, "currentPrice", startingPrice);
        double cachedHighestPrice = getCachedHighestPrice(currentAuctionId);
        currentPrice = auctionDetailLogic.resolveCurrentPrice(sameAuction, currentPrice, incomingCurrentPrice, cachedHighestPrice);
        minimumBidIncrement = JsonObjects.getDouble(itemData, "minimumBidIncrement", 0);
        minimumBidIncrement = auctionDetailLogic.resolveMinimumBidIncrement(
                minimumBidIncrement,
                dataMapper.resolveBidIncrement(JsonObjects.getString(productData, "type", "")));

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
            bidAmountField.setText(formatPlainNumber(auctionDetailLogic.nextDecreaseBid(
                    bidAmountField.getText(), currentPrice, minimumBidIncrement)));
        } catch (Exception e) {
            bidAmountField.setText(formatPlainNumber(currentPrice + minimumBidIncrement));
        }
    }

    @FXML private void handleIncreaseBid() {
        try {
            bidAmountField.setText(formatPlainNumber(auctionDetailLogic.nextIncreaseBid(
                    bidAmountField.getText(), currentPrice, minimumBidIncrement)));
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
        double minimumAllowed = auctionDetailLogic.minimumAllowedBid(currentPrice, minimumBidIncrement);
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

        if (!ProfileController.hasCompleteBidProfile(currentUser)) {
            showAlert(Alert.AlertType.WARNING, "Cập nhật hồ sơ", "Vui lòng cập nhật số điện thoại và địa chỉ trong hồ sơ trước khi đấu giá.");
            return;
        }

        confirmBidBtn.setDisable(true);
        confirmBidBtn.setText("ĐANG XỬ LÝ...");
        client.util.ClientTaskRunner.run(() -> {
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
        });
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

        List<Bid> cachedBids = bidHistoryGraphController().getCachedBids(currentAuctionId);
        if (cachedBids != null && !cachedBids.isEmpty()) {
            updateBidHistoryGraph(cachedBids, shouldAutoScrollInitialBidHistory());
        }
        refreshBidHistoryFromServer(currentAuctionId, true);
    }

    private void configureBidHistoryGraph() {
        configureRootScrollLock();
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

    private void configureRootScrollLock() {
        if (auctionDetailRoot == null) {
            return;
        }
        auctionDetailRoot.setFitToWidth(true);
        auctionDetailRoot.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        auctionDetailRoot.setPannable(false);
        auctionDetailRoot.setHvalue(0);
        auctionDetailRoot.hvalueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.doubleValue() != 0) {
                auctionDetailRoot.setHvalue(0);
            }
        });
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

        client.util.ClientTaskRunner.run(() -> {
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
        });
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

            realtimeDispatcher.dispatch(asyncMessage);
        }
    }

    @Override
    public void onAuctionUpdated(Auction auction) {
        if (isCurrentAuction(auction)) {
            applyLatestBidPrice(auction.getCurrentPrice());
        }
    }

    @Override
    public void onBidPlaced(Bid bid) {
        if (bid != null && currentAuctionId != null && currentAuctionId.equals(bid.getAuctionId())) {
            mergeBidIntoHistory(bid, false);
        }
    }

    @Override
    public void onPriceUpdated(Auction auction) {
        onAuctionUpdated(auction);
    }

    @Override
    public void onAuctionFinished(Auction auction) {
        if (isCurrentAuction(auction)) {
            applyLatestBidPrice(auction.getCurrentPrice());
            endedMode = true;
            applyEndedMode();
        }
    }

    @Override
    public void onError(String errorMessage) {
        LoggerUtil.warn("Auction realtime error: " + safeRealtimeText(errorMessage));
    }

    @Override
    public void onNotification(String message) {
        LoggerUtil.debug("Auction realtime notification: " + safeRealtimeText(message));
    }

    @Override
    public void onOutbid(Auction auction) {
        onAuctionUpdated(auction);
    }

    @Override
    public void onAuctionExtended(Auction auction) {
        if (isCurrentAuction(auction)) {
            startCountdown(auction.getEndTime());
            applyLatestBidPrice(auction.getCurrentPrice());
        }
    }

    private boolean isCurrentAuction(Auction auction) {
        return auctionDetailLogic.isCurrentAuction(auction, currentAuctionId);
    }

    private String safeRealtimeText(String value) {
        return auctionDetailLogic.safeRealtimeText(value);
    }

    private void updateBidHistoryGraph(List<Bid> bids) {
        bidHistoryGraphController().updateGraph(bids);
    }

    private void updateBidHistoryGraph(List<Bid> bids, boolean scrollToLatest) {
        bidHistoryGraphController().updateGraph(bids, scrollToLatest);
    }

    private boolean shouldKeepExistingBidGraph() {
        return bidHistoryGraphController().shouldKeepExistingGraph();
    }

    private void showFallbackCurrentPriceGraph() {
        bidHistoryGraphController().showFallbackCurrentPriceGraph();
    }

    private void keepExistingOrShowFallback() {
        bidHistoryGraphController().keepExistingOrShowFallback();
    }

    private boolean shouldAutoScrollInitialBidHistory() {
        return bidHistoryGraphController().shouldAutoScrollInitialHistory();
    }

    private void mergeBidIntoHistory(Bid bid) {
        bidHistoryGraphController().mergeBidIntoHistory(bid);
    }

    private void mergeBidIntoHistory(Bid bid, boolean scrollToLatest) {
        bidHistoryGraphController().mergeBidIntoHistory(bid, scrollToLatest);
    }

    private List<Bid> mergeWithCachedHistory(String auctionId, List<Bid> serverBids) {
        return bidHistoryGraphController().mergeWithCachedHistory(auctionId, serverBids);
    }

    private void applyLatestBidPrice(double amount) {
        if (!auctionDetailLogic.shouldApplyLatestBidPrice(amount, currentPrice)) {
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

        currentPrice = auctionDetailLogic.latestPriceFromHistory(currentPrice, sortedBids);
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

        return bidHistoryGraphController().getHighestPrice(auctionId);
    }

    private AuctionImageGalleryRenderer imageGalleryRenderer() {
        if (imageGalleryRenderer == null) {
            imageGalleryRenderer = new AuctionImageGalleryRenderer(mainImageView, thumbnailBox);
        }
        return imageGalleryRenderer;
    }

    private AuctionBidHistoryGraphRenderer bidHistoryGraphRenderer() {
        if (bidHistoryGraphRenderer == null) {
            bidHistoryGraphRenderer = new AuctionBidHistoryGraphRenderer(
                    bidHistoryChart,
                    bidTurnAxis,
                    bidPriceAxis,
                    this::formatVnd,
                    this::updatePinnedBidPriceAxis
            );
        }
        return bidHistoryGraphRenderer;
    }

    private AuctionBidHistoryGraphController bidHistoryGraphController() {
        if (bidHistoryGraphController == null) {
            bidHistoryGraphController = new AuctionBidHistoryGraphController(
                    bidHistoryChart,
                    bidHistoryScrollPane,
                    () -> currentAuctionId,
                    () -> startingPrice > 0 ? startingPrice : currentPrice,
                    () -> minimumBidIncrement,
                    bidHistoryGraphRenderer(),
                    this::syncPriceFromBidHistory
            );
        }
        return bidHistoryGraphController;
    }

    private void renderProductDetails(JsonObject itemData, JsonObject productData) {
        detailGridRenderer.render(dynamicDetailsGrid, itemData, productData, this::formatVnd);
    }

    private double parseBidAmount() {
        return auctionDetailLogic.parseBidAmount(
                bidAmountField == null ? null : bidAmountField.getText(),
                currentPrice,
                minimumBidIncrement);
    }

    private void clearView() {
        stopBidHistoryRefresh();
        if (productNameLabel != null) productNameLabel.setText("Không có dữ liệu");
        if (dynamicDetailsGrid != null) dynamicDetailsGrid.getChildren().clear();
        if (bidHistoryChart != null) bidHistoryChart.getData().clear();
        bidHistoryGraphController().clearSignature();
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

    private String formatVnd(double amount) {
        return auctionDetailLogic.formatVnd(amount);
    }

    private String formatPlainNumber(double amount) {
        return auctionDetailLogic.formatPlainNumber(amount);
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

