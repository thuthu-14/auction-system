package client.service;

import client.network.MessageTransport;
import common.Message;
import common.MessageType;
import server.model.User;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddAuctionProductClientService {
    @FunctionalInterface
    private interface CategoryPayloadBuilder {
        CategoryBuildResult build(Map<String, Object> payload, CategoryFieldReader fields);
    }

    public record CategoryFormConfig(
            String label,
            String fxmlPath,
            String startDateId,
            String startHourId,
            String endDateId,
            String endHourId
    ) {
    }

    public interface CategoryFieldReader {
        String fieldValue(String fieldId, String defaultValue);

        LocalDate dateValue(String fieldId);

        String comboValue(String fieldId);
    }

    private final List<CategoryFormConfig> categoryForms = List.of(
            new CategoryFormConfig("\u0110\u1ed3 \u0111i\u1ec7n t\u1eed", "/fxml/SellerView/AddAuctionProduct_Electronics.fxml", "#elecStartDatePicker", "#elecStartHourCombo", "#elecEndDatePicker", "#elecEndHourCombo"),
            new CategoryFormConfig("Th\u1eddi trang", "/fxml/SellerView/AddAuctionProduct_Fashion.fxml", "#fashionStartDatePicker", "#fashionStartHourCombo", "#fashionEndDatePicker", "#fashionEndHourCombo"),
            new CategoryFormConfig("Trang s\u1ee9c", "/fxml/SellerView/AddAuctionProduct_Jewelry.fxml", "#jewelryStartDatePicker", "#jewelryStartHourCombo", "#jewelryEndDatePicker", "#jewelryEndHourCombo"),
            new CategoryFormConfig("S\u01b0u t\u1ea7m", "/fxml/SellerView/AddAuctionProduct_Art.fxml", "#artStartDatePicker", "#artStartHourCombo", "#artEndDatePicker", "#artEndHourCombo"),
            new CategoryFormConfig("Xe c\u1ed9", "/fxml/SellerView/AddAuctionProduct_Vehicle.fxml", "#vehicleStartDatePicker", "#vehicleStartHourCombo", "#vehicleEndDatePicker", "#vehicleEndHourCombo"),
            new CategoryFormConfig("Kh\u00e1c", "/fxml/SellerView/AddAuctionProduct_Other.fxml", "#otherStartDatePicker", "#otherStartHourCombo", "#otherEndDatePicker", "#otherEndHourCombo")
    );

    private final Map<String, CategoryPayloadBuilder> categoryBuilders = Map.of(
            normalize("\u0110\u1ed3 \u0111i\u1ec7n t\u1eed"), this::buildElectronics,
            normalize("Th\u1eddi trang"), this::buildFashion,
            normalize("Trang s\u1ee9c"), this::buildJewelry,
            normalize("S\u01b0u t\u1ea7m"), this::buildArt,
            normalize("Xe c\u1ed9"), this::buildVehicle,
            normalize("Kh\u00e1c"), this::buildOther
    );

    public List<String> categoryLabels() {
        return categoryForms.stream().map(CategoryFormConfig::label).toList();
    }

    public CategoryFormConfig findCategoryForm(String category) {
        String normalizedCategory = normalize(category);
        return categoryForms.stream()
                .filter(config -> normalize(config.label()).equals(normalizedCategory))
                .findFirst()
                .orElse(null);
    }

    public Map<String, Object> buildPayload(User user, String name, String description, String category,
                                            List<String> imagePaths, CategoryFieldReader fields) {
        validateBase(name, description, category, imagePaths);
        if (user == null) {
            throw new ValidationException("Lỗi mạng", "Bạn chưa đăng nhập hoặc mất kết nối máy chủ!");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("itemId", "");
        payload.put("sellerId", user.getUsername());
        payload.put("name", name.trim());
        payload.put("description", description.trim());
        payload.put("images", new ArrayList<>(imagePaths));

        CategoryBuildResult categoryResult = buildCategoryPayload(category, payload, fields);

        if (categoryResult.durationMinutes <= 0) {
            throw new ValidationException("Lỗi", "Thời gian kết thúc phải sau thời gian bắt đầu!");
        }
        payload.put("startingPrice", categoryResult.startPrice);
        payload.put("duration", categoryResult.durationMinutes);
        return payload;
    }

    private CategoryBuildResult buildCategoryPayload(String category, Map<String, Object> payload, CategoryFieldReader fields) {
        CategoryPayloadBuilder builder = categoryBuilders.get(normalize(category));
        if (builder != null) {
            return builder.build(payload, fields);
        }
        throw new ValidationException("Lỗi", "Danh mục không hợp lệ!");
    }

    public Message publish(MessageTransport transport, User user, Map<String, Object> payload) throws Exception {
        if (transport == null || !transport.isConnected() || user == null) {
            throw new java.io.IOException("Socket is not connected");
        }

        Message request = new Message();
        request.setType(MessageType.CREATE_SELLER_ITEM);
        request.setData(payload);
        request.setSenderId(user.getUsername());
        return transport.sendAndReceive(request);
    }

    private void validateBase(String name, String description, String category, List<String> imagePaths) {
        if (name == null || name.trim().isEmpty() || description == null || description.trim().isEmpty() || category == null) {
            throw new ValidationException("Thiếu thông tin", "Vui lòng nhập đầy đủ tên sản phẩm, mô tả và chọn ngành hàng!");
        }
        if (imagePaths == null || imagePaths.isEmpty()) {
            throw new ValidationException("Thiếu ảnh", "Vui lòng chọn ít nhất 1 ảnh cho sản phẩm!");
        }
    }

    private CategoryBuildResult buildElectronics(Map<String, Object> payload, CategoryFieldReader fields) {
        payload.put("type", "ELECTRONICS");
        double startPrice = parsePrice(fields.fieldValue("#elecStartPriceField", ""), "Giá khởi điểm", 50000.0);
        int duration = checkedDuration(fields, "#elecStartDatePicker", "#elecStartHourCombo", "#elecEndDatePicker", "#elecEndHourCombo", 1440, 10080);
        payload.put("brand", fields.fieldValue("#elecBrandField", ""));
        payload.put("warrantyPeriod", fields.fieldValue("#elecWarrantyField", ""));
        return new CategoryBuildResult(startPrice, duration);
    }

    private CategoryBuildResult buildFashion(Map<String, Object> payload, CategoryFieldReader fields) {
        payload.put("type", "FASHION");
        double startPrice = parsePrice(fields.fieldValue("#fashionStartPriceField", ""), "Giá khởi điểm", 10000.0);
        int duration = checkedDuration(fields, "#fashionStartDatePicker", "#fashionStartHourCombo", "#fashionEndDatePicker", "#fashionEndHourCombo", 1440, 4320);
        payload.put("brand", fields.fieldValue("#fashionBrandField", ""));
        payload.put("material", fields.fieldValue("#fashionMaterialField", ""));
        return new CategoryBuildResult(startPrice, duration);
    }

    private CategoryBuildResult buildJewelry(Map<String, Object> payload, CategoryFieldReader fields) {
        payload.put("type", "JEWELRY");
        double startPrice = parsePrice(fields.fieldValue("#jewelryStartPriceField", ""), "Giá khởi điểm", 25000.0);
        int duration = checkedDuration(fields, "#jewelryStartDatePicker", "#jewelryStartHourCombo", "#jewelryEndDatePicker", "#jewelryEndHourCombo", 1440, 7200);
        payload.put("material", fields.fieldValue("#jewelryMaterialField", ""));
        payload.put("weight", parseDouble(fields.fieldValue("#jewelryWeightField", ""), "Trọng lượng", 0.1, 100000));
        return new CategoryBuildResult(startPrice, duration);
    }

    private CategoryBuildResult buildArt(Map<String, Object> payload, CategoryFieldReader fields) {
        payload.put("type", "ART");
        double startPrice = parsePrice(fields.fieldValue("#artStartPriceField", ""), "Giá khởi điểm", 100000.0);
        int duration = checkedDuration(fields, "#artStartDatePicker", "#artStartHourCombo", "#artEndDatePicker", "#artEndHourCombo", 1440, 20160);
        payload.put("creator", fields.fieldValue("#artCreatorField", ""));
        payload.put("material", fields.fieldValue("#artMaterialField", ""));
        return new CategoryBuildResult(startPrice, duration);
    }

    private CategoryBuildResult buildVehicle(Map<String, Object> payload, CategoryFieldReader fields) {
        payload.put("type", "VEHICLE");
        double startPrice = parsePrice(fields.fieldValue("#vehicleStartPriceField", ""), "Giá khởi điểm", 500000.0);
        int duration = checkedDuration(fields, "#vehicleStartDatePicker", "#vehicleStartHourCombo", "#vehicleEndDatePicker", "#vehicleEndHourCombo", 1440, 10080);
        payload.put("model", fields.fieldValue("#vehicleYearField", ""));
        payload.put("odometer", parseInt(fields.fieldValue("#vehicleOdometerField", ""), "Số km", 0, 10000000));
        return new CategoryBuildResult(startPrice, duration);
    }

    private CategoryBuildResult buildOther(Map<String, Object> payload, CategoryFieldReader fields) {
        payload.put("type", "OTHER");
        double startPrice = parsePrice(fields.fieldValue("#otherStartPriceField", ""), "Giá khởi điểm", 1.0);
        double reservePrice = parseOptionalPrice(fields.fieldValue("#otherReservePriceField", ""), "Giá bảo lưu", 1.0);
        double minimumBidIncrement = parsePrice(fields.fieldValue("#otherStepPriceField", ""), "Bước giá", 1.0);
        if (reservePrice > 0 && reservePrice < startPrice) {
            throw new ValidationException("Lỗi giá trị", "Giá bảo lưu phải lớn hơn hoặc bằng giá khởi điểm!");
        }
        payload.put("reservePrice", reservePrice);
        payload.put("minimumBidIncrement", minimumBidIncrement);
        int duration = checkedDuration(fields, "#otherStartDatePicker", "#otherStartHourCombo", "#otherEndDatePicker", "#otherEndHourCombo", 1440, 43200);
        return new CategoryBuildResult(startPrice, duration);
    }

    private int checkedDuration(CategoryFieldReader fields, String startDateId, String startHourId,
                                String endDateId, String endHourId, int min, int max) {
        long duration = calculateDuration(
                fields.dateValue(startDateId),
                fields.comboValue(startHourId),
                fields.dateValue(endDateId),
                fields.comboValue(endHourId));
        if (duration < min || duration > max) {
            throw new ValidationException("Lỗi thời gian", "Thời gian phải từ " + (min / 1440) + "-" + (max / 1440) + " ngày");
        }
        return (int) duration;
    }

    private long calculateDuration(LocalDate startDate, String startHour, LocalDate endDate, String endHour) {
        if (startDate == null || startHour == null || endDate == null || endHour == null) {
            return 1440;
        }
        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.parse(startHour));
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.parse(endHour));
        return Duration.between(startTime, endTime).toMinutes();
    }

    private double parsePrice(String value, String fieldName, double minValue) {
        return parseDouble(value, fieldName, minValue, Double.MAX_VALUE);
    }

    private double parseOptionalPrice(String value, String fieldName, double minValue) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        return parseDouble(value, fieldName, minValue, Double.MAX_VALUE);
    }

    private double parseDouble(String value, String fieldName, double minValue, double maxValue) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Thiếu thông tin", "Vui lòng nhập " + fieldName + "!");
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < minValue || parsed > maxValue) {
                throw new ValidationException("Lỗi giá trị", fieldName + " phải từ " + minValue + " đến " + maxValue);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ValidationException("Lỗi định dạng", fieldName + " phải là số!");
        }
    }

    private int parseInt(String value, String fieldName, int minValue, int maxValue) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Thiếu thông tin", "Vui lòng nhập " + fieldName + "!");
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minValue || parsed > maxValue) {
                throw new ValidationException("Lỗi giá trị", fieldName + " phải từ " + minValue + " đến " + maxValue);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new ValidationException("Lỗi định dạng", fieldName + " phải là số!");
        }
    }

    private record CategoryBuildResult(double startPrice, int durationMinutes) {
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase();
    }

    public static class ValidationException extends RuntimeException {
        private final String title;

        public ValidationException(String title, String message) {
            super(message);
            this.title = title;
        }

        public String getTitle() {
            return title;
        }
    }
}
