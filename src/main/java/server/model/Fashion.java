// src/main/java/server/model/Fashion.java
package server.model;

import common.ItemCategory;

import java.util.List;


public class Fashion extends Item {
    private static final long serialVersionUID = 1L;

    private String brand;
    private String material;

    public Fashion(String itemId, String name, String description,
                   double startingPrice, String sellerId,
                   String brand, String material, List<String> images) {
        super(itemId, name, description, startingPrice, ItemCategory.FASHION, sellerId);
        this.brand = brand;
        this.material = material;
        this.setImages(images);
    }

    public Fashion() {
        super();
    }

    @Override
    public String getDetailedInfo() {
        return "👕 THỜI TRANG\n" +
                "Tên: " + name + "\n" +
                "Hãng: " + brand + "\n" +
                "Chất liệu: " + material + "\n" +
                "Mô tả: " + description + "\n" +
                "Giá khởi điểm: $" + startingPrice;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    @Override
    public String toString() {
        return "Fashion{" + "name='" + name + '\'' + ", brand='" + brand +
                '\'' + ", material='" + material + '\'' + '}';
    }
}
