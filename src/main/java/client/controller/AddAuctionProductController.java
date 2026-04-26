package client.controller;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AddAuctionProductController implements Initializable {

    @FXML private TextField productNameField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField startPriceField;
    @FXML private TextField reservePriceField;
    @FXML private TextField stepPriceField;

    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startHourCombo;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endHourCombo;

    @FXML private TextArea descriptionArea;
    @FXML private Label stepPriceHintLabel;
    @FXML private Label timeHintLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Đồ điện tử", "Thời trang", "Sưu tầm", "Đồ gia dụng", "Khác"
        ));

        startHourCombo.setItems(generateTimeOptions());
        endHourCombo.setItems(generateTimeOptions());

        startDatePicker.setValue(LocalDate.now());
        startHourCombo.getSelectionModel().select("08:00");
        setupPriceFormatter(startPriceField);
        setupPriceFormatter(reservePriceField);
        setupPriceFormatter(stepPriceField);
    }

    @FXML
    public void handleCategoryChange(ActionEvent event) {
        String selectedCategory = categoryComboBox.getValue();
        if (selectedCategory == null) return;

        if (stepPriceHintLabel != null && timeHintLabel != null) {
            stepPriceHintLabel.setStyle("-fx-text-fill: #2563eb;");
            timeHintLabel.setStyle("-fx-text-fill: #2563eb;");

            switch (selectedCategory) {
                case "Đồ điện tử":
                    stepPriceHintLabel.setText("↳ Bước giá tối thiểu: 50,000 đ");
                    timeHintLabel.setText("↳ Thời gian: 1 - 7 ngày");
                    stepPriceField.setText("50000"); 
                    break;
                case "Thời trang":
                    stepPriceHintLabel.setText("↳ Bước giá tối thiểu: 10,000 đ");
                    timeHintLabel.setText("↳ Thời gian: 1 - 3 ngày");
                    stepPriceField.setText("10000");
                    break;
                case "Sưu tầm":
                    stepPriceHintLabel.setText("↳ Bước giá tối thiểu: 100,000 đ");
                    timeHintLabel.setText("↳ Thời gian: 1 - 14 ngày");
                    stepPriceField.setText("100000");
                    break;
                case "Đồ gia dụng":
                    stepPriceHintLabel.setText("↳ Bước giá tối thiểu: 20,000 đ");
                    timeHintLabel.setText("↳ Thời gian: 1 - 5 ngày");
                    stepPriceField.setText("20000");
                    break;
                default:
                    stepPriceHintLabel.setText("↳ Tùy chọn bước giá");
                    timeHintLabel.setText("↳ Tùy chọn thời gian");
                    stepPriceField.clear();
                    break;
            }
        }
    }

    private javafx.collections.ObservableList<String> generateTimeOptions() {
        javafx.collections.ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 0; h < 24; h++) {
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        return times;
    }

    private void setupPriceFormatter(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.matches("\\d*")) return; // Chỉ cho nhập số
            textField.setText(newValue.replaceAll("[^\\d]", ""));
        });
    }

    @FXML
    private void handleSaveAndPublish(ActionEvent event) {
        if (productNameField.getText().isEmpty() || startPriceField.getText().isEmpty() ||
                stepPriceField.getText().isEmpty() || startDatePicker.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các trường bắt buộc (*)");
            return;
        }

        try {
            String name = productNameField.getText();
            double startPrice = Double.parseDouble(startPriceField.getText());
            double stepPrice = Double.parseDouble(stepPriceField.getText());

            // Nếu không nhập giá mong muốn, mặc định nó bằng với giá khởi điểm
            double reservePrice = startPrice;
            if (!reservePriceField.getText().isEmpty()) {
                reservePrice = Double.parseDouble(reservePriceField.getText());
            }

            // Chặn Giá khởi điểm lớn hơn Giá mong muốn
            if (startPrice > reservePrice) {
                showAlert(Alert.AlertType.ERROR, "Lỗi đặt giá", "Giá khởi điểm không được lớn hơn Giá mong muốn!");
                return;
            }

            String startTime = startDatePicker.getValue().toString() + " " + startHourCombo.getValue();

            System.out.println("Đang tạo phiên đấu giá: " + name
                    + " | Khởi điểm: " + startPrice
                    + " | Mong muốn: " + reservePrice
                    + " | Bước giá: " + stepPrice
                    + " | Bắt đầu: " + startTime);

            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã lưu và hiển thị sản phẩm đấu giá!");

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", "Giá trị nhập vào phải là số hợp lệ!");
        }
    }

    @FXML
    private void handleSaveHidden(ActionEvent event) {
        System.out.println("Lưu nháp sản phẩm...");
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        System.out.println("Hủy thao tác...");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}