package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.SellerClientService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.User;
import util.LoggerUtil;

import java.net.URL;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class SellerStatisticsController implements Initializable {

    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML private Label totalRevenueLabel;
    @FXML private Label avgBidLabel;
    @FXML private Label successRateLabel;
    @FXML private LineChart<String, Number> revenueChart;

    private User currentUser;
    private ClientSocket clientSocket;
    private boolean statisticsLoaded;
    private final SellerClientService sellerClientService = new SellerClientService();
    private final SimpleDateFormat revenuePointFormat = new SimpleDateFormat("dd/MM HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupChart();
        showEmptyStatistics();
        Platform.runLater(this::applyReadableStyles);

        User navigationUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket navigationSocket = getActiveSocket();
        if (navigationUser != null && navigationSocket != null) {
            setUserData(navigationUser, navigationSocket);
        }
    }

    public void setUserData(User user, ClientSocket socket) {
        currentUser = user;
        clientSocket = socket;

        if (user != null) {
            NavigationManager.getInstance().setCurrentUser(user);
        }
        if (socket != null) {
            NavigationManager.getInstance().setClientSocket(socket);
        }

        if (statisticsLoaded) {
            return;
        }
        statisticsLoaded = true;
        loadStatisticsFromServer();
    }

    private void setupChart() {
        if (revenueChart == null) {
            return;
        }

        revenueChart.setAnimated(false);
        revenueChart.setLegendVisible(false);
        revenueChart.setCreateSymbols(true);
        revenueChart.setHorizontalGridLinesVisible(true);
        revenueChart.setVerticalGridLinesVisible(false);
        revenueChart.setAlternativeColumnFillVisible(false);
        revenueChart.setAlternativeRowFillVisible(false);
        revenueChart.setMinHeight(350);
        revenueChart.setPrefHeight(350);
        revenueChart.getData().clear();
        configureChartAxesVisible();
        showEmptyRevenueChart();
    }

    private void loadStatisticsFromServer() {
        User user = currentUser != null ? currentUser : NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = getActiveSocket();

        if (user == null || socket == null || !socket.isConnected()) {
            LoggerUtil.warn("Seller statistics: missing user or socket");
            Platform.runLater(this::showEmptyStatistics);
            statisticsLoaded = false;
            return;
        }

        new Thread(() -> {
            try {
                List<Auction> auctions = sellerClientService.fetchSellerAuctions(socket, user);
                Platform.runLater(() -> processAndDisplayStatistics(auctions));
            } catch (Exception e) {
                LoggerUtil.error("Seller statistics load failed: " + e.getMessage());
                Platform.runLater(this::showEmptyStatistics);
            }
        }, "SellerStatisticsLoadThread").start();
    }

    private ClientSocket getActiveSocket() {
        if (clientSocket != null && clientSocket.isConnected()) {
            return clientSocket;
        }

        ClientSocket navigationSocket = NavigationManager.getInstance().getClientSocket();
        if (navigationSocket != null && navigationSocket.isConnected()) {
            clientSocket = navigationSocket;
            return navigationSocket;
        }

        ClientSocket managerSocket = ConnectionManager.getInstance().getClientSocket();
        if (managerSocket != null && managerSocket.isConnected()) {
            clientSocket = managerSocket;
            return managerSocket;
        }

        return null;
    }

    private void processAndDisplayStatistics(List<Auction> auctions) {
        List<Auction> safeAuctions = auctions != null ? auctions : List.of();

        double totalRevenue = 0;
        int totalBids = 0;
        int endedAuctions = 0;
        int successfulAuctions = 0;
        Map<Long, Double> revenueByEndTime = new TreeMap<>();

        for (Auction auction : safeAuctions) {
            if (auction == null) {
                continue;
            }

            int bidCount = auction.getBidIds() != null ? auction.getBidIds().size() : 0;
            totalBids += bidCount;

            if (isEnded(auction)) {
                endedAuctions++;
                if (bidCount > 0) {
                    successfulAuctions++;
                    totalRevenue += auction.getCurrentPrice();

                    long endTime = auction.getEndTime() > 0 ? auction.getEndTime() : auction.getCreatedAt();
                    revenueByEndTime.merge(endTime, auction.getCurrentPrice(), Double::sum);
                }
            }
        }

        setLabel(totalRevenueLabel, formatVnd(totalRevenue));

        double averageBid = safeAuctions.isEmpty() ? 0 : (double) totalBids / safeAuctions.size();
        setLabel(avgBidLabel, String.format(Locale.US, "%.1f", averageBid));

        double successRate = endedAuctions == 0 ? 0 : successfulAuctions * 100.0 / endedAuctions;
        setLabel(successRateLabel, String.format(Locale.US, "%.1f%%", successRate));

        updateRevenueChart(revenueByEndTime);
        applyReadableStyles();
        LoggerUtil.info("Seller statistics loaded: " + safeAuctions.size() + " auctions");
    }

    private void updateRevenueChart(Map<Long, Double> revenueByEndTime) {
        if (revenueChart == null) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (revenueByEndTime == null || revenueByEndTime.isEmpty()) {
            showEmptyRevenueChart();
            return;
        }

        List<String> categories = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : revenueByEndTime.entrySet()) {
            String label = revenuePointFormat.format(new Date(entry.getKey()));
            categories.add(label);
            series.getData().add(new XYChart.Data<>(label, entry.getValue()));
        }

        configureDataAxes(revenueByEndTime, categories);
        revenueChart.getData().setAll(series);
        Platform.runLater(() -> styleChartNodes(false));
    }

    private boolean isEnded(Auction auction) {
        if (auction.getStatus() == common.AuctionStatus.FINISHED
                || auction.getStatus() == common.AuctionStatus.CLOSED) {
            return true;
        }
        return auction.getEndTime() > 0 && auction.getEndTime() <= System.currentTimeMillis();
    }

    private void showEmptyRevenueChart() {
        configureEmptyAxes();

        XYChart.Series<String, Number> emptySeries = new XYChart.Series<>();
        emptySeries.getData().add(new XYChart.Data<>(" ", 0));
        emptySeries.getData().add(new XYChart.Data<>("  ", 0));

        revenueChart.getData().setAll(emptySeries);
        Platform.runLater(() -> styleChartNodes(true));
    }

    private void configureEmptyAxes() {
        Axis<String> xAxis = revenueChart == null ? null : revenueChart.getXAxis();
        if (xAxis instanceof CategoryAxis categoryAxis) {
            categoryAxis.setAutoRanging(false);
            categoryAxis.setCategories(FXCollections.observableArrayList(" ", "  "));
            categoryAxis.setTickLabelsVisible(true);
            categoryAxis.setTickMarkVisible(true);
        }

        Axis<Number> yAxis = revenueChart == null ? null : revenueChart.getYAxis();
        if (yAxis instanceof NumberAxis numberAxis) {
            numberAxis.setAutoRanging(false);
            numberAxis.setLowerBound(0);
            numberAxis.setUpperBound(110);
            numberAxis.setTickUnit(10);
            numberAxis.setMinorTickCount(4);
        }
        configureChartAxesVisible();
    }

    private void configureDataAxes(Map<Long, Double> revenueByEndTime, List<String> categories) {
        Axis<String> xAxis = revenueChart == null ? null : revenueChart.getXAxis();
        if (xAxis instanceof CategoryAxis categoryAxis) {
            categoryAxis.setAutoRanging(false);
            categoryAxis.setCategories(FXCollections.observableArrayList(categories));
            categoryAxis.setTickLabelsVisible(true);
            categoryAxis.setTickMarkVisible(true);
        }

        Axis<Number> axis = revenueChart == null ? null : revenueChart.getYAxis();
        if (!(axis instanceof NumberAxis numberAxis)) {
            return;
        }

        double max = revenueByEndTime.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(100);
        double tickUnit = calculateTickUnit(max);
        double upperBound = Math.max(tickUnit, Math.ceil(max / tickUnit) * tickUnit + tickUnit);
        numberAxis.setAutoRanging(false);
        numberAxis.setLowerBound(0);
        numberAxis.setUpperBound(upperBound);
        numberAxis.setTickUnit(tickUnit);
        numberAxis.setMinorTickCount(4);
        configureChartAxesVisible();
    }

    private void configureChartAxesVisible() {
        if (revenueChart == null) {
            return;
        }

        Axis<String> xAxis = revenueChart.getXAxis();
        Axis<Number> yAxis = revenueChart.getYAxis();

        xAxis.setVisible(true);
        xAxis.setOpacity(1);
        xAxis.setTickLabelsVisible(true);
        xAxis.setTickMarkVisible(true);
        xAxis.setTickLabelFill(Color.web("#4a5568"));
        xAxis.setStyle("-fx-border-color: #718096 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        yAxis.setVisible(true);
        yAxis.setOpacity(1);
        yAxis.setTickLabelsVisible(true);
        yAxis.setTickMarkVisible(true);
        yAxis.setTickLabelFill(Color.web("#4a5568"));
        yAxis.setStyle("-fx-border-color: transparent #718096 transparent transparent; -fx-border-width: 0 1 0 0;");
    }

    private double calculateTickUnit(double max) {
        if (max <= 0) {
            return 10;
        }
        double roughTick = max / 6.0;
        double magnitude = Math.pow(10, Math.floor(Math.log10(roughTick)));
        double normalized = roughTick / magnitude;

        if (normalized <= 1) {
            return magnitude;
        }
        if (normalized <= 2) {
            return 2 * magnitude;
        }
        if (normalized <= 5) {
            return 5 * magnitude;
        }
        return 10 * magnitude;
    }

    private void styleChartNodes(boolean hideSeries) {
        if (revenueChart == null) {
            return;
        }

        Node plotBackground = revenueChart.lookup(".chart-plot-background");
        if (plotBackground != null) {
            plotBackground.setStyle("-fx-background-color: white;");
        }

        Node horizontalGrid = revenueChart.lookup(".chart-horizontal-grid-lines");
        if (horizontalGrid != null) {
            horizontalGrid.setStyle("-fx-stroke: #e2e8f0; -fx-stroke-dash-array: 8 6;");
        }

        Node verticalGrid = revenueChart.lookup(".chart-vertical-grid-lines");
        if (verticalGrid != null) {
            verticalGrid.setStyle("-fx-stroke: transparent;");
        }

        for (Node axis : revenueChart.lookupAll(".axis")) {
            axis.setVisible(true);
            axis.setOpacity(1);
        }
        for (Node tickLabel : revenueChart.lookupAll(".axis .text")) {
            tickLabel.setVisible(true);
            tickLabel.setOpacity(1);
            tickLabel.setStyle("-fx-fill: #4a5568;");
        }
        for (Node tickMark : revenueChart.lookupAll(".axis-tick-mark")) {
            tickMark.setVisible(true);
            tickMark.setOpacity(1);
            tickMark.setStyle("-fx-stroke: #718096;");
        }
        for (Node axisLine : revenueChart.lookupAll(".axis .axis-line")) {
            axisLine.setVisible(true);
            axisLine.setOpacity(1);
            axisLine.setStyle("-fx-stroke: #718096;");
        }

        if (hideSeries) {
            for (Node line : revenueChart.lookupAll(".chart-series-line")) {
                line.setStyle("-fx-stroke: transparent;");
            }
            for (Node symbol : revenueChart.lookupAll(".chart-line-symbol")) {
                symbol.setVisible(false);
                symbol.setManaged(false);
            }
        }
    }

    private void showEmptyStatistics() {
        setLabel(totalRevenueLabel, formatVnd(0));
        setLabel(avgBidLabel, "0.0");
        setLabel(successRateLabel, "0.0%");
        updateRevenueChart(Map.of());
    }

    private void setLabel(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private String formatVnd(double amount) {
        if (Math.abs(amount) >= 1_000_000) {
            return String.format(Locale.US, "%.1fM \u0111", amount / 1_000_000.0);
        }
        return VND_FORMATTER.format(Math.round(amount)) + " \u0111";
    }

    private void applyReadableStyles() {
        Parent statisticsRoot = findStatisticsRoot();
        if (statisticsRoot == null) {
            return;
        }

        applyReadableStyles(statisticsRoot);
        setLabelColor(totalRevenueLabel, "#111827");
        setLabelColor(avgBidLabel, "#111827");
        setLabelColor(successRateLabel, "#111827");
    }

    private Parent findStatisticsRoot() {
        Node node = revenueChart != null ? revenueChart : totalRevenueLabel;
        while (node != null) {
            if (node instanceof ScrollPane scrollPane) {
                Node content = scrollPane.getContent();
                return content instanceof Parent parent ? parent : scrollPane;
            }
            node = node.getParent();
        }
        return null;
    }

    private void applyReadableStyles(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Label label) {
                styleLabelByContext(label);
            }
            if (child instanceof Parent childParent) {
                applyReadableStyles(childParent);
            }
        }
    }

    private void styleLabelByContext(Label label) {
        String parentStyle = label.getParent() == null || label.getParent().getStyle() == null
                ? ""
                : label.getParent().getStyle();

        double fontSize = label.getFont() != null ? label.getFont().getSize() : 14;
        if (fontSize >= 18) {
            setLabelColor(label, "#111827");
        } else if (label == totalRevenueLabel || label == avgBidLabel || label == successRateLabel) {
            setLabelColor(label, "#111827");
        } else {
            setLabelColor(label, "#4b5563");
        }
    }

    private void setLabelColor(Label label, String color) {
        if (label == null) {
            return;
        }

        String style = label.getStyle() == null ? "" : label.getStyle();
        style = style.replaceAll("-fx-text-fill\\s*:\\s*[^;]+;?", "").trim();
        if (!style.isEmpty() && !style.endsWith(";")) {
            style += ";";
        }
        label.setStyle(style + " -fx-text-fill: " + color + ";");
    }
}
