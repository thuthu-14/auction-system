package client.ui;

import client.logic.WalletLogic;
import common.Transaction;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Window;

public class WalletTransactionDialogFactory {

    private final WalletLogic walletLogic;

    public WalletTransactionDialogFactory(WalletLogic walletLogic) {
        this.walletLogic = walletLogic;
    }

    public void show(Transaction transaction, Window owner) {
        if (transaction == null) {
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết giao dịch");
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CLOSE);
        pane.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-border-color: #e5e7eb; -fx-border-radius: 14;"
                + " -fx-padding: 0 0 14 0;");

        pane.setContent(createContent(transaction));
        styleCloseButton(pane);
        dialog.showAndWait();
    }

    public VBox createContent(Transaction transaction) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(18, 22, 18, 22));
        content.setPrefWidth(520);

        Label title = new Label(transaction.getTypeLabel());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label amount = new Label(walletLogic.formatSignedTransactionAmount(transaction));
        amount.setAlignment(Pos.CENTER_LEFT);
        amount.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: "
                + (walletLogic.isMoneyIn(transaction) ? "#047857" : "#dc2626") + ";");

        content.getChildren().addAll(title, amount, createDetailGrid(transaction));
        return content;
    }

    public GridPane createDetailGrid(Transaction transaction) {
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
        return detailGrid;
    }

    public void addTransactionDetailRow(GridPane grid, int rowIndex, String labelText, String valueText) {
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

    private void styleCloseButton(DialogPane pane) {
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

    private String safeText(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }
}
