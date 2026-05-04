package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.Message;
import common.MessageType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import navigation.NavigationManager;
import server.model.Auction;
import server.model.User;
import util.LoggerUtil;
import util.JsonUtil; // Thêm import JsonUtil

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SellerAuctionDetailsController {

    @FXML private Label productName, productId, productDesc;
    @FXML private Label startPrice, currentPrice;
    @FXML private Label startTime, endTime, timeLeft;
    @FXML private Label status, bidCount;
    @FXML private Label topBidderName, topBidAmount;
    @FXML private ImageView productImage;

    private String auctionId;

    /**
     * Tải chi tiết phiên đấu giá từ Server
     */
    public void loadSellerAuctionDetails(String auctionId) {
        this.auctionId = auctionId;
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();
        User user = NavigationManager.getInstance().getCurrentUser();

        if (socket == null || user == null) {
            LoggerUtil.error("Socket hoặc User bị null trong loadSellerAuctionDetails");
            return;
        }

        new Thread(() -> {
            try {
                // Gửi yêu cầu lấy chi tiết
                Message req = new Message(MessageType.GET_AUCTION_DETAIL, auctionId, user.getUsername());

                // Sử dụng hàm gửi/nhận đồng bộ để tránh bị lẫn dữ liệu với các luồng khác
                socket.sendMessage(req);
                Message res = socket.receiveMessage();

                if (res != null && "SUCCESS".equals(res.getStatus()) && res.getData() != null) {

                    // --- ĐIỂM CHỐT LÀ Ở ĐÂY ---
                    // KHÔNG dùng new Gson() nữa. Phải dùng Gson đã đăng ký TypeAdapter!
                    Gson gson = JsonUtil.getGson();

                    String json = gson.toJson(res.getData());
                    Auction auction = null;

                    // FIX LỖI "Expected BEGIN_OBJECT but was BEGIN_ARRAY"
                    // Kiểm tra xem dữ liệu trả về là Mảng (List) hay Đối tượng (Object)
                    if (json.trim().startsWith("[")) {
                        // Nếu là mảng (Array/List)
                        Type listType = new TypeToken<ArrayList<Auction>>(){}.getType();
                        List<Auction> auctionList = gson.fromJson(json, listType);
                        if (auctionList != null && !auctionList.isEmpty()) {
                            auction = auctionList.get(0); // Lấy phần tử đầu tiên
                        }
                    } else {
                        // Nếu là đối tượng đơn lẻ
                        auction = gson.fromJson(json, Auction.class);
                    }

                    final Auction finalAuction = auction;
                    Platform.runLater(() -> {
                        try {
                            if (finalAuction != null) {
                                updateUI(finalAuction);
                            } else {
                                LoggerUtil.warn("Không parse được dữ liệu Auction từ Server.");
                            }
                        } catch (Exception e) {
                            LoggerUtil.error("Lỗi khi cập nhật giao diện: " + e.getMessage());
                        }
                    });
                } else {
                    String errorReason = (res != null && res.getData() != null) ? res.getData().toString() : "Không có phản hồi từ Server";
                    LoggerUtil.warn("Không tìm thấy chi tiết phiên: " + errorReason);
                    Platform.runLater(() -> {
                        if (productName != null) productName.setText("Lỗi: " + errorReason);
                    });
                }
            } catch (Exception e) {
                LoggerUtil.error("Lỗi kết nối khi lấy chi tiết phiên: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Cập nhật thông tin lên giao diện
     */
    private void updateUI(Auction auction) {
        if (auction == null) return;

        // Thông tin sản phẩm cơ bản (Kiểm tra null cho từng Label để tránh NullPointerException)
        if (productName != null) productName.setText(auction.getItem() != null ? auction.getItem().getName() : "N/A");
        if (productId != null) productId.setText("Mã phiên: " + auction.getAuctionId());
        if (productDesc != null) productDesc.setText(auction.getItem() != null ? auction.getItem().getDescription() : "");

        // Giá cả
        if (startPrice != null) startPrice.setText(auction.getItem() != null ? formatVnd(auction.getItem().getStartingPrice()) : "0 đ");
        if (currentPrice != null) currentPrice.setText(formatVnd(auction.getCurrentPrice()));

        // Thời gian
        if (startTime != null) startTime.setText(formatDate(auction.getStartTime()));
        if (endTime != null) endTime.setText(formatDate(auction.getEndTime()));

        if (timeLeft != null) {
            long remain = auction.getTimeRemainingSeconds();
            if (remain > 0) {
                timeLeft.setText(formatRemain(remain));
                timeLeft.setStyle("-fx-text-fill: #185fa5;"); // Màu xanh nếu còn thời gian
            } else {
                timeLeft.setText("Đã kết thúc");
                timeLeft.setStyle("-fx-text-fill: #dc2626;"); // Màu đỏ nếu đã hết
            }
        }

        // Trạng thái
        if (status != null) {
            String statusStr = auction.getStatus() != null ? auction.getStatus().toString() : "UNKNOWN";
            status.setText(statusStr);
            // Thêm màu sắc cho trạng thái sinh động hơn
            if (statusStr.equals("ACTIVE")) status.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 4 10; -fx-background-radius: 15;");
            else status.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #374151; -fx-padding: 4 10; -fx-background-radius: 15;");
        }

        // Lượt đặt và người dẫn đầu
        if (bidCount != null) bidCount.setText((auction.getBidIds() != null ? auction.getBidIds().size() : 0) + " lượt");
        if (topBidderName != null) topBidderName.setText(auction.getHighestBidderName() != null ? auction.getHighestBidderName() : "Chưa có");
        if (topBidAmount != null) topBidAmount.setText(formatVnd(auction.getCurrentPrice()));
    }

    @FXML private void handleEdit() { LoggerUtil.info("Chỉnh sửa: " + auctionId); }
    @FXML private void handlePause() { LoggerUtil.info("Tạm dừng: " + auctionId); }
    @FXML private void handleCancel() { LoggerUtil.info("Hủy phiên: " + auctionId); }

    private String formatVnd(double a) { return String.format("%,.0f đ", a).replace(',', '.'); }
    private String formatDate(long m) { return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(m)); }
    private String formatRemain(long s) {
        long h = s / 3600;
        long m = (s % 3600) / 60;
        if (h > 0) return h + " giờ " + m + " phút";
        return m + " phút";
    }
}