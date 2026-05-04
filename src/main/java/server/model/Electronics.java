package server.model;

import common.ItemCategory;

import java.util.List;

public class Electronics extends Item {
    private static final long serialVersionUID = 1L;

    private String brand;
    private String warrantyPeriod;

    public Electronics(String itemId, String name, String description,
                       double startingPrice, String sellerId,
                       String brand, String warrantyPeriod, List<String> images) {
        super(itemId, name, description, startingPrice, ItemCategory.ELECTRONICS, sellerId);
        this.brand = brand;
        this.warrantyPeriod = warrantyPeriod;
        this.setImages(images);
    }

    public Electronics() {
        super();
    }

    @Override
    public String getDetailedInfo() {
        return "📱 ĐIỆN TỬ\n" +
                "Tên: " + name + "\n" +
                "Hãng: " + brand + "\n" +
                "Bảo hành: " + warrantyPeriod + "\n" +
                "Mô tả: " + description + "\n" +
                "Giá khởi điểm: $" + startingPrice;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getWarrantyPeriod() {
        return warrantyPeriod;
    }

    public void setWarrantyPeriod(String warrantyPeriod) {
        this.warrantyPeriod = warrantyPeriod;
    }

    @Override
    public String toString() {
        return "Electronics{" + "name='" + name + '\'' + ", brand='" + brand + '\'' + '}';
    }
}
