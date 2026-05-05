package client.controller;

import client.network.ClientSocket;
import client.network.ConnectionManager;
import common.Message;
import common.MessageType;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import navigation.NavigationManager;
import server.model.User;
import util.LoggerUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class AddAuctionProductController implements Initializable {

    @FXML
    private TextField productNameField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private VBox categoryFormPane;
    @FXML
    private HBox imagePreviewContainer;

    private String currentCategory;
    private javafx.collections.ObservableList<String> timeOptions;
    private List<String> selectedImagePaths = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Đồ điện tử", "Thời trang", "Trang sức", "Sưu tầm", "Xe cộ"
        ));

        timeOptions = generateTimeOptions();
    }

    @FXML
    public void handleSelectImage(ActionEvent event) {
        if (selectedImagePaths.size() >= 5) {
            showAlert(Alert.AlertType.WARNING, "Giới hạn ảnh", "Chỉ được chọn tối đa 5 ảnh thôi nha!");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm (Tối đa 5 ảnh)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        // Lấy cửa sổ an toàn từ nút bấm kích hoạt sự kiện
        Window window = ((Node) event.getSource()).getScene().getWindow();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(window);

        if (selectedFiles != null) {
            for (File file : selectedFiles) {
                if (selectedImagePaths.size() >= 5) {
                    showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Đã đạt giới hạn 5 ảnh, các ảnh dư sẽ bị bỏ qua.");
                    break;
                }

                String imagePath = file.toURI().toString();

                if (!selectedImagePaths.contains(imagePath)) {
                    selectedImagePaths.add(imagePath);

                    ImageView imageView = new ImageView(new Image(imagePath));
                    imageView.setFitWidth(98.0);
                    imageView.setFitHeight(98.0);
                    imageView.setPreserveRatio(true);

                    StackPane imagePane = new StackPane(imageView);
                    imagePane.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-cursor: hand;");

                    imagePane.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            imagePreviewContainer.getChildren().remove(imagePane);
                            selectedImagePaths.remove(imagePath);
                        }
                    });

                    Tooltip.install(imagePane, new Tooltip("Click đúp chuột để xóa ảnh này"));
                    imagePreviewContainer.getChildren().add(imagePane);
                }
            }
        }
    }

    @FXML
    public void handleCategoryChange(ActionEvent event) {
        String selectedCategory = categoryComboBox.getValue();
        if (selectedCategory == null) return;

        currentCategory = selectedCategory;
        categoryFormPane.getChildren().clear();

        try {
            Parent formNode = null;

            switch (selectedCategory) {
                case "Đồ điện tử":
                    formNode = FXMLLoader.load(getClass().getResource("/fxml/AddAuctionProduct_Electronics.fxml"));
                    break;
                case "Thời trang":
                    formNode = FXMLLoader.load(getClass().getResource("/fxml/AddAuctionProduct_Fashion.fxml"));
                    break;
                case "Trang sức":
                    formNode = FXMLLoader.load(getClass().getResource("/fxml/AddAuctionProduct_Jewelry.fxml"));
                    break;
                case "Sưu tầm":
                    formNode = FXMLLoader.load(getClass().getResource("/fxml/AddAuctionProduct_Art.fxml"));
                    break;
                case "Xe cộ":
                    formNode = FXMLLoader.load(getClass().getResource("/fxml/AddAuctionProduct_Vehicle.fxml"));
                    break;
            }

            if (formNode != null) {
                applyCalendarStylesheet(formNode);
                categoryFormPane.getChildren().add(formNode);
                setupTimeOptions(formNode);
            }
        } catch (IOException e) {
            LoggerUtil.error("Lỗi tải form danh mục: " + e.getMessage());
        }
    }

    private void setupTimeOptions(Parent formNode) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        ComboBox<?> startHourCombo = null;
        ComboBox<?> endHourCombo = null;
        DatePicker startDatePicker = null;
        DatePicker endDatePicker = null;

        switch (currentCategory) {
            case "Đồ điện tử":
                startHourCombo = (ComboBox<?>) formNode.lookup("#elecStartHourCombo");
                endHourCombo = (ComboBox<?>) formNode.lookup("#elecEndHourCombo");
                startDatePicker = (DatePicker) formNode.lookup("#elecStartDatePicker");
                endDatePicker = (DatePicker) formNode.lookup("#elecEndDatePicker");
                break;
            case "Thời trang":
                startHourCombo = (ComboBox<?>) formNode.lookup("#fashionStartHourCombo");
                endHourCombo = (ComboBox<?>) formNode.lookup("#fashionEndHourCombo");
                startDatePicker = (DatePicker) formNode.lookup("#fashionStartDatePicker");
                endDatePicker = (DatePicker) formNode.lookup("#fashionEndDatePicker");
                break;
            case "Trang sức":
                startHourCombo = (ComboBox<?>) formNode.lookup("#jewelryStartHourCombo");
                endHourCombo = (ComboBox<?>) formNode.lookup("#jewelryEndHourCombo");
                startDatePicker = (DatePicker) formNode.lookup("#jewelryStartDatePicker");
                endDatePicker = (DatePicker) formNode.lookup("#jewelryEndDatePicker");
                break;
            case "Sưu tầm":
                startHourCombo = (ComboBox<?>) formNode.lookup("#artStartHourCombo");
                endHourCombo = (ComboBox<?>) formNode.lookup("#artEndHourCombo");
                startDatePicker = (DatePicker) formNode.lookup("#artStartDatePicker");
                endDatePicker = (DatePicker) formNode.lookup("#artEndDatePicker");
                break;
            case "Xe cộ":
                startHourCombo = (ComboBox<?>) formNode.lookup("#vehicleStartHourCombo");
                endHourCombo = (ComboBox<?>) formNode.lookup("#vehicleEndHourCombo");
                startDatePicker = (DatePicker) formNode.lookup("#vehicleStartDatePicker");
                endDatePicker = (DatePicker) formNode.lookup("#vehicleEndDatePicker");
                break;
        }

        if (startDatePicker != null) startDatePicker.setValue(today);
        if (endDatePicker != null) endDatePicker.setValue(tomorrow);
        applyCalendarStyles(startDatePicker, endDatePicker);

        if (startHourCombo != null) {
            @SuppressWarnings("unchecked") ComboBox<String> comboBox = (ComboBox<String>) startHourCombo;
            comboBox.setItems(timeOptions);
            comboBox.getSelectionModel().select("08:00");
        }
        if (endHourCombo != null) {
            @SuppressWarnings("unchecked") ComboBox<String> comboBox = (ComboBox<String>) endHourCombo;
            comboBox.setItems(timeOptions);
            comboBox.getSelectionModel().select("17:00");
        }
    }

    private void applyCalendarStyles(DatePicker... datePickers) {
        String calendarCss = getCalendarStylesheet();
        if (calendarCss == null) return;

        for (DatePicker datePicker : datePickers) {
            if (datePicker != null && !datePicker.getStylesheets().contains(calendarCss)) {
                datePicker.getStylesheets().add(calendarCss);
            }
            if (datePicker != null) {
                datePicker.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null && !newScene.getStylesheets().contains(calendarCss)) {
                        newScene.getStylesheets().add(calendarCss);
                    }
                });
                datePicker.setOnShowing(event -> {
                    if (datePicker.getScene() != null && !datePicker.getScene().getStylesheets().contains(calendarCss)) {
                        datePicker.getScene().getStylesheets().add(calendarCss);
                    }
                });
            }
        }
    }

    private void applyCalendarStylesheet(Parent formNode) {
        String calendarCss = getCalendarStylesheet();
        if (calendarCss != null && !formNode.getStylesheets().contains(calendarCss)) {
            formNode.getStylesheets().add(calendarCss);
        }
        Platform.runLater(() -> {
            if (calendarCss != null && formNode.getScene() != null && !formNode.getScene().getStylesheets().contains(calendarCss)) {
                formNode.getScene().getStylesheets().add(calendarCss);
            }
        });
    }

    private String getCalendarStylesheet() {
        URL calendarCssUrl = getClass().getResource("/CSS/calendar.css");
        if (calendarCssUrl == null) {
            LoggerUtil.error("Không tìm thấy CSS lịch: /CSS/calendar.css");
            return null;
        }
        return calendarCssUrl.toExternalForm();
    }

    private javafx.collections.ObservableList<String> generateTimeOptions() {
        javafx.collections.ObservableList<String> times = FXCollections.observableArrayList();
        for (int h = 0; h < 24; h++) {
            times.add(String.format("%02d:00", h));
            times.add(String.format("%02d:30", h));
        }
        return times;
    }

    @FXML
    private void handleSaveAndPublish(ActionEvent event) {
        if (productNameField.getText().isEmpty() || descriptionArea.getText().isEmpty() || categoryComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ tên sản phẩm, mô tả và chọn ngành hàng!");
            return;
        }

        if (selectedImagePaths.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu ảnh", "Vui lòng chọn ít nhất 1 ảnh cho sản phẩm!");
            return;
        }

        User currentUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();

        if (currentUser == null || socket == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Bạn chưa đăng nhập hoặc mất kết nối máy chủ!");
            return;
        }

        try {
            String name = productNameField.getText().trim();
            String description = descriptionArea.getText().trim();
            String category = categoryComboBox.getValue();

            Map<String, Object> payload = new HashMap<>();
            // THÊM 2 DÒNG NÀY ĐỂ SERVER KHÔNG BỊ NULL
            payload.put("itemId", "");
            payload.put("sellerId", currentUser.getUsername());

            payload.put("name", name);
            payload.put("description", description);
            payload.put("images", new ArrayList<>(selectedImagePaths));

            double startPrice = 0;
            int durationMinutes = 0;

            switch (category) {
                case "Đồ điện tử":
                    payload.put("type", "ELECTRONICS"); // SỬA THÀNH CHỮ THƯỜNG
                    startPrice = validateAndParsePrice(getFieldValue("#elecStartPriceField", ""), "Giá khởi điểm", 50000.0);
                    if (startPrice < 0) return;
                    durationMinutes = (int) calculateDuration("#elecStartDatePicker", "#elecStartHourCombo", "#elecEndDatePicker", "#elecEndHourCombo");
                    if (durationMinutes < 1440 || durationMinutes > 10080) { showAlert(Alert.AlertType.ERROR, "Lỗi thời gian", "Thời gian phải từ 1-7 ngày"); return; }
                    payload.put("brand", getFieldValue("#elecBrandField", ""));
                    payload.put("warrantyPeriod", getFieldValue("#elecWarrantyField", "")); // Sửa warranty thành warrantyPeriod
                    break;

                case "Thời trang":
                    payload.put("type", "FASHION"); // SỬA THÀNH CHỮ THƯỜNG
                    startPrice = validateAndParsePrice(getFieldValue("#fashionStartPriceField", ""), "Giá khởi điểm", 10000.0);
                    if (startPrice < 0) return;
                    durationMinutes = (int) calculateDuration("#fashionStartDatePicker", "#fashionStartHourCombo", "#fashionEndDatePicker", "#fashionEndHourCombo");
                    if (durationMinutes < 1440 || durationMinutes > 4320) { showAlert(Alert.AlertType.ERROR, "Lỗi thời gian", "Thời gian phải từ 1-3 ngày"); return; }
                    payload.put("brand", getFieldValue("#fashionBrandField", ""));
                    payload.put("material", getFieldValue("#fashionMaterialField", ""));
                    break;

                case "Trang sức":
                    payload.put("type", "JEWELRY"); // SỬA THÀNH CHỮ THƯỜNG
                    startPrice = validateAndParsePrice(getFieldValue("#jewelryStartPriceField", ""), "Giá khởi điểm", 25000.0);
                    if (startPrice < 0) return;
                    durationMinutes = (int) calculateDuration("#jewelryStartDatePicker", "#jewelryStartHourCombo", "#jewelryEndDatePicker", "#jewelryEndHourCombo");
                    if (durationMinutes < 1440 || durationMinutes > 7200) { showAlert(Alert.AlertType.ERROR, "Lỗi thời gian", "Thời gian phải từ 1-5 ngày"); return; }
                    payload.put("material", getFieldValue("#jewelryMaterialField", ""));
                    double jewelryWeight = validateAndParseDouble(getFieldValue("#jewelryWeightField", ""), "Trọng lượng", 0.1, 100000);
                    if (jewelryWeight < 0) return;
                    payload.put("weight", jewelryWeight);
                    break;

                case "Sưu tầm":
                    payload.put("type", "ART"); // SỬA THÀNH CHỮ THƯỜNG
                    startPrice = validateAndParsePrice(getFieldValue("#artStartPriceField", ""), "Giá khởi điểm", 100000.0);
                    if (startPrice < 0) return;
                    durationMinutes = (int) calculateDuration("#artStartDatePicker", "#artStartHourCombo", "#artEndDatePicker", "#artEndHourCombo");
                    if (durationMinutes < 1440 || durationMinutes > 20160) { showAlert(Alert.AlertType.ERROR, "Lỗi thời gian", "Thời gian phải từ 1-14 ngày"); return; }
                    payload.put("creator", getFieldValue("#artCreatorField", ""));
                    payload.put("material", getFieldValue("#artMaterialField", ""));
                    break;

                case "Xe cộ":
                    payload.put("type", "VEHICLE"); // SỬA THÀNH CHỮ THƯỜNG
                    startPrice = validateAndParsePrice(getFieldValue("#vehicleStartPriceField", ""), "Giá khởi điểm", 500000.0);
                    if (startPrice < 0) return;
                    durationMinutes = (int) calculateDuration("#vehicleStartDatePicker", "#vehicleStartHourCombo", "#vehicleEndDatePicker", "#vehicleEndHourCombo");
                    if (durationMinutes < 1440 || durationMinutes > 10080) { showAlert(Alert.AlertType.ERROR, "Lỗi thời gian", "Thời gian phải từ 1-7 ngày"); return; }
                    int odometer = validateAndParseInt(getFieldValue("#vehicleOdometerField", ""), "Số km", 0, 10000000);
                    if (odometer < 0) return;
                    payload.put("model", getFieldValue("#vehicleYearField", ""));
                    payload.put("odometer", odometer);
                    break;

                default:
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Category không hợp lệ!");
                    return;
            }

            if (durationMinutes <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Thời gian kết thúc phải sau thời gian bắt đầu!");
                return;
            }

            payload.put("startingPrice", startPrice); // SỬA TỪ price THÀNH startingPrice
            payload.put("duration", durationMinutes);

            // Gửi dữ liệu cho Server (ĐÃ SỬA CÁCH TẠO MESSAGE ĐỂ KHÔNG BỊ NULL DATA)
            new Thread(() -> {
                try {
                    Message request = new Message();
                    request.setType(MessageType.CREATE_SELLER_ITEM);
                    request.setData(payload); // Gán data rõ ràng
                    request.setSenderId(currentUser.getUsername());

                    socket.sendMessage(request);
                    Message response = socket.receiveMessage();

                    Platform.runLater(() -> {
                        if (response != null && "SUCCESS".equals(response.getStatus())) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Sản phẩm đã được đẩy lên Sàn Đấu Giá!");
                            clearForm();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Thất bại", "Server từ chối: " + (response != null ? response.getMessage() : ""));
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Mất kết nối tới Server."));
                    LoggerUtil.error("Lỗi khi tạo sản phẩm đấu giá: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            LoggerUtil.error("Lỗi xử lý UI: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi: " + e.getMessage());
        }
    }

    private void clearForm() {
        productNameField.clear();
        descriptionArea.clear();
        categoryComboBox.setValue(null);
        categoryFormPane.getChildren().clear();

        if (imagePreviewContainer != null) {
            imagePreviewContainer.getChildren().clear();
        }
        selectedImagePaths.clear();
    }

    @FXML
    private void handleSaveHidden(ActionEvent event) {
        LoggerUtil.info("Lưu nháp sản phẩm...");
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        LoggerUtil.info("Hủy thao tác...");
        clearForm();
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String getFieldValue(String fieldId, String defaultValue) {
        if (categoryFormPane != null && !categoryFormPane.getChildren().isEmpty()) {
            Node node = categoryFormPane.getChildren().get(0);
            if (node instanceof Parent) {
                Parent form = (Parent) node;
                TextField field = (TextField) form.lookup(fieldId);
                if (field != null && !field.getText().isEmpty()) {
                    return field.getText();
                }
            }
        }
        return defaultValue;
    }

    private double validateAndParsePrice(String value, String fieldName, double minValue) {
        if (value == null || value.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Vui lòng nhập " + fieldName + "!");
            return -1;
        }
        try {
            double price = Double.parseDouble(value);
            if (price < minValue) {
                showAlert(Alert.AlertType.ERROR, "Lỗi giá", fieldName + " phải tối thiểu " + minValue);
                return -1;
            }
            return price;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", fieldName + " phải là số!");
            return -1;
        }
    }

    private double validateAndParseDouble(String value, String fieldName, double minValue, double maxValue) {
        if (value == null || value.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Vui lòng nhập " + fieldName + "!");
            return -1;
        }
        try {
            double val = Double.parseDouble(value);
            if (val < minValue || val > maxValue) {
                showAlert(Alert.AlertType.ERROR, "Lỗi giá trị", fieldName + " phải từ " + minValue + " đến " + maxValue);
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", fieldName + " phải là số!");
            return -1;
        }
    }

    private int validateAndParseInt(String value, String fieldName, int minValue, int maxValue) {
        if (value == null || value.trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Thiếu thông tin", "Vui lòng nhập " + fieldName + "!");
            return -1;
        }
        try {
            int val = Integer.parseInt(value);
            if (val < minValue || val > maxValue) {
                showAlert(Alert.AlertType.ERROR, "Lỗi giá trị", fieldName + " phải từ " + minValue + " đến " + maxValue);
                return -1;
            }
            return val;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi định dạng", fieldName + " phải là số!");
            return -1;
        }
    }

    private long calculateDuration(String startDateId, String startHourId, String endDateId, String endHourId) {
        try {
            if (categoryFormPane != null && !categoryFormPane.getChildren().isEmpty()) {
                Node node = categoryFormPane.getChildren().get(0);
                if (node instanceof Parent) {
                    Parent form = (Parent) node;
                    DatePicker startDatePicker = (DatePicker) form.lookup(startDateId);
                    ComboBox<String> startHourCombo = (ComboBox<String>) form.lookup(startHourId);
                    DatePicker endDatePicker = (DatePicker) form.lookup(endDateId);
                    ComboBox<String> endHourCombo = (ComboBox<String>) form.lookup(endHourId);

                    if (startDatePicker == null || startHourCombo == null || endDatePicker == null || endHourCombo == null)
                        return 1440;

                    LocalDate startDate = startDatePicker.getValue();
                    String startHourStr = startHourCombo.getValue();
                    LocalDate endDate = endDatePicker.getValue();
                    String endHourStr = endHourCombo.getValue();

                    if (startDate == null || startHourStr == null || endDate == null || endHourStr == null) return 1440;

                    LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.parse(startHourStr));
                    LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.parse(endHourStr));

                    return Duration.between(startTime, endTime).toMinutes();
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("Lỗi tính thời gian: " + e.getMessage());
        }
        return 1440;
    }
}
