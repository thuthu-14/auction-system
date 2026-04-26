package client.controller;

import client.network.ClientSocket;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import server.model.User;
import util.LoggerUtil;
import client.controller.AddBankDialogController;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;




import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import common.Transaction;




public class WalletController implements Initializable {
    // ==========================================
    // CÁC THÀNH PHẦN Ở MÀN HÌNH CHÍNH
    // ==========================================
    @FXML private Label balanceLabel;
    @FXML private ComboBox<String> timeFilter;
    @FXML private TextField searchField;
    @FXML private VBox addBankBox;
    @FXML private VBox linkedBankBox;
    @FXML private Label displayBankName;
    @FXML private Label displayAccNumber;
    @FXML private Label displayBankBalance;

    // ← THÊM: Transaction history table
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

        // ← THÊM: Init table columns
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
            double balance = currentUser.getWallet();
            balanceLabel.setText(formatVnd(balance));
            LoggerUtil.info("Loaded wallet balance: " + balance);
        } else if (balanceLabel != null) {
            balanceLabel.setText(formatVnd(0));
        }

        // Load bank account theo user hiện tại
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
    private void handleDeposit() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nạp tiền");
        dialog.setHeaderText("Nhập số tiền muốn nạp (VND)");
        dialog.setContentText("Số tiền:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        try {
            double amount = Double.parseDouble(result.get().replace(",", "").trim());
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền phải > 0");
                return;
            }

            if (clientSocket == null || currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không kết nối với server");
                return;
            }

            new Thread(() -> {
                try {
                    // Tạo message ADD_FUNDS
                    Map<String, Object> depositData = new HashMap<>();
                    depositData.put("username", currentUser.getUsername());
                    depositData.put("amount", amount);

                    Message message = new Message(MessageType.ADD_FUNDS, depositData, currentUser.getUsername());
                    clientSocket.sendMessage(message);

                    // Nhận response từ server
                    Message response = clientSocket.receiveMessage();

                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        // Cập nhật user object
                        User updatedUser = (User) response.getData();
                        if (updatedUser != null) {
                            currentUser = updatedUser;
                            Platform.runLater(() -> {
                                try {
                                    // ← THÊM: Save transaction
                                    saveTransaction("DEPOSIT", amount, currentUser.getWallet());
                                    loadWalletData();
                                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                            "Nạp tiền thành công! Số dư mới: " + formatVnd(currentUser.getWallet()));
                                } catch (Exception ex) {
                                    LoggerUtil.error("Save transaction error: " + ex.getMessage());
                                }
                            });

                        }
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Nạp tiền thất bại";
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", errorMsg));
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Deposit error: " + e.getMessage());
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi kết nối: " + e.getMessage()));
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ");
        }
    }

    @FXML
    private void handleWithdraw() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rút tiền");
        dialog.setHeaderText("Nhập số tiền muốn rút (VND)");
        dialog.setContentText("Số tiền:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        try {
            double amount = Double.parseDouble(result.get().replace(",", "").trim());
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền phải > 0");
                return;
            }

            if (amount > currentUser.getWallet()) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Số dư không đủ!");
                return;
            }

            if (clientSocket == null || currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không kết nối với server");
                return;
            }

            new Thread(() -> {
                try {
                    Map<String, Object> withdrawData = new HashMap<>();
                    withdrawData.put("username", currentUser.getUsername());
                    withdrawData.put("amount", amount);

                    Message message = new Message(MessageType.WITHDRAW, withdrawData, currentUser.getUsername());
                    clientSocket.sendMessage(message);

                    Message response = clientSocket.receiveMessage();

                    if (response != null && "SUCCESS".equals(response.getStatus())) {
                        User updatedUser = (User) response.getData();
                        if (updatedUser != null) {
                            currentUser = updatedUser;
                            Platform.runLater(() -> {
                                try {
                                    // ← THÊM: Save transaction
                                    saveTransaction("WITHDRAW", amount, currentUser.getWallet());
                                    loadWalletData();
                                    showAlert(Alert.AlertType.INFORMATION, "Thành công",
                                            "Rút tiền thành công! Số dư mới: " + formatVnd(currentUser.getWallet()));
                                } catch (Exception ex) {
                                    LoggerUtil.error("Save transaction error: " + ex.getMessage());
                                }
                            });

                        }
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Rút tiền thất bại";
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", errorMsg));
                    }
                } catch (Exception e) {
                    LoggerUtil.error("Withdraw error: " + e.getMessage());
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi kết nối: " + e.getMessage()));
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Số tiền không hợp lệ");
        }
    }

    @FXML
    private void showAddBankPopup() {
        try {
            URL fxmlLocation = getClass().getResource("/fxml/AddBankDialog.fxml");
            if (fxmlLocation == null) {
                LoggerUtil.error("Không tìm thấy file AddBankDialog.fxml");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            AddBankDialogController dialogController = loader.getController();
            dialogController.setOnBankLinkedListener((bank, account, amount) -> {
                // ← THÊM: Save file bank account trước (dòng dưới)
                try {
                    saveBankAccountForCurrentUser(bank, account, amount);
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không lưu được tài khoản: " + e.getMessage());
                    return;
                }

                if (displayBankName != null) displayBankName.setText(bank.toUpperCase());

                if (displayAccNumber != null) {
                    if (account.length() >= 4) {
                        displayAccNumber.setText("**** **** **** " + account.substring(account.length() - 4));
                    } else {
                        displayAccNumber.setText(account);
                    }
                }

                if (displayBankBalance != null) displayBankBalance.setText(formatVnd(amount));

                if (addBankBox != null) {
                    addBankBox.setVisible(false);
                    addBankBox.setManaged(false);
                }
                if (linkedBankBox != null) {
                    linkedBankBox.setVisible(true);
                    linkedBankBox.setManaged(true);
                }

                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã liên kết tài khoản ngân hàng.");
            });


            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);

            dialogStage.setScene(scene);
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (Exception e) {
            LoggerUtil.error("Error loading bank dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField != null ? searchField.getText() : "";
        LoggerUtil.info("Tìm kiếm giao dịch: " + keyword);
    }

    @FXML private void filterAll() { LoggerUtil.info("Lọc tất cả giao dịch"); }
    @FXML private void filterIn() { LoggerUtil.info("Lọc tiền vào"); }
    @FXML private void filterOut() { LoggerUtil.info("Lọc tiền ra"); }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String formatVnd(double value) {
        return String.format("%,.0f đ", value);
    }

    private void saveBankAccountForCurrentUser(String bank, String account, double amount) throws Exception {
        if (currentUser == null || currentUser.getUserId() == null || currentUser.getUserId().isBlank()) {
            throw new IllegalStateException("Không có thông tin user hiện tại.");
        }

        Path path = Paths.get(BANK_FILE);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "[]");
        }

        Type listType = new TypeToken<List<BankAccountEntry>>() {}.getType();
        String json = Files.readString(path);
        List<BankAccountEntry> list = (json == null || json.trim().isEmpty())
                ? new ArrayList<>()
                : gson.fromJson(json, listType);

        if (list == null) list = new ArrayList<>();

        String uid = currentUser.getUserId();
        list.removeIf(x -> uid.equals(x.userId)); // mỗi user 1 bank account

        BankAccountEntry entry = new BankAccountEntry();
        entry.userId = uid;
        entry.bankName = bank;
        entry.accountNumber = account;
        entry.initialBalance = amount;
        entry.createdAt = System.currentTimeMillis();

        list.add(entry);
        Files.writeString(path, gson.toJson(list));
    }

    private BankAccountEntry loadBankAccountForCurrentUser() {
        try {
            if (currentUser == null || currentUser.getUserId() == null || currentUser.getUserId().isBlank()) {
                return null;
            }

            Path path = Paths.get(BANK_FILE);
            if (!Files.exists(path)) return null;

            Type listType = new TypeToken<List<BankAccountEntry>>() {}.getType();
            String json = Files.readString(path);
            List<BankAccountEntry> list = (json == null || json.trim().isEmpty())
                    ? new ArrayList<>()
                    : gson.fromJson(json, listType);

            if (list == null) return null;

            String uid = currentUser.getUserId();
            for (BankAccountEntry e : list) {
                if (uid.equals(e.userId)) return e;
            }
        } catch (Exception e) {
            LoggerUtil.error("Load bank account error: " + e.getMessage());
        }
        return null;
    }

    // ← THÊM: Load lịch sử giao dịch
    private void loadTransactionHistory() {
        try {
            if (currentUser == null) return;

            List<Transaction> txns = loadTransactionsForUser();
            ObservableList<Transaction> data = FXCollections.observableArrayList(txns);
            if (transactionTable != null) {
                transactionTable.setItems(data);
            }
        } catch (Exception e) {
            LoggerUtil.error("Load transaction error: " + e.getMessage());
        }
    }

    // ← THÊM: Lưu giao dịch vào file
    private void saveTransaction(String type, double amount, double balanceAfter) throws Exception {
        if (currentUser == null) return;

        Path path = Paths.get(TRANSACTIONS_FILE);
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "[]");
        }

        Type listType = new TypeToken<List<Transaction>>() {}.getType();
        String json = Files.readString(path);
        List<Transaction> txns = (json == null || json.trim().isEmpty())
                ? new ArrayList<>()
                : gson.fromJson(json, listType);

        if (txns == null) txns = new ArrayList<>();

        Transaction txn = new Transaction(currentUser.getUserId(), type, amount, balanceAfter,
                type.equals("DEPOSIT") ? "Nạp tiền từ ngân hàng" : "Rút tiền về ngân hàng");
        txns.add(txn);

        Files.writeString(path, gson.toJson(txns));
    }

    // ← THÊM: Load giao dịch của user hiện tại
    private List<Transaction> loadTransactionsForUser() throws Exception {
        Path path = Paths.get(TRANSACTIONS_FILE);
        if (!Files.exists(path)) return new ArrayList<>();

        Type listType = new TypeToken<List<Transaction>>() {}.getType();
        String json = Files.readString(path);
        List<Transaction> all = (json == null || json.trim().isEmpty())
                ? new ArrayList<>()
                : gson.fromJson(json, listType);

        if (all == null || currentUser == null) return new ArrayList<>();

        String uid = currentUser.getUserId();
        return all.stream()
                .filter(t -> uid.equals(t.userId))
                .sorted((a, b) -> Long.compare(b.timestamp, a.timestamp))
                .collect(java.util.stream.Collectors.toList());
    }


}