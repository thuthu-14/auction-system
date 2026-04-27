package client.controller;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import common.Transaction;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import server.model.User;
import util.LoggerUtil;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.lang.reflect.Type;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class WalletController implements Initializable {

    // Thẻ ngoài cùng để chèn overlay, nếu FXML chưa có nó sẽ lấy root của Scene
    @FXML private StackPane rootPane;

    @FXML private Label balanceLabel;
    @FXML private ComboBox<String> timeFilter;
    @FXML private TextField searchField;
    @FXML private VBox addBankBox;
    @FXML private VBox linkedBankBox;
    @FXML private Label displayBankName;
    @FXML private Label displayAccNumber;
    @FXML private Label displayBankBalance;

    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> dateColumn;
    @FXML private TableColumn<Transaction, String> typeColumn;
    @FXML private TableColumn<Transaction, Double> amountColumn;
    @FXML private TableColumn<Transaction, Double> balanceColumn;
    @FXML private TableColumn<Transaction, String> descriptionColumn;

    private ClientSocket clientSocket;
    private User currentUser;

    private static final String BANK_FILE = "data/json/bank_accounts.json";
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final String TRANSACTIONS_FILE = "data/json/transactions.json";

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

        loadWalletData();
    }

    public void setUserData(User currentUser, ClientSocket clientSocket) {
        this.currentUser = currentUser;
        this.clientSocket = clientSocket;
        loadWalletData();
    }

    private void loadWalletData() {
        if (currentUser != null && balanceLabel != null) {
            balanceLabel.setText(formatVnd(currentUser.getWallet()));
        }

        BankAccountEntry entry = loadBankAccountForCurrentUser();
        if (entry != null) {
            if (displayBankName != null) displayBankName.setText(entry.bankName.toUpperCase());
            if (displayAccNumber != null) {
                String acc = entry.accountNumber == null ? "" : entry.accountNumber;
                displayAccNumber.setText(acc.length() >= 4 ? "**** **** **** " + acc.substring(acc.length() - 4) : acc);
            }
            if (displayBankBalance != null) displayBankBalance.setText(formatVnd(entry.initialBalance));
            if (addBankBox != null) { addBankBox.setVisible(false); addBankBox.setManaged(false); }
            if (linkedBankBox != null) { linkedBankBox.setVisible(true); linkedBankBox.setManaged(true); }
        } else {
            if (addBankBox != null) { addBankBox.setVisible(true); addBankBox.setManaged(true); }
            if (linkedBankBox != null) { linkedBankBox.setVisible(false); linkedBankBox.setManaged(false); }
        }
        loadTransactionHistory();
    }

    @FXML
    private void showAddBankPopup() {
        try {
            URL fxmlLocation = getClass().getResource("/fxml/AddBankDialog.fxml");
            if (fxmlLocation == null) return;

            // XỬ LÝ LỖI NULL ROOTPANE: Nếu rootPane null, tìm thằng cha là Pane để chèn overlay
            Pane container = rootPane;
            if (container == null) {
                // Lấy Scene root hiện tại nếu không thấy rootPane
                if (balanceLabel != null && balanceLabel.getScene() != null) {
                    Parent root = balanceLabel.getScene().getRoot();
                    if (root instanceof Pane) container = (Pane) root;
                }
            }

            // Nếu vẫn null thì show dialog bình thường không cần overlay để không bị crash
            Region overlay = null;
            if (container != null) {
                overlay = new Region();
                overlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.5);");
                overlay.setPrefSize(container.getWidth() + 100, container.getHeight() + 100);

                FadeTransition fadeIn = new FadeTransition(Duration.millis(300), overlay);
                fadeIn.setFromValue(0); fadeIn.setToValue(1);

                container.getChildren().add(overlay);
                fadeIn.play();
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();
            AddBankDialogController dialogController = loader.getController();

            final Region finalOverlay = overlay;
            final Pane finalContainer = container;

            dialogController.setOnCloseCallback(() -> {
                if (finalOverlay != null && finalContainer != null) {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), finalOverlay);
                    fadeOut.setFromValue(1); fadeOut.setToValue(0);
                    fadeOut.setOnFinished(e -> finalContainer.getChildren().remove(finalOverlay));
                    fadeOut.play();
                }
            });

            dialogController.setOnBankLinkedListener((bank, account, amount) -> {
                try {
                    saveBankAccountForCurrentUser(bank, account, amount);
                    Platform.runLater(this::loadWalletData);
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã liên kết tài khoản ngân hàng.");
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
                }
            });

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Giữ nguyên các hàm xử lý dữ liệu của sếp ---
    private void processTransaction(String type, String amountStr, BankAccountEntry bankEntry) {
        try {
            double amount = Double.parseDouble(amountStr.replace(",", "").trim());
            if (amount <= 0) return;
            new Thread(() -> {
                try {
                    Map<String, Object> data = new HashMap<>();
                    data.put("username", currentUser.getUsername());
                    data.put("amount", amount);
                    MessageType msgType = type.equals("DEPOSIT") ? MessageType.ADD_FUNDS : MessageType.WITHDRAW;
                    clientSocket.sendMessage(new Message(msgType, data, currentUser.getUsername()));
                    Message response = clientSocket.receiveMessage();
                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        currentUser = (User) response.getData();
                        Platform.runLater(() -> {
                            try {
                                updateBankBalanceForCurrentUser(type.equals("DEPOSIT") ? -amount : amount);
                                saveTransaction(type, amount, currentUser.getWallet());
                                loadWalletData();
                            } catch (Exception ex) { ex.printStackTrace(); }
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        } catch (Exception e) { }
    }

    @FXML private void handleDeposit() {
        BankAccountEntry bankEntry = loadBankAccountForCurrentUser();
        if (bankEntry == null) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa liên kết ngân hàng"); return; }
        TextInputDialog d = new TextInputDialog(); d.setHeaderText("Nạp tiền");
        d.showAndWait().ifPresent(s -> processTransaction("DEPOSIT", s, bankEntry));
    }

    @FXML private void handleWithdraw() {
        BankAccountEntry bankEntry = loadBankAccountForCurrentUser();
        if (bankEntry == null) { showAlert(Alert.AlertType.ERROR, "Lỗi", "Chưa liên kết ngân hàng"); return; }
        TextInputDialog d = new TextInputDialog(); d.setHeaderText("Rút tiền");
        d.showAndWait().ifPresent(s -> processTransaction("WITHDRAW", s, bankEntry));
    }

    private String formatVnd(double value) { return String.format("%,.0f đ", value); }
    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void saveBankAccountForCurrentUser(String bank, String account, double amount) throws Exception {
        Path path = Paths.get(BANK_FILE);
        if (!Files.exists(path)) { Files.createDirectories(path.getParent()); Files.writeString(path, "[]"); }
        List<BankAccountEntry> list = gson.fromJson(Files.readString(path), new TypeToken<List<BankAccountEntry>>(){}.getType());
        if (list == null) list = new ArrayList<>();
        list.removeIf(x -> currentUser.getUserId().equals(x.userId));
        BankAccountEntry e = new BankAccountEntry(); e.userId = currentUser.getUserId();
        e.bankName = bank; e.accountNumber = account; e.initialBalance = amount; e.createdAt = System.currentTimeMillis();
        list.add(e); Files.writeString(path, gson.toJson(list));
    }

    private BankAccountEntry loadBankAccountForCurrentUser() {
        try {
            Path path = Paths.get(BANK_FILE);
            if (!Files.exists(path)) return null;
            List<BankAccountEntry> list = gson.fromJson(Files.readString(path), new TypeToken<List<BankAccountEntry>>(){}.getType());
            return list.stream().filter(e -> e.userId.equals(currentUser.getUserId())).findFirst().orElse(null);
        } catch (Exception e) { return null; }
    }

    private void updateBankBalanceForCurrentUser(double delta) throws Exception {
        Path path = Paths.get(BANK_FILE);
        List<BankAccountEntry> list = gson.fromJson(Files.readString(path), new TypeToken<List<BankAccountEntry>>(){}.getType());
        for (BankAccountEntry e : list) if (e.userId.equals(currentUser.getUserId())) { e.initialBalance += delta; break; }
        Files.writeString(path, gson.toJson(list));
    }

    private void loadTransactionHistory() {
        try {
            if (currentUser == null) return;
            ObservableList<Transaction> data = FXCollections.observableArrayList(loadTransactionsForUser());
            if (transactionTable != null) transactionTable.setItems(data);
        } catch (Exception e) { }
    }

    private void saveTransaction(String type, double amount, double balanceAfter) throws Exception {
        Path path = Paths.get(TRANSACTIONS_FILE);
        if (!Files.exists(path)) { Files.createDirectories(path.getParent()); Files.writeString(path, "[]"); }
        List<Transaction> txns = gson.fromJson(Files.readString(path), new TypeToken<List<Transaction>>(){}.getType());
        if (txns == null) txns = new ArrayList<>();
        txns.add(new Transaction(currentUser.getUserId(), type, amount, balanceAfter, type.equals("DEPOSIT")?"Nạp tiền":"Rút tiền"));
        Files.writeString(path, gson.toJson(txns));
    }

    private List<Transaction> loadTransactionsForUser() throws Exception {
        Path path = Paths.get(TRANSACTIONS_FILE);
        if (!Files.exists(path)) return new ArrayList<>();
        List<Transaction> all = gson.fromJson(Files.readString(path), new TypeToken<List<Transaction>>(){}.getType());
        return (all == null) ? new ArrayList<>() : all.stream().filter(t -> currentUser.getUserId().equals(t.userId))
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp)).collect(java.util.stream.Collectors.toList());
    }

    @FXML private void handleSearch() {}
    @FXML private void filterAll() {}
    @FXML private void filterIn() {}
    @FXML private void filterOut() {}
}