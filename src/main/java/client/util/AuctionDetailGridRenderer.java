package client.util;

import com.google.gson.JsonObject;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AuctionDetailGridRenderer {
    private final Map<String, DetailRenderer> renderers = createDetailRenderers();

    @FunctionalInterface
    private interface DetailRenderer {
        int render(GridPane grid, JsonObject productData, int rowIndex);
    }

    public void render(GridPane grid, JsonObject itemData, JsonObject productData,
                       Function<Double, String> currencyFormatter) {
        if (grid == null) {
            return;
        }

        grid.getChildren().clear();
        int rowIndex = 0;
        rowIndex = addDetailRowIfPresent(grid, "Người bán", itemData, "sellerName", rowIndex);
        rowIndex = addDetailRowIfPresent(grid, "Mã sản phẩm", productData, "itemId", rowIndex);
        rowIndex = addDetailRowIfPresent(
                grid,
                "Giá khởi điểm",
                currencyFormatter.apply(JsonObjects.getDouble(productData, "startingPrice", 0)),
                rowIndex
        );

        DetailRenderer renderer = renderers.get(JsonObjects.getString(productData, "type", ""));
        if (renderer != null) {
            renderer.render(grid, productData, rowIndex);
        }
    }

    private Map<String, DetailRenderer> createDetailRenderers() {
        Map<String, DetailRenderer> detailRenderers = new HashMap<>();
        detailRenderers.put("VEHICLE", (grid, data, row) -> {
            row = addDetailRowIfPresent(grid, "Đời xe", data, "model", row);
            return addDetailRowIfPresent(grid, "Số km", JsonObjects.getInt(data, "odometer", 0) + " km", row);
        });
        detailRenderers.put("FASHION", (grid, data, row) -> {
            row = addDetailRowIfPresent(grid, "Thương hiệu", data, "brand", row);
            return addDetailRowIfPresent(grid, "Chất liệu", data, "material", row);
        });
        detailRenderers.put("ART", (grid, data, row) -> {
            row = addDetailRowIfPresent(grid, "Tác giả", data, "creator", row);
            return addDetailRowIfPresent(grid, "Chất liệu", data, "material", row);
        });
        detailRenderers.put("JEWELRY", (grid, data, row) -> {
            row = addDetailRowIfPresent(grid, "Chất liệu", data, "material", row);
            return addDetailRowIfPresent(grid, "Khối lượng", JsonObjects.getDouble(data, "weight", 0) + " gr", row);
        });
        detailRenderers.put("ELECTRONICS", (grid, data, row) -> {
            row = addDetailRowIfPresent(grid, "Thương hiệu", data, "brand", row);
            return addDetailRowIfPresent(grid, "Bảo hành", data, "warrantyPeriod", row);
        });
        return detailRenderers;
    }

    private int addDetailRowIfPresent(GridPane grid, String title, JsonObject data, String key, int rowIndex) {
        return addDetailRowIfPresent(grid, title, JsonObjects.getString(data, key, ""), rowIndex);
    }

    private int addDetailRowIfPresent(GridPane grid, String title, String value, int rowIndex) {
        if (value == null || value.isBlank() || value.startsWith("0")) {
            return rowIndex;
        }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px;");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: #111827; -fx-font-size: 14px; -fx-font-weight: bold;");
        grid.add(titleLabel, 0, rowIndex);
        grid.add(valueLabel, 1, rowIndex);
        return rowIndex + 1;
    }
}
