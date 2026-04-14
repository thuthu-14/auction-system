// src/main/java/client/controller/HomeScreenController.java
package client.controller;

import client.network.ClientSocket;
import client.util.AlertUtil;
import common.*;
import server.model.Auction;
import server.model.User;
import server.model.RegularUser;
import util.LoggerUtil;
import util.DateTimeUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.io.IOException;
import java.util.*;

/**
 * HomeScreenController - Màn hình chính
 * MVC Pattern: Controller
 *
 * Flow:
 * 1. Mặc định hiển thị tab BIDDER (xem & bid)
 * 2. Có nút "Nâng cấp Seller" - sau khi nâng cấp sẽ hiển thị tab SELLER
 * 3. User vừa có thể bid vừa có thể sell (nếu là seller)
 */




import javafx.event.ActionEvent;
import javafx.scene.input.MouseEvent;


public class HomeScreenController {

    @FXML private Label titleLabel;
    @FXML private Label walletLabel;
    @FXML private HBox roleButtonsBox;
    @FXML private Button bidderButton;
    @FXML private Button sellerButton;
    @FXML private Button upgradeSellerButton;
    @FXML private TabPane tabPane;
    @FXML private Tab bidderTab;
    @FXML private Tab sellerTab;
    @FXML private Button logoutButton;
    @FXML private Button addFundsButton;

    private ClientSocket clientSocket;
    private User currentUser;
    private BidderPanelController bidderController;
    private SellerPanelController sellerController;
    private Runnable onLogout;
    private Timer refreshTimer;

    @FXML
    private void handleMenuClick() {
        // TODO: map menu -> content/tab
    }

    @FXML
    private void handleProductSearch() {
        // TODO: search logic
    }

    @FXML
    private void clearSearch() {
        // TODO: clear search logic
    }






    @FXML
    public void initialize() {
        LoggerUtil.info("✓ HomeScreenController initialized");
    }

    /**
     * Set user data after login
     */
    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;

        updateUI();
        startAutoRefresh();

