package client.controller;

import client.network.ClientSocket;
import client.service.AdminClientService;
import common.UserRole;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import server.model.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserManagementController {

    @FXML private TextField searchUserInput;
    @FXML private Button btnRefresh;
    @FXML private Button btnUnlock;
    @FXML private Button btnLock;
    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, String> colId;
    @FXML private TableColumn<UserRow, String> colUsername;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, String> colStatus;

    private final ObservableList<User> allUsers = FXCollections.observableArrayList();
    private final AdminClientService adminClientService = new AdminClientService();
    private User currentUser;
    private ClientSocket clientSocket;

    @FXML
    public void initialize() {
        setupColumns();
        setupEvents();
    }

    public void setContext(User user, ClientSocket socket) {
        this.currentUser = user;
        this.clientSocket = socket;
        loadUsers();
    }

    private void setupColumns() {
        colId.setCellValueFactory(cell -> cell.getValue().idProperty());
        colUsername.setCellValueFactory(cell -> cell.getValue().usernameProperty());
        colRole.setCellValueFactory(cell -> cell.getValue().roleProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
    }

    private void setupEvents() {
        searchUserInput.textProperty().addListener((observable, oldValue, newValue) -> applySearch());
        btnRefresh.setOnAction(event -> loadUsers());
        btnUnlock.setOnAction(event -> handleUnlockUser());
        btnLock.setOnAction(event -> handleLockUser());
    }

    private void loadUsers() {
        new Thread(() -> {
            try {
                List<User> users = adminClientService.fetchAllUsers(clientSocket, currentUser);
                Platform.runLater(() -> {
                    allUsers.setAll(users);
                    applySearch();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    allUsers.clear();
                    userTable.setItems(FXCollections.observableArrayList());
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách người dùng từ server.");
                });
            }
        }, "AdminLoadUsersThread").start();
    }

    private void applySearch() {
        String keyword = searchUserInput.getText() == null
                ? ""
                : searchUserInput.getText().trim().toLowerCase();

        List<UserRow> rows = allUsers.stream()
                .filter(user -> keyword.isEmpty() || matches(user, keyword))
                .map(UserRow::new)
                .collect(Collectors.toList());

        userTable.setItems(FXCollections.observableArrayList(rows));
    }

    private boolean matches(User user, String keyword) {
        return value(user.getUserId()).toLowerCase().contains(keyword)
                || value(user.getUsername()).toLowerCase().contains(keyword)
                || value(user.getEmail()).toLowerCase().contains(keyword)
                || roleText(user.getRole()).toLowerCase().contains(keyword)
                || statusText(user.isActive()).toLowerCase().contains(keyword);
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    @FXML
    private void handleUnlockUser() {
        updateSelectedUserStatus(true);
    }

    @FXML
    private void handleLockUser() {
        updateSelectedUserStatus(false);
    }

    private void updateSelectedUserStatus(boolean active) {
        UserRow selectedRow = userTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn người dùng", "Hãy chọn một người dùng trong bảng trước.");
            return;
        }

        User user = selectedRow.getUser();
        new Thread(() -> {
            try {
                adminClientService.updateUserStatus(clientSocket, currentUser, user.getUserId(), active);
                Platform.runLater(() -> {
                    user.setActive(active);
                    applySearch();
                    showAlert(
                            Alert.AlertType.INFORMATION,
                            "Thành công",
                            (active ? "Đã mở khóa " : "Đã khóa ") + user.getUsername()
                    );
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật trạng thái người dùng trên server."));
            }
        }, "AdminUpdateUserStatusThread").start();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        client.util.DialogUtil.showAlert(type, title, null, content, userTable);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    private static String roleText(UserRole role) {
        if (role == null) {
            return "-";
        }
        return switch (role) {
            case ADMIN -> "Quản trị viên";
            case SELLER -> "Người bán";
            case BIDDER -> "Người đấu giá";
        };
    }

    private static String statusText(boolean active) {
        return active ? "Đang hoạt động" : "Đã khóa";
    }

    public static class UserRow {
        private final User user;

        public UserRow(User user) {
            this.user = user;
        }

        public User getUser() {
            return user;
        }

        public SimpleStringProperty idProperty() {
            return new SimpleStringProperty(value(user.getUserId()));
        }

        public SimpleStringProperty usernameProperty() {
            String username = value(user.getUsername());
            String email = value(user.getEmail());
            return new SimpleStringProperty(email.isBlank() ? username : username + " (" + email + ")");
        }

        public SimpleStringProperty roleProperty() {
            return new SimpleStringProperty(roleText(user.getRole()));
        }

        public SimpleStringProperty statusProperty() {
            return new SimpleStringProperty(statusText(user.isActive()));
        }
    }
}
