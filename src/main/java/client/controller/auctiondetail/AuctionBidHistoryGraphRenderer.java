package client.controller.auctiondetail;

import javafx.application.Platform;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import server.model.Bid;

import java.util.List;
import java.util.function.DoubleFunction;

public class AuctionBidHistoryGraphRenderer {
    private final LineChart<String, Number> chart;
    private final CategoryAxis turnAxis;
    private final NumberAxis priceAxis;
    private final DoubleFunction<String> moneyFormatter;
    private final Runnable pinnedAxisUpdater;

    public AuctionBidHistoryGraphRenderer(LineChart<String, Number> chart,
                                          CategoryAxis turnAxis,
                                          NumberAxis priceAxis,
                                          DoubleFunction<String> moneyFormatter,
                                          Runnable pinnedAxisUpdater) {
        this.chart = chart;
        this.turnAxis = turnAxis;
        this.priceAxis = priceAxis;
        this.moneyFormatter = moneyFormatter;
        this.pinnedAxisUpdater = pinnedAxisUpdater;
    }

    public void render(List<Bid> sortedBids, double firstValue, double minimumBidIncrement) {
        if (chart == null) {
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Giá theo lượt đặt");

        XYChart.Data<String, Number> basePoint = new XYChart.Data<>("Giá đầu", firstValue);
        series.getData().add(basePoint);
        basePoint.setXValue("Điểm khởi đầu");
        basePoint.setNode(createBidPoint("Điểm khởi đầu\n" + moneyFormatter.apply(firstValue)));

        if (sortedBids != null) {
            for (int i = 0; i < sortedBids.size(); i++) {
                Bid bid = sortedBids.get(i);
                String turnLabel = "Lần thứ " + (i + 1);
                XYChart.Data<String, Number> bidPoint = new XYChart.Data<>(turnLabel, bid.getAmount());
                bidPoint.setNode(createBidPoint(turnLabel
                        + "\nNgười đặt: " + safeText(bid.getBidderName(), "Không rõ")
                        + "\nGiá: " + moneyFormatter.apply(bid.getAmount())));
                series.getData().add(bidPoint);
            }
        }

        applySeries(series);
        updatePriceAxis(sortedBids != null ? sortedBids : List.of(), firstValue, minimumBidIncrement);
    }

    private void applySeries(XYChart.Series<String, Number> series) {
        if (turnAxis != null) {
            turnAxis.getCategories().setAll(series.getData().stream()
                    .map(XYChart.Data::getXValue)
                    .toList());
        }
        chart.getData().setAll(series);
        double width = calculateChartWidth(series.getData().size());
        chart.setPrefWidth(width);
        chart.setMinWidth(width);
        chart.setMaxWidth(Region.USE_PREF_SIZE);
    }

    private double calculateChartWidth(int pointCount) {
        return Math.max(900, 180 + Math.max(pointCount, 1) * 90.0);
    }

    private StackPane createBidPoint(String tooltipText) {
        Circle outer = new Circle(6);
        outer.setStyle("-fx-fill: #f15a24;");
        Circle inner = new Circle(3);
        inner.setStyle("-fx-fill: white;");
        StackPane point = new StackPane(outer, inner);
        point.setMinSize(18, 18);
        point.setPrefSize(18, 18);
        point.setMaxSize(18, 18);
        Tooltip.install(point, new Tooltip(tooltipText));
        return point;
    }

    private void updatePriceAxis(List<Bid> sortedBids, double firstValue, double minimumBidIncrement) {
        if (priceAxis == null) {
            return;
        }

        double minValue = firstValue;
        double maxValue = firstValue;
        for (Bid bid : sortedBids) {
            minValue = Math.min(minValue, bid.getAmount());
            maxValue = Math.max(maxValue, bid.getAmount());
        }

        double spread = Math.max(maxValue - minValue, Math.max(maxValue * 0.02, minimumBidIncrement));
        double tickUnit = choosePriceTickUnit(spread);
        double padding = Math.max(tickUnit * 2, spread * 0.2);
        double lowerBound = Math.max(0, Math.floor((minValue - padding) / tickUnit) * tickUnit);
        double upperBound = Math.ceil((maxValue + padding) / tickUnit) * tickUnit;
        if (upperBound <= lowerBound) {
            upperBound = lowerBound + tickUnit * 6;
        }

        priceAxis.setLowerBound(lowerBound);
        priceAxis.setUpperBound(upperBound);
        priceAxis.setTickUnit(tickUnit);
        if (pinnedAxisUpdater != null) {
            Platform.runLater(pinnedAxisUpdater);
        }
    }

    private double choosePriceTickUnit(double spread) {
        double target = Math.max(spread / 7, 1);
        double[] units = {10_000, 25_000, 50_000, 100_000, 250_000, 500_000, 1_000_000, 2_500_000, 5_000_000, 10_000_000};
        for (double unit : units) {
            if (target <= unit) {
                return unit;
            }
        }
        return 25_000_000;
    }

    private String safeText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
