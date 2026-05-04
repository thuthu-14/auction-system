package client.controller;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import common.Transaction;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class WalletController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private Label balanceLabel;
    @FXML private ComboBox<String> timeFilter;
    @FXML private TextField searchField;
    @FXML private VBox addBankBox;
    @FXML private VBox linkedBankBox;
    @FXML private Label displayBankName;
    @FXML private Label displayAccNumber;

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, Double> amountColumn;
    @FXML private TableColumn<Transaction, Double> balanceColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;

    private ClientSocket clientSocket;
    private User currentUser;

    private static final String BANK_FILE = "data/json/bank_accounts.json";
    private static final String TRANSACTIONS_FILE = "data/json/transactions.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static class BankAccountEntry {
        String userId;
        String bankName;
        String accountNumber;
        double initialBalance;
        long createdAt;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (timeFilter != null) {
            timeFilter.getItems().addAll("Tháng này", "Tháng trước", "3 tháng gần đây", "Tất cả thời gian");
            timeFilter.getSelectionModel().selectFirst();
        }

        if (dateColumn != null) dateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        if (typeColumn != null) typeColumn.setCellValueFactory(new PropertyValueFactory<>("typeLabel"));
        if (amountColumn != null) amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        if (balanceColumn != null) balanceColumn.setCellValueFactory(new PropertyValueFactory<>("balanceAfter"));
        if (descriptionColumn != null) descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    public void setUserData(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        Platform.runLater(this::loadWalletData);
    }

    private void loadWalletData() {
        this.currentUser = NavigationManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            LoggerUtil.warn("WalletController: currentUser is still null");
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
                overlay.setStyle("-fx-background-color: rgba(229, 231, 235, 0.74);");
                overlay.setPrefSize(container.getWidth() + 100, container.getHeight() + 100);

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
                if (finalOverlay != null && finalContainer != null) {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), finalOverlay);
                    fadeOut.setFromValue(1);
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> finalContainer.getChildren().remove(finalOverlay));
                    fadeOut.play();
                }
            });

            dialogController.setOnBankLinkedListener((bank, account, amount) -> {
                try {
                    saveBankAccountForCurrentUser(bank, account, amount);
                    Platform.runLater(() -> {
                        loadWalletData();
                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Liên kết ngân hàng thành công!");
                    });
                } catch (Exception e) {
                    LoggerUtil.error("Lỗi lưu ngân hàng: " + e.getMessage());
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu thông tin ngân hàng."));
                }
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);

            dialogStage.setOnHiding(e -> {
                if (finalOverlay != null && finalContainer != null && finalContainer.getChildren().contains(finalOverlay)) {
                    finalContainer.getChildren().remove(finalOverlay);
                }
            });

            dialogStage.showAndWait();

        } catch (Exception e) {
            LoggerUtil.error("Lỗi show popup thêm thẻ: " + e.getMessage());
        }
    }

    private void processTransaction(String type, String amountStr, BankAccountEntry bankEntry) {
        try {
            double amount = Double.parseDouble(amountStr.replace(",", "").trim());
            if (amount <= 0) {
                showAlert(Alert.AlertType.WARNING, "Thông báo", "Số tiền phải lớn hơn 0");
                return;
            }

            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("username", currentUser.getUsername());
                    data.put("amount", amount);

                    MessageType msgType = type.equals("DEPOSIT") ? MessageType.ADD_FUNDS : MessageType.WITHDRAW;

                    clientSocket.sendMessage(new Message(msgType, data, currentUser.getUsername()));
                    Message response = clientSocket.receiveMessage();

                    if (response != null && "SUCCESS".equals(response.getStatus())) {
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
                            loadWalletData();
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Giao dịch hoàn tất!");
                        });
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Giao dịch bị từ chối";
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", errorMsg));
                    }

                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể gửi yêu cầu tới Server"));
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập số tiền hợp lệ");
        }
    }

    @FXML private void handleDeposit() {
        BankAccountEntry bankEntry = loadBankAccountForCurrentUser();
        if (bankEntry == null) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng liên kết ngân hàng trước"); return; }

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Nạp tiền");
        // ĐÃ SỬA: Set Header về null để xóa dòng số dư ngân hàng
        d.setHeaderText(null);
        d.setContentText("Nhập số tiền muốn nạp vào ví:");
        d.showAndWait().ifPresent(s -> processTransaction("DEPOSIT", s, bankEntry));
    }

    @FXML private void handleWithdraw() {
        BankAccountEntry bankEntry = loadBankAccountForCurrentUser();
        if (bankEntry == null) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng liên kết ngân hàng trước"); return; }

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Rút tiền");
        d.setHeaderText("Ví hiện có: " + formatVnd(currentUser.getWallet()));
        d.setContentText("Nhập số tiền muốn rút về ngân hàng:");
        d.showAndWait().ifPresent(s -> processTransaction("WITHDRAW", s, bankEntry));
    }

    private String formatVnd(double value) { return String.format("%,.0f đ", value); }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void saveBankAccountForCurrentUser(String bank, String account, double amount) throws Exception {
        Path path = Paths.get(BANK_FILE);
        if (path.getParent() != null && !Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.writeString(path, "[]");
        }

        String content = Files.readString(path).trim();
        if (content.isEmpty() || content.equals("null")) content = "[]";

        List<BankAccountEntry> list = gson.fromJson(content, new TypeToken<List<BankAccountEntry>>(){}.getType());
        if (list == null) list = new ArrayList<>();

        list.removeIf(x -> currentUser.getUserId().equals(x.userId));

        BankAccountEntry e = new BankAccountEntry();
        e.userId = currentUser.getUserId();
        e.bankName = bank;
        e.accountNumber = account;
        e.initialBalance = amount;
        e.createdAt = System.currentTimeMillis();

        list.add(e);
        Files.writeString(path, gson.toJson(list));
    }

    private BankAccountEntry loadBankAccountForCurrentUser() {
        try {
            Path path = Paths.get(BANK_FILE);
            if (!Files.exists(path)) return null;
            String content = Files.readString(path).trim();
            if (content.isEmpty() || content.equals("null")) return null;

            List<BankAccountEntry> list = gson.fromJson(content, new TypeToken<List<BankAccountEntry>>(){}.getType());
            if (list == null) return null;
            return list.stream().filter(e -> e.userId.equals(currentUser.getUserId())).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private void loadTransactionHistory() {
        try {
            if (currentUser == null) return;
            List<Transaction> list = loadTransactionsForUser();
            Platform.runLater(() -> {
                transactionTable.getItems().clear();
                transactionTable.setItems(FXCollections.observableArrayList(list));
                transactionTable.refresh();
            });
        } catch (Exception e) {
            LoggerUtil.error("Lỗi load lịch sử GD: " + e.getMessage());
        }
    }

    private void saveTransaction(String type, double amount, double balanceAfter) throws Exception {
        Path path = Paths.get(TRANSACTIONS_FILE);
        if (path.getParent() != null && !Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        if (!Files.exists(path)) {
            Files.writeString(path, "[]");
        }

        String content = Files.readString(path).trim();
        if (content.isEmpty() || content.equals("null")) content = "[]";

        List<Transaction> txns = gson.fromJson(content, new TypeToken<List<Transaction>>(){}.getType());
        if (txns == null) txns = new ArrayList<>();
        txns.add(new Transaction(currentUser.getUserId(), type, amount, balanceAfter, type.equals("DEPOSIT") ? "Nạp tiền vào ví" : "Rút tiền về ngân hàng"));
        Files.writeString(path, gson.toJson(txns));
    }

    private List<Transaction> loadTransactionsForUser() throws Exception {
        Path path = Paths.get(TRANSACTIONS_FILE);
        if (!Files.exists(path)) return new ArrayList<>();
        String content = Files.readString(path).trim();
        if (content.isEmpty() || content.equals("null")) return new ArrayList<>();

        List<Transaction> all = gson.fromJson(content, new TypeToken<List<Transaction>>(){}.getType());
        if (all == null) return new ArrayList<>();
        return all.stream()
                .filter(t -> currentUser.getUserId().equals(t.userId))
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .collect(java.util.stream.Collectors.toList());
    }

    @FXML private void handleSearch() {}
    @FXML private void filterAll() {}
    @FXML private void filterIn() {}
    @FXML private void filterOut() {}
}
