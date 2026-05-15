package server.repository;

import common.ItemCategory;
import server.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ItemMapper {

    public static Item fromResultSet(ResultSet rs, List<String> images) throws SQLException {
        String type = rs.getString("type");
        String categoryText = rs.getString("category");
        ItemCategory category = categoryText != null ? ItemCategory.valueOf(categoryText) : ItemCategory.OTHER;

        Item item = createByType(type, category);

        item.setItemId(rs.getString("item_id"));
        item.setName(rs.getString("name"));
        item.setDescription(rs.getString("description"));
        item.setStartingPrice(rs.getDouble("starting_price"));
        item.setCategory(category);
        item.setSellerId(rs.getString("seller_id"));
        item.setImages(images);

        // NOTE: createdAt khong co setter trong Item -> neu can, phai them setter.
        // item.setCreatedAt(rs.getLong("created_at"));

        if (item instanceof Electronics e) {
            e.setBrand(rs.getString("brand"));
            e.setWarrantyPeriod(rs.getString("warranty_period"));
        } else if (item instanceof Vehicle v) {
            v.setModel(rs.getString("model"));
            v.setOdometer(rs.getInt("odometer"));
        } else if (item instanceof Fashion f) {
            f.setBrand(rs.getString("brand"));
            f.setMaterial(rs.getString("material"));
        } else if (item instanceof Jewelry j) {
            j.setMaterial(rs.getString("material"));
            j.setWeight(rs.getDouble("weight"));
        } else if (item instanceof Art a) {
            a.setCreator(rs.getString("creator"));
            a.setMaterial(rs.getString("material"));
        }

        return item;
    }

    private static Item createByType(String type, ItemCategory category) {
        if (type == null) type = "";

        return switch (type) {
            case "Electronics" -> new Electronics();
            case "Vehicle" -> new Vehicle();
            case "Fashion" -> new Fashion();
            case "Jewelry" -> new Jewelry();
            case "Art" -> new Art();
            case "OtherItem" -> new OtherItem();
            default -> {
                // fallback theo category
                yield switch (category) {
                    case ELECTRONICS -> new Electronics();
                    case VEHICLE -> new Vehicle();
                    case FASHION -> new Fashion();
                    case JEWELRY -> new Jewelry();
                    case ART -> new Art();
                    default -> new OtherItem();
                };
            }
        };
    }

    public static String resolveType(Item item) {
        if (item == null) return "OtherItem";
        return item.getClass().getSimpleName();
    }
}