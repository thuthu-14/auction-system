package client.controller;

import client.exception.ClientErrorType;
import client.exception.ClientExceptionHandler;
import client.network.ClientSocket;
import client.network.ConnectionManager;
import client.service.AddAuctionProductClient;
import client.service.AddAuctionProductClientService;
import client.service.AddAuctionProductClientService.CategoryFormConfig;
import common.Message;
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
import java.util.ArrayList;
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
    @FXML
    private ScrollPane formScrollPane;

    private String currentCategory;
    private javafx.collections.ObservableList<String> timeOptions;
    private List<String> selectedImagePaths = new ArrayList<>();
    private final AddAuctionProductClient productService;

    public AddAuctionProductController() {
        this(new AddAuctionProductClientService());
    }

    AddAuctionProductController(AddAuctionProductClient productService) {
        this.productService = productService;
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        categoryComboBox.setItems(FXCollections.observableArrayList(productService.categoryLabels()));
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

                String imagePath = file.getAbsolutePath();

                if (!selectedImagePaths.contains(imagePath)) {
                    selectedImagePaths.add(imagePath);

                    ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                    imageView.setFitWidth(82.0);
                    imageView.setFitHeight(82.0);
                    imageView.setPreserveRatio(true);

                    StackPane imagePane = new StackPane(imageView);
                    imagePane.setPrefSize(84.0, 84.0);
                    imagePane.setMinSize(84.0, 84.0);
                    imagePane.setMaxSize(84.0, 84.0);
                    imagePane.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-cursor: hand;");

                    Button removeButton = new Button("x");
                    removeButton.setFocusTraversable(false);
                    removeButton.setPrefSize(18, 18);
                    removeButton.setMinSize(18, 18);
                    removeButton.setMaxSize(18, 18);
                    removeButton.setStyle("-fx-background-color: rgba(17, 24, 39, 0.82); -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; -fx-background-radius: 9; -fx-padding: 0; -fx-cursor: hand;");
                    StackPane.setAlignment(removeButton, javafx.geometry.Pos.TOP_RIGHT);
                    StackPane.setMargin(removeButton, new javafx.geometry.Insets(4));
                    removeButton.setOnAction(e -> {
                        imagePreviewContainer.getChildren().remove(imagePane);
                        selectedImagePaths.remove(imagePath);
                    });

                    imagePane.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            imagePreviewContainer.getChildren().remove(imagePane);
                            selectedImagePaths.remove(imagePath);
                        }
                    });

                    Tooltip.install(imagePane, new Tooltip("Click đúp chuột để xóa ảnh này"));
                    imagePane.getChildren().add(removeButton);
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
            CategoryFormConfig config = productService.findCategoryForm(selectedCategory);
            Parent formNode = config != null ? FXMLLoader.load(getClass().getResource(config.fxmlPath())) : null;

            if (formNode != null) {
                applyCalendarStylesheet(formNode);
                categoryFormPane.getChildren().add(formNode);
                setupTimeOptions(formNode, config);
            }
        } catch (IOException e) {
            ClientExceptionHandler.showError(ClientErrorType.UI, "Load category form", e, categoryFormPane);
        }
    }

    private void setupTimeOptions(Parent formNode, CategoryFormConfig config) {
        ComboBox<?> startHourCombo = (ComboBox<?>) formNode.lookup(config.startHourId());
        ComboBox<?> endHourCombo = (ComboBox<?>) formNode.lookup(config.endHourId());
        DatePicker startDatePicker = (DatePicker) formNode.lookup(config.startDateId());
        DatePicker endDatePicker = (DatePicker) formNode.lookup(config.endDateId());

        LocalDateTime nextStartDateTime = nextAvailableStartDateTime();
        LocalDate startDate = nextStartDateTime.toLocalDate();
        LocalTime nextStartTime = nextStartDateTime.toLocalTime();
        LocalDate endDate = startDate.plusDays(1);

        if (startDatePicker != null) {
            startDatePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate date, boolean empty) {
                    super.updateItem(date, empty);
                    LocalDate minDate = nextAvailableStartDateTime().toLocalDate();
                    setDisable(empty || date.isBefore(minDate));
                }
            });
            startDatePicker.setValue(startDate);
        }
        if (endDatePicker != null) endDatePicker.setValue(endDate);
        applyCalendarStyles(startDatePicker, endDatePicker);

        if (startHourCombo != null) {
            @SuppressWarnings("unchecked")
            ComboBox<String> comboBox = (ComboBox<String>) startHourCombo;
            comboBox.setItems(startTimeOptionsFor(startDate, nextStartDateTime));
            comboBox.getSelectionModel().select(nextStartTime.toString());
            if (startDatePicker != null) {
                startDatePicker.valueProperty().addListener((obs, oldDate, newDate) ->
                        updateStartTimeOptions(startDatePicker, comboBox));
                comboBox.setOnShowing(event -> updateStartTimeOptions(startDatePicker, comboBox));
            }
        }
        if (endHourCombo != null) {
            @SuppressWarnings("unchecked")
            ComboBox<String> comboBox = (ComboBox<String>) endHourCombo;
            comboBox.setItems(timeOptions);
            comboBox.getSelectionModel().select(nextStartTime.toString());
        }
    }

    private LocalDateTime nextAvailableStartDateTime() {
        LocalDateTime now = LocalDateTime.now();
        int roundedMinute = now.getMinute() < 30 ? 30 : 0;
        int hourOffset = now.getMinute() < 30 ? 0 : 1;
        LocalDateTime candidate = now.withMinute(roundedMinute).withSecond(0).withNano(0).plusHours(hourOffset);
        return candidate.isAfter(now) ? candidate : candidate.plusMinutes(30);
    }

    private void updateStartTimeOptions(DatePicker startDatePicker, ComboBox<String> startHourCombo) {
        if (startDatePicker == null || startHourCombo == null) return;

        LocalDateTime minStart = nextAvailableStartDateTime();
        LocalDate selectedDate = startDatePicker.getValue();
        if (selectedDate == null || selectedDate.isBefore(minStart.toLocalDate())) {
            selectedDate = minStart.toLocalDate();
            startDatePicker.setValue(selectedDate);
        }

        javafx.collections.ObservableList<String> validOptions = startTimeOptionsFor(selectedDate, minStart);
        String selectedTime = startHourCombo.getValue();
        startHourCombo.setItems(validOptions);
        if (selectedTime != null && validOptions.contains(selectedTime)) {
            startHourCombo.getSelectionModel().select(selectedTime);
        } else if (!validOptions.isEmpty()) {
            startHourCombo.getSelectionModel().selectFirst();
        } else {
            startHourCombo.getSelectionModel().clearSelection();
        }
    }

    private javafx.collections.ObservableList<String> startTimeOptionsFor(LocalDate selectedDate, LocalDateTime minStart) {
        javafx.collections.ObservableList<String> options = FXCollections.observableArrayList();
        if (selectedDate == null || selectedDate.isBefore(minStart.toLocalDate())) {
            return options;
        }
        if (selectedDate.isAfter(minStart.toLocalDate())) {
            options.addAll(timeOptions);
            return options;
        }

        LocalTime minTime = minStart.toLocalTime();
        for (String timeOption : timeOptions) {
            if (!LocalTime.parse(timeOption).isBefore(minTime)) {
                options.add(timeOption);
            }
        }
        return options;
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
            ClientExceptionHandler.handle(ClientErrorType.UI, "Missing calendar stylesheet", new IllegalStateException("/CSS/calendar.css"));
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
        handleSaveAndPublishViaService();
    }

    private void handleSaveAndPublishViaService() {
        User currentUser = NavigationManager.getInstance().getCurrentUser();
        ClientSocket socket = ConnectionManager.getInstance().getClientSocket();

        if (currentUser == null || socket == null || !socket.isConnected()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Bạn chưa đăng nhập hoặc mất kết nối máy chủ!");
            return;
        }

        try {
            Map<String, Object> payload = productService.buildPayload(
                    socket,
                    currentUser,
                    productNameField.getText(),
                    descriptionArea.getText(),
                    categoryComboBox.getValue(),
                    selectedImagePaths,
                    new AddAuctionProductClientService.CategoryFieldReader() {
                        @Override
                        public String fieldValue(String fieldId, String defaultValue) {
                            return getFieldValue(fieldId, defaultValue);
                        }

                        @Override
                        public LocalDate dateValue(String fieldId) {
                            return getDateValue(fieldId);
                        }

                        @Override
                        public String comboValue(String fieldId) {
                            return getComboValue(fieldId);
                        }

                    });

            client.util.ClientTaskRunner.run(() -> {
                try {
                    Message response = productService.publish(socket, currentUser, payload);
                    Platform.runLater(() -> {
                        if (response != null && "SUCCESS".equals(response.getStatus())) {
                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Phiên đấu giá đã được tạo thành công!");
                            clearForm();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Thất bại", "Server từ chối: " + (response != null ? response.getMessage() : ""));
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Mất kết nối tới Server."));
                    ClientExceptionHandler.handle(ClientErrorType.NETWORK, "Publish auction product", e);
                }
            });
        } catch (AddAuctionProductClientService.ValidationException e) {
            showAlert(Alert.AlertType.ERROR, e.getTitle(), e.getMessage());
        } catch (Exception e) {
            ClientExceptionHandler.handle(ClientErrorType.UI, "Build auction product payload", e);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Lỗi: " + e.getMessage());
        }
    }

    private void clearForm() {
        productNameField.clear();
        descriptionArea.clear();
        currentCategory = null;
        categoryComboBox.getSelectionModel().clearSelection();
        resetCategoryPlaceholder();

        if (imagePreviewContainer != null) {
            imagePreviewContainer.getChildren().clear();
        }
        selectedImagePaths.clear();

        Platform.runLater(() -> {
            if (formScrollPane != null) {
                formScrollPane.setVvalue(0);
            }
            productNameField.requestFocus();
        });
    }

    private void resetCategoryPlaceholder() {
        if (categoryFormPane == null) return;

        categoryFormPane.getChildren().clear();

        VBox placeholder = new VBox(10);
        placeholder.setAlignment(javafx.geometry.Pos.CENTER);
        placeholder.setPadding(new javafx.geometry.Insets(20));

        Label label = new Label("Ch\u1ecdn ng\u00e0nh h\u00e0ng \u0111\u1ec3 b\u1eaft \u0111\u1ea7u");
        label.setTextFill(javafx.scene.paint.Color.web("#a0aec0"));
        label.getStyleClass().add("field-label");
        label.setFont(javafx.scene.text.Font.font(14));

        placeholder.getChildren().add(label);
        categoryFormPane.getChildren().add(placeholder);
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
        client.util.DialogUtil.showAlert(type, title, null, msg, productNameField);
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

    private LocalDate getDateValue(String fieldId) {
        Node node = lookupCategoryNode(fieldId);
        return node instanceof DatePicker datePicker ? datePicker.getValue() : null;
    }

    private String getComboValue(String fieldId) {
        Node node = lookupCategoryNode(fieldId);
        return node instanceof ComboBox<?> comboBox && comboBox.getValue() != null
                ? comboBox.getValue().toString()
                : null;
    }

    private Node lookupCategoryNode(String fieldId) {
        if (categoryFormPane != null && !categoryFormPane.getChildren().isEmpty()) {
            Node node = categoryFormPane.getChildren().get(0);
            if (node instanceof Parent form) {
                return form.lookup(fieldId);
            }
        }
        return null;
    }
}
