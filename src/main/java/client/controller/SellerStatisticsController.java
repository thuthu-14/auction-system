package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.User;
import util.LoggerUtil;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class SellerStatisticsController implements Initializable {

    // Khai báo các thành phần UI từ file FXML
    @FXML private Label totalRevenueLabel;
    @FXML private Label avgBidLabel;
    @FXML private Label successRateLabel;
    @FXML private LineChart<String, Number> revenueChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Khởi tạo biểu đồ trống
        revenueChart.getData().clear();
        revenueChart.setAnimated(false); // Tắt animation mặc định để tránh giật khi load data

        // Gọi dữ liệu thật từ Server
        loadStatisticsFromServer();
    }

    private void loadStatisticsFromServer() {
        User currentUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();

        if (currentUser == null || socket == null) {
            LoggerUtil.error("Chưa đăng nhập hoặc mất kết nối Server!");
            return;
        }

        // Tạo luồng riêng để giao tiếp mạng, tránh đơ UI
        new Thread(() -> {
            try {
                // Tận dụng lệnh GET_SELLER_AUCTIONS đã có sẵn trên Server
                Message request = new Message(MessageType.GET_SELLER_AUCTIONS, null, currentUser.getUsername());
                Message response = socket.sendAndReceive(request);

                if (response != null && "SUCCESS".equals(response.getStatus())) {
                    @SuppressWarnings("unchecked")
                    List<Auction> auctions = (List<Auction>) response.getData();

                    // Xử lý tính toán số liệu và cập nhật giao diện
                    Platform.runLater(() -> processAndDisplayStatistics(auctions));
                } else {
                    LoggerUtil.error("Lỗi lấy dữ liệu thống kê: " + (response != null ? response.getMessage() : ""));
                }
            } catch (Exception e) {
                LoggerUtil.error("Lỗi giao tiếp mạng khi lấy thống kê: " + e.getMessage());
            }
        }).start();
    }

    private void processAndDisplayStatistics(List<Auction> auctions) {
        double totalRevenue = 0;
        int totalBids = 0;
        int endedAuctions = 0;
        int successfulAuctions = 0; // Số phiên đấu giá kết thúc có người mua

        // Dùng TreeMap để tự động sắp xếp các tháng tăng dần
        Map<String, Double> revenueByMonth = new TreeMap<>();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy");

        for (Auction auc : auctions) {
            int bidsCount = (auc.getBidIds() != null) ? auc.getBidIds().size() : 0;
            totalBids += bidsCount;

            // Kiểm tra xem phiên đấu giá đã kết thúc chưa
            if (auc.getTimeRemainingSeconds() <= 0) {
                endedAuctions++;

                // Nếu có người đặt giá -> Đấu giá thành công
                if (bidsCount > 0) {
                    successfulAuctions++;
                    totalRevenue += auc.getCurrentPrice();

                    // Cộng dồn doanh thu theo tháng
                    String monthKey = monthFormat.format(new Date(auc.getEndTime()));
                    revenueByMonth.put(monthKey, revenueByMonth.getOrDefault(monthKey, 0.0) + auc.getCurrentPrice());
                }
            }
        }

        // 1. Cập nhật các Label số liệu tổng quan
        totalRevenueLabel.setText(String.format("%,.0f đ", totalRevenue).replace(",", "."));

        double avgBid = auctions.isEmpty() ? 0 : (double) totalBids / auctions.size();
        avgBidLabel.setText(String.format("%.1f", avgBid));

        double successRate = endedAuctions == 0 ? 0 : ((double) successfulAuctions / endedAuctions) * 100;
        successRateLabel.setText(String.format("%.1f %%", successRate));

        // 2. Cập nhật Biểu đồ doanh thu
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu thực tế");

        if (revenueByMonth.isEmpty()) {
            // Nếu chưa có doanh thu, hiển thị 1 mốc 0 đồng cho khỏi trống
            series.getData().add(new XYChart.Data<>("Chưa có", 0));
        } else {
            // Nạp dữ liệu từng tháng vào biểu đồ
            for (Map.Entry<String, Double> entry : revenueByMonth.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
        }

        revenueChart.getData().clear();
        revenueChart.getData().add(series);

        LoggerUtil.info("Đã tải xong dữ liệu Thống kê cho Seller.");
    }
}
