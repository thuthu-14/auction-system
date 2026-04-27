package client.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class SellerStatisticsController implements Initializable {

    // Khai báo các thành phần UI từ file FXML
    @FXML private Label totalRevenueLabel;
    @FXML private Label avgBidLabel;
    @FXML private Label successRateLabel;
    @FXML private LineChart<String, Number> revenueChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Cài đặt dữ liệu cho biểu đồ
        setupRevenueChart();

        // 2. Gợi ý cách cập nhật số liệu (Sau này sếp dùng dữ liệu thật từ Database)
        /*
        totalRevenueLabel.setText("150.5M đ");
        avgBidLabel.setText("15.2");
        successRateLabel.setText("92.5%");
        */
    }

    /**
     * Hàm bơm dữ liệu giả vào Biểu đồ đường (Line Chart)
     */
    private void setupRevenueChart() {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh thu");

        // Thêm dữ liệu giả lập: Trục X là Tháng, Trục Y là Doanh thu (VNĐ)
        series.getData().add(new XYChart.Data<>("Tháng 1", 15000000));
        series.getData().add(new XYChart.Data<>("Tháng 2", 28000000));
        series.getData().add(new XYChart.Data<>("Tháng 3", 22000000));
        series.getData().add(new XYChart.Data<>("Tháng 4", 45000000));
        series.getData().add(new XYChart.Data<>("Tháng 5", 38000000));
        series.getData().add(new XYChart.Data<>("Tháng 6", 65000000));

        // Nạp series vào biểu đồ
        revenueChart.getData().add(series);
    }
}