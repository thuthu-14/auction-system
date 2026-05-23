package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.logic.WalletLogic;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.WalletClient;
import client.service.WalletService;
import client.service.WalletService.BankAccountEntry;
import common.Transaction;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import navigation.NavigationManager;
import server.model.User;
import util.LoggerUtil;

import java.net.URL;
import java.util.*;

public class WalletController implements Initializable {

    private static final String WALLET_OVERLAY_STYLE_CLASS = "wallet-modal-overlay";

    @FXML private StackPane rootPane;
    @FXML private Label balanceLabel;
    @FXML private ComboBox<String> timeFilter;
    @FXML private TextField searchField;
    @FXML private Button filterAllBtn;
    @FXML private Button filterInBtn;
    @FXML private Button filterOutBtn;
    @FXML private VBox addBankBox;
    @FXML private VBox linkedBankBox;
    @FXML private Label displayBankName;
    @FXML private Label displayAccNumber;
    @FXML private Button depositButton;
    @FXML private Button withdrawButton;

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, String> moneyInColumn;
    @FXML private TableColumn<Transaction, String> moneyOutColumn;
    @FXML private TableColumn<Transaction, String> balanceColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;

    private ClientSocket clientSocket;
    private User currentUser;
    private List<Transaction> allTransactions = new ArrayList<>();
    private WalletLogic.TransactionFilter activeTransactionFilter = WalletLogic.TransactionFilter.ALL;
    private final WalletClient walletService;
    private final WalletLogic walletLogic = new WalletLogic();
    private WalletViewMode viewMode = WalletViewMode.BIDDER;

    public WalletController() {
        this(new WalletService());
    }

    WalletController(WalletClient walletService) {
        this.walletService = walletService;
    }

    public enum WalletViewMode {
        BIDDER, SELLER
    }

    private static final String ACTIVE_FILTER_STYLE =
            "-fx-background-color: #1a1a1a; -fx-background-radius: 10; -fx-cursor: hand;";
    private static final String INACTIVE_FILTER_STYLE =
            "-fx-background-color: #f3f4f6; -fx-background-radius: 10; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(this::resetWalletViewState);
        installAutoReloadHook();

        if (timeFilter != null) {
            timeFilter.getItems().addAll("Tháng này", "Tháng trước", "3 tháng gần đây", "Tất cả thời gian");
            timeFilter.getSelectionModel().selectFirst();
        }

        updateFilterButtons();

