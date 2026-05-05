package server.model;

import common.ItemCategory;

import java.util.List;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;

    private String model;
    private int odometer;

    public Vehicle(String itemId, String name, String description,
                   double startingPrice, String sellerId,
                   String model, int odometer, List<String> images) {
        super(itemId, name, description, startingPrice, ItemCategory.VEHICLE, sellerId);
        this.model = model;
        this.odometer = odometer;
        this.setImages(images);
    }

    public Vehicle() {
        super();
    }

    @Override
    public String getDetailedInfo() {
        return "XE CỘ\n" +
                "Tên: " + name + "\n" +
                "Đời xe: " + model + "\n" +
                "Số km đã đi: " + odometer + "km\n" +
                "Mô tả: " + description + "\n" +
                "Giá khởi điểm: $" + startingPrice;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getOdometer() {
        return odometer;
    }

    public void setOdometer(int odometer) {
        this.odometer = odometer;
    }

    @Override
    public String toString() {
        return "Vehicle{" + "name='" + name + '\'' + ", model='" + model + '\'' + '}';
    }
}