        LoggerUtil.info("✓ HomeScreen set for user: " + user.getUsername());
    }

    /**
     * Cập nhật UI dựa trên user role
     */
    private void updateUI() {
        titleLabel.setText(currentUser.getDashboardTitle());
        walletLabel.setText("💳 Tài khoản: $" + String.format("%.2f", currentUser.getWallet()));

        // Kiểm tra xem có phải RegularUser không
        if (!(currentUser instanceof RegularUser)) {
            sellerButton.setVisible(false);
            upgradeSellerButton.setVisible(false);
            return;
        }

        RegularUser regularUser = (RegularUser) currentUser;

        if (regularUser.isSeller()) {
            // Đã là seller - hiển thị cả 2 tab
            bidderButton.setVisible(true);
            bidderButton.setStyle("-fx-text-fill: #2196F3;");
            bidderButton.setOnAction(e -> tabPane.getSelectionModel().select(bidderTab));

            sellerButton.setVisible(true);
            sellerButton.setStyle("-fx-text-fill: #FF9800;");
            sellerButton.setOnAction(e -> tabPane.getSelectionModel().select(sellerTab));

            upgradeSellerButton.setText("✓ Bạn đã là Seller");
            upgradeSellerButton.setDisable(true);
        } else {
            // Chưa là seller - ẩn tab seller
            bidderButton.setVisible(true);
            bidderButton.setStyle("-fx-text-fill: #2196F3;");
            bidderButton.setDisable(true);

            sellerButton.setVisible(false);

            upgradeSellerButton.setText("⬆️ Nâng cấp Seller");
            upgradeSellerButton.setDisable(false);
            upgradeSellerButton.setStyle("-fx-padding: 10; -fx-font-size: 14;");
            upgradeSellerButton.setOnAction(e -> handleUpgradeSeller());
        }

        logoutButton.setOnAction(e -> handleLogout());
        addFundsButton.setOnAction(e -> handleAddFunds());
    }

    /**
     * Set tab controllers
     */
    public void setTabControllers(BidderPanelController bidderCtrl, SellerPanelController sellerCtrl) {
        this.bidderController = bidderCtrl;
        this.sellerController = sellerCtrl;

        bidderCtrl.setUserData(currentUser, clientSocket, this);

        // Chỉ set seller controller nếu user là seller
        if (currentUser instanceof RegularUser) {
            RegularUser regularUser = (RegularUser) currentUser;
            if (regularUser.isSeller()) {
                sellerCtrl.setUserData(currentUser, clientSocket, this);
            }
        }
    }

    /**
     * Set logout callback
     */
    public void setOnLogout(Runnable callback) {
        this.onLogout = callback;
    }

    /**
     * ← THÊM: Xử lý nâng cấp seller
     */
    @FXML
    private void handleUpgradeSeller() {
        if (!(currentUser instanceof RegularUser)) {
            AlertUtil.showError("Lỗi", "Chỉ người dùng thường mới có thể nâng cấp!");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Nâng cấp Seller");
        confirmation.setHeaderText("Xác nhận nâng cấp");
        confirmation.setContentText("Bạn có chắc chắn muốn nâng cấp lên Seller?\n" +
                "Sau đó bạn sẽ có thể tạo phiên đấu giá và quản lý sản phẩm.");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            new Thread(() -> {
                try {
                    Message message = new Message(MessageType.UPGRADE_SELLER, null, currentUser.getUserId());
                    clientSocket.sendMessage(message);

                    Message response = clientSocket.receiveMessage();

                    if (response != null && response.getStatus().equals("SUCCESS")) {
                        currentUser = (User) response.getData();

                        Platform.runLater(() -> {
                            AlertUtil.showSuccess("Thành công", "Bạn đã nâng cấp lên Seller!");
                            updateUI();

                            // Re-initialize seller controller
                            if (sellerController != null) {
                                sellerController.setUserData(currentUser, clientSocket, this);
                            }
                        });

                        LoggerUtil.info("✓ User upgraded to Seller: " + currentUser.getUsername());
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Lỗi không xác định";
                        AlertUtil.showError("Lỗi", errorMsg);
                    }

                } catch (IOException | ClassNotFoundException e) {
                    LoggerUtil.error("Upgrade seller error: " + e.getMessage());
                    AlertUtil.showError("Lỗi", "Lỗi kết nối: " + e.getMessage());
                }
            }).start();
        }
    }

    /**
     * Xử lý nạp tiền
     */
    @FXML
    private void handleAddFunds() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nhập số tiền cần nạp");
        dialog.setContentText("Số tiền (USD):");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                double amount = Double.parseDouble(result.get());

                if (amount <= 0) {
                    AlertUtil.showError("Lỗi", "Vui lòng nhập số tiền dương!");
                    return;
                }

                currentUser.addFunds(amount);
                walletLabel.setText("💳 Tài khoản: $" + String.format("%.2f", currentUser.getWallet()));

                AlertUtil.showSuccess("Thành công", "Bạn đã nạp $" + String.format("%.2f", amount));
                LoggerUtil.info("✓ User added funds: $" + amount);

            } catch (NumberFormatException e) {
                AlertUtil.showError("Lỗi", "Vui lòng nhập số hợp lệ!");
            }
        }
    }

    /**
     * Xử lý logout
     */
    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Xác nhận");
        alert.setHeaderText("Đăng xuất");
        alert.setContentText("Bạn có chắc chắn muốn đăng xuất?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                if (refreshTimer != null) {
                    refreshTimer.cancel();
                }

                if (clientSocket != null) {
                    clientSocket.close();
                }

                Platform.runLater(() -> {
                    if (onLogout != null) {
                        onLogout.run();
                    }
                });

                LoggerUtil.info("✓ User logged out: " + currentUser.getUsername());
            } catch (Exception e) {
                LoggerUtil.error("Logout error: " + e.getMessage());
            }
        }
    }

    /**
     * Auto-refresh wallet & auctions
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    walletLabel.setText("💳 Tài khoản: $" + String.format("%.2f", currentUser.getWallet()));
                });
            }
        }, 5000, 5000);
    }

    /**
     * Cập nhật wallet
     */
    public void updateWallet(double newWallet) {
        currentUser.setWallet(newWallet);
        walletLabel.setText("💳 Tài khoản: $" + String.format("%.2f", newWallet));
    }

    /**
     * Cleanup
     */
    public void cleanup() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        if (bidderController != null) {
            bidderController.cleanup();
        }

        if (sellerController != null) {
            sellerController.cleanup();
        }
    }
}