        if (dateColumn != null) dateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        if (typeColumn != null) typeColumn.setCellValueFactory(new PropertyValueFactory<>("typeLabel"));
        if (moneyInColumn != null) moneyInColumn.setCellValueFactory(new PropertyValueFactory<>("moneyIn"));
        if (moneyOutColumn != null) moneyOutColumn.setCellValueFactory(new PropertyValueFactory<>("moneyOut"));
        if (balanceColumn != null) balanceColumn.setCellValueFactory(new PropertyValueFactory<>("formattedBalanceAfter"));
        if (descriptionColumn != null) descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        setupTransactionRowPopup();
        applyViewMode();
    }

    public void setViewMode(WalletViewMode viewMode) {
        this.viewMode = viewMode == null ? WalletViewMode.BIDDER : viewMode;
        Platform.runLater(this::applyViewMode);
    }

    private void applyViewMode() {
        if (viewMode == WalletViewMode.SELLER) {
            applyWalletButtonSize(170, 48, 17);
        } else {
            applyWalletButtonSize(185, 50, 18);
        }
    }

    private void applyWalletButtonSize(double depositWidth, double height, double fontSize) {
        applyWalletButtonSize(depositButton, depositWidth, height, fontSize);
        applyWalletButtonSize(withdrawButton, depositWidth - 20, height, fontSize);
    }

    private void applyWalletButtonSize(Button button, double width, double height, double fontSize) {
        if (button == null) {
            return;
        }
        button.setPrefWidth(width);
        button.setPrefHeight(height);
        button.setMinWidth(width);
        button.setMinHeight(height);
        String colorStyle = button == depositButton
                ? "-fx-background-color: #1a1a1a;"
                : "-fx-background-color: white; -fx-border-color: #e2e8f0;";
        double radius = height / 2.0;
        button.setStyle(colorStyle
                + " -fx-border-radius: " + radius + ";"
                + " -fx-background-radius: " + radius + ";"
                + " -fx-cursor: hand;");
        button.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, fontSize));
        button.setPadding(new Insets(0, 28, 0, 28));
    }

    private void setupTransactionRowPopup() {
        if (transactionTable == null) {
            return;
        }
        transactionTable.setRowFactory(table -> {
            TableRow<Transaction> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY) {
                    showTransactionDetailPopup(row.getItem());
                }
            });
            row.setOnMouseEntered(event -> {
                if (!row.isEmpty()) {
                    row.setStyle("-fx-cursor: hand;");
                }
            });
            return row;
        });
    }

    private void showTransactionDetailPopup(Transaction transaction) {
        if (transaction == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết giao dịch");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (rootPane != null && rootPane.getScene() != null && rootPane.getScene().getWindow() != null) {
            dialog.initOwner(rootPane.getScene().getWindow());
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e5e7eb; -fx-border-radius: 14;"
                + " -fx-padding: 0 0 14 0;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(520);

        Label title = new Label(transaction.getTypeLabel());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label amount = new Label(formatSignedTransactionAmount(transaction));
        amount.setAlignment(Pos.CENTER_LEFT);
        amount.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: "
                + (isMoneyIn(transaction) ? "#047857" : "#dc2626") + ";");

        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(24);
        detailGrid.setVgap(12);
        detailGrid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; -fx-padding: 16;");

        addTransactionDetailRow(detailGrid, 0, "Mã giao dịch", safeText(transaction.id));
        addTransactionDetailRow(detailGrid, 1, "Thời gian", transaction.getFormattedDate());
        addTransactionDetailRow(detailGrid, 2, "Loại giao dịch", transaction.getTypeLabel());
        addTransactionDetailRow(detailGrid, 3, "Tiền vào", walletLogic.emptyToDash(transaction.getMoneyIn()));
        addTransactionDetailRow(detailGrid, 4, "Tiền ra", walletLogic.emptyToDash(transaction.getMoneyOut()));
        addTransactionDetailRow(detailGrid, 5, "Số dư sau", transaction.getFormattedBalanceAfter());
        addTransactionDetailRow(detailGrid, 6, "Mô tả", safeText(transaction.getDescription()));

        content.getChildren().addAll(title, amount, detailGrid);
        pane.setContent(content);
        styleTransactionDetailCloseButton(pane);
        dialog.showAndWait();
    }

    private void styleTransactionDetailCloseButton(DialogPane pane) {
        Node closeNode = pane.lookupButton(ButtonType.CLOSE);
        if (closeNode instanceof Button closeButton) {
            closeButton.setText("Đóng");
            closeButton.setMinHeight(40);
            closeButton.setPrefHeight(40);
            closeButton.setMinWidth(110);
            closeButton.setDefaultButton(true);
            closeButton.setStyle("-fx-background-color: #111827;"
                    + " -fx-text-fill: white;"
                    + " -fx-font-size: 14px;"
                    + " -fx-font-weight: bold;"
                    + " -fx-background-radius: 20;"
                    + " -fx-cursor: hand;"
                    + " -fx-padding: 0 24 0 24;");
        }
    }

    private void addTransactionDetailRow(GridPane grid, int rowIndex, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setMinWidth(130);
        label.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #64748b;");

        Label value = new Label(valueText);
        value.setWrapText(true);
        value.setMaxWidth(320);
        value.setStyle("-fx-font-size: 14px; -fx-text-fill: #111827;");

        grid.add(label, 0, rowIndex);
        grid.add(value, 1, rowIndex);
    }

    private String formatSignedTransactionAmount(Transaction transaction) {
        return walletLogic.formatSignedTransactionAmount(transaction);
    }

    private boolean isMoneyIn(Transaction transaction) {
        return walletLogic.isMoneyIn(transaction);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    public void setUserData(User user, ClientSocket socket) {
        // ← LUÔN lấy user mới nhất từ NavigationManager
        User navUser = NavigationManager.getInstance().getCurrentUser();
        if (navUser != null) {
            this.currentUser = navUser;
        } else if (user != null) {
            this.currentUser = user;
        }

        this.clientSocket = socket != null ? socket : getActiveSocket();
        Platform.runLater(this::loadWalletData);
    }

    public void reloadWalletData() {
        // ← Trước khi load, force sync user từ NavigationManager
        User latestUser = NavigationManager.getInstance().getCurrentUser();
        if (latestUser != null) {
            this.currentUser = latestUser;  // THÊM: Đảm bảo currentUser mới nhất
        }
        Platform.runLater(this::loadWalletData);
    }

    private void installAutoReloadHook() {
        if (rootPane == null) {
            return;
        }

        rootPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                return;
            }
            Platform.runLater(this::loadWalletData);
            newScene.windowProperty().addListener((windowObservable, oldWindow, newWindow) -> {
                if (newWindow != null) {
                    Platform.runLater(this::loadWalletData);
                }
            });
        });
    }

    private void loadWalletData() {
        resetWalletViewState();
        User navigationUser = NavigationManager.getInstance().getCurrentUser();
        if (navigationUser != null) {
            this.currentUser = navigationUser;
        }

        if (currentUser == null) {
            LoggerUtil.warn("WalletController: currentUser is still null");
            Platform.runLater(this::resetWalletViewState);
            return;
        }

        NavigationManager.getInstance().setCurrentUser(currentUser);
        LoggerUtil.info("Wallet load user="
                + currentUser.getUsername()
                + ", id=" + currentUser.getUserId()
                + ", wallet=" + currentUser.getWallet());

        // ← CẬP NHẬP SỐ DƯ NGAY LẬP TỨC (không chờ)
        if (balanceLabel != null) {
            balanceLabel.setText(formatVnd(currentUser.getWallet()));
            balanceLabel.applyCss();  // ← THÊM: Force re-render UI
        }

        // ← Load bank account & transaction async (không block UI)
        client.util.ClientTaskRunner.run(() -> {
            try {
                BankAccountEntry entry = loadBankAccountForCurrentUser();
                LoggerUtil.info("Wallet linked bank found=" + (entry != null));

                Platform.runLater(() -> {
                    if (entry != null) {
                        displayBankName.setText(entry.bankName.toUpperCase());
                        displayAccNumber.setText(walletLogic.maskAccountNumber(entry.accountNumber));

                        addBankBox.setVisible(false);
                        addBankBox.setManaged(false);
                        linkedBankBox.setVisible(true);
                        linkedBankBox.setManaged(true);
                    } else {
                        addBankBox.setVisible(true);
                        addBankBox.setManaged(true);
                        linkedBankBox.setVisible(false);
                        linkedBankBox.setManaged(false);
                    }
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Error loading bank account: " + e.getMessage())));
            }
        });

        loadTransactionHistory();
        Platform.runLater(this::resetWalletViewState);
    }

    public void refreshVisualState() {
        resetWalletViewState();
        Platform.runLater(this::resetWalletViewState);
    }

    @FXML
    private void showAddBankPopup() {
        if (this.currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Dữ liệu người dùng chưa được tải!");
            return;
        }

        try {
            URL fxmlLocation = getClass().getResource("/fxml/BidderView/AddBankDialog.fxml");
            if (fxmlLocation == null) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Không tìm thấy file AddBankDialog.fxml")));
                return;
            }

            Pane container = rootPane;
            if (container == null && balanceLabel != null && balanceLabel.getScene() != null) {
                Parent sceneRoot = balanceLabel.getScene().getRoot();
                if (sceneRoot instanceof Pane) {
                    container = (Pane) sceneRoot;
                }
            }

            Region overlay = null;
            if (container != null) {
                overlay = new Region();
                overlay.getStyleClass().add(WALLET_OVERLAY_STYLE_CLASS);
                overlay.setStyle("-fx-background-color: rgba(229, 231, 235, 0.74);");
                overlay.prefWidthProperty().bind(container.widthProperty());
                overlay.prefHeightProperty().bind(container.heightProperty());

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);

                container.getChildren().add(overlay);
                fadeIn.play();
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            AddBankDialogController dialogController = loader.getController();

            dialogController.setUserData(this.currentUser, this.clientSocket);

            final Region finalOverlay = overlay;
            final Pane finalContainer = container;

            dialogController.setOnCloseCallback(() -> {
                removeWalletOverlay(finalContainer, finalOverlay);
            });

            dialogController.setOnBankLinkedListener((bank, account, amount) -> {
                try {
                    saveBankAccountForCurrentUser(bank, account, amount);
                    Platform.runLater(() -> refreshLinkedBankDisplay(bank, account));
                } catch (Exception e) {
                    ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Lỗi lưu ngân hàng: " + e.getMessage())));
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu thông tin ngân hàng."));
                }
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            if (container != null && container.getScene() != null && container.getScene().getWindow() != null) {
                dialogStage.initOwner(container.getScene().getWindow());
            }
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);

            dialogStage.setOnHiding(e -> {
                removeWalletOverlay(finalContainer, finalOverlay);
            });

            dialogStage.setOnShown(event -> dialogStage.centerOnScreen());
            dialogStage.showAndWait();

        } catch (Exception e) {
            clearWalletOverlay();
            ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Lỗi show popup thêm thẻ: " + e.getMessage())));
        }
    }

    private void clearWalletOverlay() {
        if (rootPane == null) {
            return;
        }
        rootPane.getChildren().removeIf(node -> rootPane.getChildren().indexOf(node) > 0
                && (node instanceof Region || node.getStyleClass().contains(WALLET_OVERLAY_STYLE_CLASS)));
    }

    private void resetWalletViewState() {
        if (rootPane == null) {
            return;
        }

        clearWalletOverlay();
        rootPane.applyCss();
        rootPane.layout();
        resetNodeState(rootPane);
        applyWalletLabelColors(rootPane);
    }

    private void resetNodeState(Node node) {
        if (!node.opacityProperty().isBound()) {
            node.setOpacity(1);
        }
        if (!node.disableProperty().isBound()) {
            node.setDisable(false);
        }

        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                resetNodeState(child);
            }
        }
    }

    private void applyWalletLabelColors(Parent parent) {
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Label label) {
                applyWalletLabelColor(label);
            }
            if (child instanceof Parent childParent) {
                applyWalletLabelColors(childParent);
            }
        }
    }

    private void applyWalletLabelColor(Label label) {
        String text = label.getText() == null ? "" : label.getText();
        double fontSize = label.getFont() == null ? 14 : label.getFont().getSize();

        if (label == balanceLabel || label == displayBankName || fontSize >= 20) {
            setLabelColor(label, "#1a1a1a");
            return;
        }

        if (text.startsWith("+ Thêm")) {
            setLabelColor(label, "#4a5568");
            return;
        }

        setLabelColor(label, "#718096");
    }

    private void setLabelColor(Label label, String color) {
        label.setTextFill(Color.web(color));

        String style = label.getStyle() == null ? "" : label.getStyle();
        style = style.replaceAll("-fx-text-fill\\s*:\\s*[^;]+;?", "").trim();
        if (!style.isEmpty() && !style.endsWith(";")) {
            style += ";";
        }
        label.setStyle(style + " -fx-text-fill: " + color + ";");
    }

    private void removeWalletOverlay(Pane container, Region overlay) {
        if (container == null || overlay == null) {
            return;
        }

        if (!container.getChildren().contains(overlay)) {
            return;
        }

        overlay.prefWidthProperty().unbind();
        overlay.prefHeightProperty().unbind();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), overlay);
        fadeOut.setFromValue(overlay.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> container.getChildren().remove(overlay));
        fadeOut.play();
    }

    private void processTransaction(String type, String amountStr, BankAccountEntry bankEntry) {
        try {
            double amount = parseMoneyAmount(amountStr);
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Số tiền phải lớn hơn 0");
                return;
            }

            if ("WITHDRAW".equals(type) && currentUser != null && amount > currentUser.getWallet()) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Số dư ví không đủ để rút số tiền này.");
                return;
            }

            client.util.ClientTaskRunner.run(() -> {
                try {
                    var response = walletService.submitWalletTransaction(getActiveSocket(), currentUser, type, amount);

                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        if (!(response.getData() instanceof User)) {
                            throw new IllegalStateException("Invalid wallet response data: " + response.getData());
                        }
                        User updatedUser = (User) response.getData();

                        Platform.runLater(() -> {
                            NavigationManager.getInstance().setCurrentUser(updatedUser);
                            this.currentUser = updatedUser;
                            refreshWalletAfterTransaction(updatedUser);
                            showAlert(Alert.AlertType.INFORMATION, "Th\u00e0nh c\u00f4ng", "Giao d\u1ecbch ho\u00e0n t\u1ea5t!");
                        });
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Giao d\u1ecbch b\u1ecb t\u1eeb ch\u1ed1i";
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "L\u1ed7i", errorMsg));
                    }

                } catch (Exception e) {
                    ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Wallet transaction failed: " + e.getMessage())));
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "L\u1ed7i m\u1ea1ng", "Kh\u00f4ng th\u1ec3 g\u1eedi y\u00eau c\u1ea7u t\u1edbi Server"));
                }
            });

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số tiền hợp lệ");
        }
    }

    private double parseMoneyAmount(String amountStr) {
        return walletService.parseMoneyAmount(amountStr);
    }

    private void refreshWalletAfterTransaction(User updatedUser) {
        if (updatedUser == null) {
            loadWalletData();
            return;
        }

        this.currentUser = updatedUser;

        // ← QUAN TRỌNG: Update NavigationManager để Seller/Bidder khác cũng thấy
        NavigationManager.getInstance().setCurrentUser(updatedUser);

        if (balanceLabel != null) {
            balanceLabel.setText(formatVnd(updatedUser.getWallet()));
            balanceLabel.applyCss();  // ← THÊM: Force re-render UI
        }

        // ← Load bank account & transaction async (không block UI)
        client.util.ClientTaskRunner.run(() -> {
            try {
                BankAccountEntry entry = loadBankAccountForCurrentUser();
                if (entry != null) {
                    Platform.runLater(() -> refreshLinkedBankDisplay(entry.bankName, entry.accountNumber));
                }

                List<Transaction> transactions = loadTransactionsForUser();
                Platform.runLater(() -> {
                    allTransactions = new ArrayList<>(transactions);
                    applyTransactionFilters();
                });
            } catch (Exception e) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Lỗi refresh lịch sử giao dịch: " + e.getMessage())));
            }
        });

        Platform.runLater(this::resetWalletViewState);
    }

    private ClientSocket getActiveSocket() {
        if (clientSocket != null && clientSocket.isConnected()) {
            return clientSocket;
        }

        ClientSocket managerSocket = ConnectionManager.getInstance().getClientSocket();
        if (managerSocket != null && managerSocket.isConnected()) {
            clientSocket = managerSocket;
            return managerSocket;
        }

        ClientSocket navigationSocket = NavigationManager.getInstance().getClientSocket();
        if (navigationSocket != null && navigationSocket.isConnected()) {
            clientSocket = navigationSocket;
            return navigationSocket;
        }

        return null;
    }

    @FXML private void handleDeposit() {
        BankAccountEntry bankEntry = loadBankAccountForCurrentUser();
        if (bankEntry == null) {
            promptBankLinkBeforeTransaction();
            return;
        }

        showTransactionPopup("DEPOSIT", bankEntry);
    }

    @FXML private void handleWithdraw() {
        BankAccountEntry bankEntry = loadBankAccountForCurrentUser();
        if (bankEntry == null) {
            promptBankLinkBeforeTransaction();
            return;
        }

        showTransactionPopup("WITHDRAW", bankEntry);
    }

    private void promptBankLinkBeforeTransaction() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cần liên kết ngân hàng");
        alert.setHeaderText("Cần liên kết ngân hàng");
        alert.setContentText("Vui lòng liên kết tài khoản ngân hàng trước khi nạp hoặc rút tiền.");
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);

        client.util.DialogUtil.prepareDialog(alert, rootPane);

        alert.showAndWait()
                .filter(ButtonType.OK::equals)
                .ifPresent(button -> showAddBankPopup());
    }

    private void showTransactionPopup(String type, BankAccountEntry bankEntry) {
        try {
            URL fxmlLocation = getClass().getResource("/fxml/BidderView/WalletTransactionDialog.fxml");
            if (fxmlLocation == null) {
                ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Không tìm thấy file WalletTransactionDialog.fxml")));
                return;
            }

            Pane container = rootPane;
            if (container == null && balanceLabel != null && balanceLabel.getScene() != null) {
                Parent sceneRoot = balanceLabel.getScene().getRoot();
                if (sceneRoot instanceof Pane) {
                    container = (Pane) sceneRoot;
                }
            }

            Region overlay = null;
            if (container != null) {
                overlay = new Region();
                overlay.getStyleClass().add(WALLET_OVERLAY_STYLE_CLASS);
                overlay.setStyle("-fx-background-color: rgba(229, 231, 235, 0.74);");
                overlay.prefWidthProperty().bind(container.widthProperty());
                overlay.prefHeightProperty().bind(container.heightProperty());
                container.getChildren().add(overlay);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(220), overlay);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            WalletTransactionDialogController dialogController = loader.getController();
            dialogController.setTransactionData(type, currentUser != null ? formatVnd(currentUser.getWallet()) : "");
            dialogController.setOnConfirmListener(amount -> processTransaction(type, amount, bankEntry));

            final Region finalOverlay = overlay;
            final Pane finalContainer = container;
            dialogController.setOnCloseCallback(() -> removeWalletOverlay(finalContainer, finalOverlay));

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            if (container != null && container.getScene() != null && container.getScene().getWindow() != null) {
                dialogStage.initOwner(container.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);
            dialogStage.setOnHiding(e -> removeWalletOverlay(finalContainer, finalOverlay));
            dialogStage.setOnShown(event -> dialogStage.centerOnScreen());
            dialogStage.showAndWait();
        } catch (Exception e) {
            clearWalletOverlay();
            ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Lỗi show popup giao dịch ví: " + e.getMessage())));
        }
    }

    private String formatVnd(double value) { return walletLogic.formatVnd(value); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        client.util.DialogUtil.showAlert(type, title, null, msg, rootPane != null ? rootPane : balanceLabel);
    }




    private void loadTransactionHistory() {
        try {
            if (currentUser == null) return;
            List<Transaction> list = loadTransactionsForUser();
            LoggerUtil.info("Wallet transactions loaded=" + list.size()
                    + " for user=" + currentUser.getUserId());
            Platform.runLater(() -> {
                allTransactions = new ArrayList<>(list);
                syncBalanceFromTransactions(allTransactions);
                applyTransactionFilters();
            });
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.UNKNOWN, "Client error", new RuntimeException(String.valueOf("Lỗi load lịch sử GD: " + e.getMessage())));
        }
    }

    private void saveTransaction(String type, double amount, double balanceAfter) throws Exception {

    }

    private List<Transaction> loadTransactionsForUser() throws Exception {
        if (currentUser == null || currentUser.getUserId() == null) {
            return new ArrayList<>();
        }
        return walletService.fetchTransactionList(getActiveSocket(), currentUser);
    }

    private void syncBalanceFromTransactions(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty() || currentUser == null || currentUser.getUserId() == null) {
            return;
        }

        Transaction latest = transactions.stream()
                .filter(t -> t != null && currentUser.getUserId().equals(t.getUserId()))
                .max(Comparator.comparingLong(t -> t.timestamp))
                .orElse(null);
        if (latest == null) {
            return;
        }

        double latestBalance = latest.getBalanceAfter();
        currentUser.setWallet(latestBalance);
        NavigationManager.getInstance().setCurrentUser(currentUser);
        if (balanceLabel != null) {
            balanceLabel.setText(formatVnd(latestBalance));
            balanceLabel.applyCss();
        }
        LoggerUtil.info("Wallet balance synced from latest transaction: " + latestBalance);
    }

    private void showTransactions(List<Transaction> list) {
        transactionTable.getItems().clear();
        transactionTable.setItems(FXCollections.observableArrayList(list));
        transactionTable.refresh();
    }

    @FXML public void applyTransactionFilters() {
        updateFilterButtons();
        String keyword = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase();
        int selectedTimeIndex = timeFilter == null ? -1 : timeFilter.getSelectionModel().getSelectedIndex();
        List<Transaction> filtered = walletLogic.filterTransactions(allTransactions,
                activeTransactionFilter,
                selectedTimeIndex,
                keyword,
                System.currentTimeMillis());
        showTransactions(filtered);
    }

    @FXML private void handleSearch() {
        applyTransactionFilters();
    }

    @FXML private void filterAll() {
        setActiveTransactionFilter(WalletLogic.TransactionFilter.ALL);
    }

    @FXML private void filterIn() {
        setActiveTransactionFilter(WalletLogic.TransactionFilter.IN);
    }

    @FXML private void filterOut() {
        setActiveTransactionFilter(WalletLogic.TransactionFilter.OUT);
    }

    private void setActiveTransactionFilter(WalletLogic.TransactionFilter filter) {
        activeTransactionFilter = filter;
        applyTransactionFilters();
    }

    private boolean matchesActiveTypeFilter(Transaction transaction) {
        return walletLogic.matchesTypeFilter(transaction, activeTransactionFilter);
    }

    private boolean matchesTimeFilter(Transaction transaction) {
        int selectedIndex = timeFilter == null ? -1 : timeFilter.getSelectionModel().getSelectedIndex();
        return walletLogic.matchesTimeFilter(transaction, selectedIndex, System.currentTimeMillis());
    }

    private boolean matchesSearchKeyword(Transaction transaction, String keyword) {
        return walletLogic.matchesSearchKeyword(transaction, keyword);
    }

    private void updateFilterButtons() {
        styleFilterButton(filterAllBtn, activeTransactionFilter == WalletLogic.TransactionFilter.ALL);
        styleFilterButton(filterInBtn, activeTransactionFilter == WalletLogic.TransactionFilter.IN);
        styleFilterButton(filterOutBtn, activeTransactionFilter == WalletLogic.TransactionFilter.OUT);
    }

    private void styleFilterButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.setStyle(active ? ACTIVE_FILTER_STYLE : INACTIVE_FILTER_STYLE);
        button.setTextFill(active ? Color.WHITE : Color.web("#4a5568"));
    }

    private BankAccountEntry loadBankAccountForCurrentUser() {
        if (currentUser == null) {
            return null;
        }

        try {
            ClientSocket socket = getActiveSocket();
            if (socket == null) {
                LoggerUtil.warn("No active socket to load bank account");
                return null;
            }
            return walletService.loadBankAccount(socket, currentUser);
        } catch (Exception e) {
            LoggerUtil.warn("Non-critical: Failed to load bank account: " + e.getMessage());
        }

        return null;
    }

    private void saveBankAccountForCurrentUser(String bankName, String accountNumber, double initialBalance) throws Exception {
        ClientSocket socket = getActiveSocket();
        walletService.saveBankAccount(socket, currentUser, bankName, accountNumber, initialBalance);
    }

    private void refreshLinkedBankDisplay(String bankName, String accountNumber) {
        if (displayBankName != null) {
            displayBankName.setText(bankName.toUpperCase());
        }
        if (displayAccNumber != null) {
            displayAccNumber.setText(walletLogic.maskAccountNumber(accountNumber));
        }
        if (addBankBox != null && linkedBankBox != null) {
            addBankBox.setVisible(false);
            addBankBox.setManaged(false);
            linkedBankBox.setVisible(true);
            linkedBankBox.setManaged(true);
        }
    }

}

