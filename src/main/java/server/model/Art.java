package server.model;

import common.ItemCategory;

public class Art extends Item {
    private static final long serialVersionUID = 1L;

    private String creator;
    private String material;

    public Art(String itemId, String name, String description,
               double startingPrice, String sellerId,
               String creator, String material) {
        super(itemId, name, description, startingPrice, ItemCategory.ART, sellerId);
        this.creator = creator;
        this.material = material;
    }

    public Art() {
        super();
    }

    @Override
    public String getDetailedInfo() {
        return "🎨 NGHỆ THUẬT\n" +
                "Tên: " + name + "\n" +
                "Người tạo: " + creator + "\n" +
                "Chất liệu: " + material + "\n" +
                "Mô tả: " + description + "\n" +
                "Giá khởi điểm: $" + startingPrice;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    @Override
    public String toString() {
        return "Art{" + "name='" + name + '\'' + ", creator='" + creator + '\'' + '}';
    }
}
