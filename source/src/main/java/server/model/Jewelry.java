// src/main/java/server/model/Jewelry.java
package server.model;

import common.ItemCategory;

/**
 * Jewelry - Loại sản phẩm Trang sức
 * OOP: Inheritance, Polymorphism
 *
 * Thuộc tính riêng:
 * - material: Chất liệu (Gold, Silver, Platinum, Diamond, Pearl, v.v)
 * - weight: Trọng lượng (gram)
 */
public class Jewelry extends Item {
    private static final long serialVersionUID = 1L;

    private String material;
    private double weight; // gram

    public Jewelry(String itemId, String name, String description,
                   double startingPrice, String sellerId,
                   String material, double weight) {
        super(itemId, name, description, startingPrice, ItemCategory.JEWELRY, sellerId);
        this.material = material;
        this.weight = weight;
    }

    public Jewelry() {
        super();
    }

    @Override
    public String getDetailedInfo() {
        return "💎 TRANG SỨC\n" +
                "Tên: " + name + "\n" +
                "Chất liệu: " + material + "\n" +
                "Trọng lượng: " + weight + "g\n" +
                "Mô tả: " + description + "\n" +
                "Giá khởi điểm: $" + startingPrice;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Jewelry{" + "name='" + name + '\'' + ", material='" + material +
                '\'' + ", weight=" + weight + "g" + '}';
    }
}
