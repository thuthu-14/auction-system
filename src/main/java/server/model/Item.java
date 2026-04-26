package server.model;

import common.ItemCategory;
import java.io.Serializable;

public abstract class Item implements Serializable {
    private static final long serialVersionUID = 1L;

    protected String itemId;
    protected String name;
    protected String description;
    protected double startingPrice;
    protected ItemCategory category;
    protected String sellerId;
    protected long createdAt;

    public Item(String itemId, String name, String description,
                double startingPrice, ItemCategory category, String sellerId) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.category = category;
        this.sellerId = sellerId;
        this.createdAt = System.currentTimeMillis();
    }

    public Item() {}

    public abstract String getDetailedInfo();

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public ItemCategory getCategory() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        this.category = category;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Item{" + "itemId='" + itemId + '\'' + ", name='" + name + '\'' + '}';
    }
}
