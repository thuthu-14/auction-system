package client.ui;

import client.controller.SellerManagementController.AuctionItem;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.VBox;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

public class SellerAuctionTableCellFactory {
    private static final NumberFormat VND_FORMATTER = NumberFormat.getInstance(new Locale("vi", "VN"));

    public TableCell<AuctionItem, String> productCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                AuctionItem row = rowAt(this);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(createProductContent(row));
            }
        };
    }

    public TableCell<AuctionItem, String> priceCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                AuctionItem row = rowAt(this);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(createPriceContent(row));
            }
        };
    }

    public TableCell<AuctionItem, String> statusCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                AuctionItem row = rowAt(this);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Label label = new Label(row.getStatusText());
                label.setStyle(statusStyle(row.getStatusKey()));
                setGraphic(label);
            }
        };
    }

    public TableCell<AuctionItem, Void> actionCell(Consumer<String> openDetails) {
        return new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                AuctionItem row = rowAt(this);
                if (empty || row == null) {
                    setGraphic(null);
                    return;
                }
                Button button = new Button("Chi tiet");
                button.setStyle("-fx-background-color: white; -fx-border-color: #d1d5db; -fx-border-radius: 5; -fx-cursor: hand;");
                button.setOnAction(event -> openDetails.accept(row.getId()));
                setGraphic(button);
            }
        };
    }

    private VBox createProductContent(AuctionItem row) {
        VBox box = new VBox(2);
        Label name = new Label(row.getName());
        name.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
        Label id = new Label("ID: " + row.getId());
        id.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
        box.getChildren().addAll(name, id);
        return box;
    }

    private VBox createPriceContent(AuctionItem row) {
        VBox box = new VBox(2);
        Label current = new Label(formatVnd(row.getCurrentPrice()));
        current.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
        Label start = new Label("Goc: " + formatVnd(row.getStartPrice()));
        start.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
        box.getChildren().addAll(current, start);
        return box;
    }

    private AuctionItem rowAt(TableCell<AuctionItem, ?> cell) {
        if (cell.getTableView() == null || cell.getIndex() < 0 || cell.getIndex() >= cell.getTableView().getItems().size()) {
            return null;
        }
        return cell.getTableView().getItems().get(cell.getIndex());
    }

    private String formatVnd(long amount) {
        return VND_FORMATTER.format(amount) + " d";
    }

    private String statusStyle(String statusKey) {
        String style = "-fx-background-radius: 20; -fx-padding: 3 10; -fx-font-size: 10px; -fx-font-weight: bold;";
        return switch (statusKey) {
            case "active" -> style + "-fx-background-color: #eaf3de; -fx-text-fill: #3b6d11;";
            case "pending" -> style + "-fx-background-color: #e6f1fb; -fx-text-fill: #185fa5;";
            case "ended" -> style + "-fx-background-color: #f3f4f6; -fx-text-fill: #4b5563;";
            case "cancelled" -> style + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;";
            default -> style + "-fx-background-color: #fff7ed; -fx-text-fill: #9a3412;";
        };
    }
}
