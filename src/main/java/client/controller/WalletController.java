package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.WalletService;
import client.service.WalletService.BankAccountEntry;
import common.Message;
import common.Transaction;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
    private TransactionFilter activeTransactionFilter = TransactionFilter.ALL;
    private final WalletService walletService = new WalletService();

    private enum TransactionFilter {
        ALL, IN, OUT
    }

    private static final String ACTIVE_FILTER_STYLE =
            "-fx-background-color: #1a1a1a; -fx-background-radius: 10; -fx-cursor: hand;";
    private static final String INACTIVE_FILTER_STYLE =
            "-fx-background-color: #f3f4f6; -fx-background-radius: 10; -fx-cursor: hand;";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Platform.runLater(this::resetWalletViewState);

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
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        Platform.runLater(this::loadWalletData);
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

        balanceLabel.setText(formatVnd(currentUser.getWallet()));

        BankAccountEntry entry = loadBankAccountForCurrentUser();

        if (entry != null) {
            displayBankName.setText(entry.bankName.toUpperCase());
            String acc = entry.accountNumber == null ? "" : entry.accountNumber;
            displayAccNumber.setText(acc.length() >= 4 ? "**** **** **** " + acc.substring(acc.length() - 4) : acc);

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
            URL fxmlLocation = getClass().getResource("/fxml/AddBankDialog.fxml");
            if (fxmlLocation == null) {
                LoggerUtil.error("Không tìm thấy file AddBankDialog.fxml");
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
                    LoggerUtil.error("Lỗi lưu ngân hàng: " + e.getMessage());
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
            LoggerUtil.error("Lỗi show popup thêm thẻ: " + e.getMessage());
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
        node.setOpacity(1);
        node.setDisable(false);

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

            new Thread(() -> {
                try {
                    Message response = walletService.submitWalletTransaction(getActiveSocket(), currentUser, type, amount);

                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        if (!(response.getData() instanceof User)) {
                            throw new IllegalStateException("Invalid wallet response data: " + response.getData());
                        }
                        User updatedUser = (User) response.getData();

                        Platform.runLater(() -> {
                            NavigationManager.getInstance().setCurrentUser(updatedUser);
                            this.currentUser = updatedUser;

                            try {
                                // ĐÃ XÓA: updateBankBalanceForCurrentUser - Không còn can thiệp tiền ngân hàng ảo
                                saveTransaction(type, amount, updatedUser.getWallet());
                            } catch (Exception ex) {
                                LoggerUtil.error("Lỗi lưu file local: " + ex.getMessage());
                            }
                            refreshWalletAfterTransaction(updatedUser);
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Giao dịch hoàn tất!");
                        });
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Giao dịch bị từ chối";
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", errorMsg));
                    }

                } catch (Exception e) {
                    LoggerUtil.error("Wallet transaction failed: " + e.getMessage());
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu tới Server"));
                }
            }).start();

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
        if (balanceLabel != null) {
            balanceLabel.setText(formatVnd(updatedUser.getWallet()));
        }

        BankAccountEntry entry = loadBankAccountForCurrentUser();
        if (entry != null) {
            refreshLinkedBankDisplayOnly(entry.bankName, entry.accountNumber);
        }

        try {
            allTransactions = new ArrayList<>(loadTransactionsForUser());
            applyTransactionFilters();
        } catch (Exception e) {
            LoggerUtil.error("Lỗi refresh lịch sử giao dịch: " + e.getMessage());
        }

        resetWalletViewState();
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
        if (bankEntry == null) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng liên kết ngân hàng trước"); return; }

        showTransactionPopup("DEPOSIT", bankEntry);
    }

    @FXML private void handleWithdraw() {
        BankAccountEntry bankEntry = loadBankAccountForCurrentUser();
        if (bankEntry == null) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng liên kết ngân hàng trước"); return; }

        showTransactionPopup("WITHDRAW", bankEntry);
    }

    private void showTransactionPopup(String type, BankAccountEntry bankEntry) {
        try {
            URL fxmlLocation = getClass().getResource("/fxml/WalletTransactionDialog.fxml");
            if (fxmlLocation == null) {
                LoggerUtil.error("Không tìm thấy file WalletTransactionDialog.fxml");
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
            LoggerUtil.error("Lỗi show popup giao dịch ví: " + e.getMessage());
        }
    }

    private String formatVnd(double value) { return String.format("%,.0f đ", value); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        client.util.DialogUtil.showAlert(type, title, null, msg, rootPane != null ? rootPane : balanceLabel);
    }

    private void saveBankAccountForCurrentUser(String bank, String account, double amount) throws Exception {
        walletService.saveBankAccount(currentUser, bank, account, amount);
    }

    private BankAccountEntry loadBankAccountForCurrentUser() {
        return walletService.loadBankAccount(currentUser);
    }

    private void refreshLinkedBankDisplay(String bank, String account) {
        refreshLinkedBankDisplayOnly(bank, account);
        resetWalletViewState();
        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Liên kết ngân hàng thành công!");
    }

    private void refreshLinkedBankDisplayOnly(String bank, String account) {
        if (displayBankName != null) {
            displayBankName.setText(bank == null ? "" : bank.toUpperCase());
        }
        if (displayAccNumber != null) {
            String acc = account == null ? "" : account;
            displayAccNumber.setText(acc.length() >= 4 ? "**** **** **** " + acc.substring(acc.length() - 4) : acc);
        }
        if (addBankBox != null) {
            addBankBox.setVisible(false);
            addBankBox.setManaged(false);
        }
        if (linkedBankBox != null) {
            linkedBankBox.setVisible(true);
            linkedBankBox.setManaged(true);
        }
    }

    private void loadTransactionHistory() {
        try {
            if (currentUser == null) return;
            List<Transaction> list = loadTransactionsForUser();
            Platform.runLater(() -> {
                allTransactions = new ArrayList<>(list);
                applyTransactionFilters();
            });
        } catch (Exception e) {
            LoggerUtil.error("Lỗi load lịch sử GD: " + e.getMessage());
        }
    }

    private void saveTransaction(String type, double amount, double balanceAfter) throws Exception {
        walletService.saveTransaction(currentUser, type, amount, balanceAfter);
    }

    private List<Transaction> loadTransactionsForUser() throws Exception {
        return walletService.loadTransactions(currentUser);
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
        List<Transaction> filtered = allTransactions.stream()
                .filter(this::matchesActiveTypeFilter)
                .filter(this::matchesTimeFilter)
                .filter(t -> keyword.isEmpty() || matchesSearchKeyword(t, keyword))
                .collect(java.util.stream.Collectors.toList());
        showTransactions(filtered);
    }

    @FXML private void handleSearch() {
        applyTransactionFilters();
    }

    @FXML private void filterAll() {
        setActiveTransactionFilter(TransactionFilter.ALL);
    }

    @FXML private void filterIn() {
        setActiveTransactionFilter(TransactionFilter.IN);
    }

    @FXML private void filterOut() {
        setActiveTransactionFilter(TransactionFilter.OUT);
    }

    private void setActiveTransactionFilter(TransactionFilter filter) {
        activeTransactionFilter = filter;
        animateActiveFilterButton();
        applyTransactionFilters();
    }

    private boolean matchesActiveTypeFilter(Transaction transaction) {
        if (activeTransactionFilter == TransactionFilter.IN) {
            return "DEPOSIT".equals(transaction.type);
        }
        if (activeTransactionFilter == TransactionFilter.OUT) {
            return "WITHDRAW".equals(transaction.type);
        }
        return true;
    }

    private boolean matchesTimeFilter(Transaction transaction) {
        if (timeFilter == null || timeFilter.getSelectionModel().getSelectedIndex() < 0) {
            return true;
        }

        int selectedIndex = timeFilter.getSelectionModel().getSelectedIndex();
        if (selectedIndex == 3) {
            return true;
        }

        Calendar transactionDate = Calendar.getInstance();
        transactionDate.setTimeInMillis(transaction.timestamp);

        Calendar now = Calendar.getInstance();
        if (selectedIndex == 0) {
            return transactionDate.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    && transactionDate.get(Calendar.MONTH) == now.get(Calendar.MONTH);
        }

        if (selectedIndex == 1) {
            now.add(Calendar.MONTH, -1);
            return transactionDate.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                    && transactionDate.get(Calendar.MONTH) == now.get(Calendar.MONTH);
        }

        Calendar threeMonthsAgo = Calendar.getInstance();
        threeMonthsAgo.add(Calendar.MONTH, -3);
        return transaction.timestamp >= threeMonthsAgo.getTimeInMillis();
    }

    private boolean matchesSearchKeyword(Transaction transaction, String keyword) {
        String description = transaction.description == null ? "" : transaction.description.toLowerCase();
        String typeLabel = transaction.getTypeLabel() == null ? "" : transaction.getTypeLabel().toLowerCase();
        return description.contains(keyword)
                || typeLabel.contains(keyword)
                || transaction.getFormattedDate().contains(keyword);
    }

    private void updateFilterButtons() {
        styleFilterButton(filterAllBtn, activeTransactionFilter == TransactionFilter.ALL);
        styleFilterButton(filterInBtn, activeTransactionFilter == TransactionFilter.IN);
        styleFilterButton(filterOutBtn, activeTransactionFilter == TransactionFilter.OUT);
    }

    private void styleFilterButton(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.setStyle(active ? ACTIVE_FILTER_STYLE : INACTIVE_FILTER_STYLE);
        button.setTextFill(active ? Color.WHITE : Color.web("#4a5568"));
    }

    private void animateActiveFilterButton() {
        Button activeButton = switch (activeTransactionFilter) {
            case IN -> filterInBtn;
            case OUT -> filterOutBtn;
            default -> filterAllBtn;
        };
        if (activeButton == null) {
            return;
        }
        ScaleTransition transition = new ScaleTransition(Duration.millis(110), activeButton);
        transition.setFromX(0.96);
        transition.setFromY(0.96);
        transition.setToX(1);
        transition.setToY(1);
        transition.play();
    }
}
