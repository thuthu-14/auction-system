package client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TableView;

public class ManageAuctionsController {

    @FXML
    private TableView<?> auctionTable;

    @FXML
    private void handleCreateAuction() {
        System.out.println("Tạo phiên mới");
    }

    @FXML
    private void handleDeleteAuction() {
        System.out.println("Xóa phiên");
    }

    @FXML
    private void handleViewHistory() {
        System.out.println("Xem bid history");
    }

    @FXML
    private void handleRefresh() {
        System.out.println("Refresh danh sách");
    }
}