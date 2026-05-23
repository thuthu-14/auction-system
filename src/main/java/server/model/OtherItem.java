package server.model;

import common.ItemCategory;

import java.util.List;

public class OtherItem extends Item {
    private static final long serialVersionUID = 1L;

    public OtherItem(String itemId, String name, String description,
                     double startingPrice, String sellerId, List<String> images) {
        super(itemId, name, description, startingPrice, ItemCategory.OTHER, sellerId);
        this.setImages(images);
    }

    public OtherItem() {
        super();
    }

    @Override
    public String getDetailedInfo() {
        return "KHÁC\n" +
                "Tên: " + name + "\n" +
                "Mô tả: " + description + "\n" +
                "Giá khởi điểm: " + startingPrice;
    }
}
