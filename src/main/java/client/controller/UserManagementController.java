package client.controller;


import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
// import io.auctionsystem.models.User;

public class UserManagementController {

    @FXML
    private TextField searchUserInput;

    @FXML
    private Button btnRefresh;

    @FXML
    private Button btnUnlock;

    @FXML
    private Button btnLock;

    @FXML
    private TableView<Object> userTable; // Thay Object bằng class User

    @FXML
    private TableColumn<Object, String> colId;

    @FXML
    private TableColumn<Object, String> colUsername;

    @FXML
    private TableColumn<Object, String> colRole;

    @FXML
    private TableColumn<Object, String> colStatus;

    @FXML
    public void initialize() {
        // 1. Cấu hình cách đổ dữ liệu vào từng cột
        // colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        // colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        // colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        // colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        searchUserInput.textProperty().addListener((observable, oldValue, newValue) -> {
            handleSearch(newValue);
        });

        System.out.println("User Management Controller initialized!");
    }

    @FXML
    private void handleRefresh() {
        System.out.println("Đang làm mới danh sách người dùng...");
        // Code load lại data từ Database ở đây
    }

    @FXML
    private void handleUnlockUser() {
        Object selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            System.out.println("Đang mở khóa user: " + selectedUser);
        } else {
            System.out.println("Bạn chưa chọn user nào để mở khóa!");
        }
    }

    @FXML
    private void handleLockUser() {
        Object selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            System.out.println("Đang khóa user: " + selectedUser);
        } else {
            System.out.println("Chọn 1 user để khóa đã nhé!");
        }
    }

    private void handleSearch(String searchText) {
        System.out.println("Đang tìm kiếm: " + searchText);
    }
}