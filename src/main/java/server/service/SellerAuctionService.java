package server.service;

import common.ItemCategory;
import server.exception.PermissionDeniedException;
import server.model.*;
import util.ItemValidationUtil;
import util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SellerAuctionService {
    public Auction createSellerAuction(RegularUser seller, Map<String, Object> auctionData) throws Exception {
        return createAuctionInternal(seller, auctionData, true);
    }

    public Auction createSellerAuction(User currentUser, Object data) throws Exception {
        return createSellerAuction(requireRegularUser(currentUser), RequestPayloadUtil.objectMap(data));
    }

    public Auction createAuction(RegularUser seller, Map<String, Object> auctionData) throws Exception {
        return createAuctionInternal(seller, auctionData, false);
    }

    public Auction createAuction(User currentUser, Object data) throws Exception {
        return createAuction(requireRegularUser(currentUser), RequestPayloadUtil.objectMap(data));
    }

    private Auction createAuctionInternal(RegularUser seller, Map<String, Object> auctionData, boolean sellerRequired)
            throws Exception {
        if (sellerRequired) {
            requireSeller(seller);
        } else if (seller == null) {
            throw new PermissionDeniedException("Ban phai dang nhap bang tai khoan nguoi ban");
        }
        String itemType = firstText(auctionData, "itemType", "Type", "type");
        if (itemType == null) {
            throw new IllegalArgumentException("Thieu loai san pham");
        }
        itemType = itemType.toUpperCase();
        ItemCategory category = parseCategory(itemType);

        String name = text(auctionData.get("name"));
        String description = text(auctionData.get("description"));
        double price = requiredNumber(auctionData, "price", "startingPrice").doubleValue();
        int duration = requiredNumber(auctionData, "duration").intValue();
        long startTime = longValue(auctionData.get("startTime"), System.currentTimeMillis());
        long endTime = longValue(auctionData.get("endTime"), startTime + duration * 60_000L);
        duration = (int) ((endTime - startTime) / 60_000L);
        double reservePrice = numberValue(auctionData.get("reservePrice"), 0.0);
        double minimumBidIncrement = numberValue(auctionData.get("minimumBidIncrement"), 0.0);

        validateCommon(category, name, description, price, duration);
        validateTimeWindow(startTime, endTime);
        validateCategoryFields(itemType, auctionData);

        Item item = createItemFromData(seller.getUserId(), itemType, auctionData);
        return AuctionService.createAuction(seller, item, startTime, endTime, reservePrice, minimumBidIncrement);
    }

    public void deleteSellerAuction(RegularUser seller, String auctionId) throws Exception {
        requireSeller(seller);
        Auction auction = AuctionService.getAuctionById(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Phien dau gia khong ton tai");
        }
        if (!seller.getUserId().equals(auction.getSellerId())) {
            throw new PermissionDeniedException("Ban khong phai chu cua phien dau gia nay");
        }
        AuctionService.deleteAuction(auctionId);
    }

    public String deleteSellerAuction(User currentUser, Object data) throws Exception {
        String auctionId = RequestPayloadUtil.requiredAuctionId(data);
        deleteSellerAuction(requireRegularUser(currentUser), auctionId);
        return auctionId;
    }

    public Item createItemFromData(String sellerId, String itemType, Map<String, Object> data) throws Exception {
        String name = text(data.get("name"));
        String description = text(data.get("description"));
        double price = optionalNumber(data, "price", "startingPrice");
        List<String> images = stringList(data.get("images"));

        return switch (itemType.toUpperCase()) {
            case "ELECTRONICS" -> new Electronics("", name, description, price, sellerId,
                    text(data.get("brand")), firstText(data, "warranty", "warrantyPeriod"), images);
            case "ART" -> new Art("", name, description, price, sellerId,
                    text(data.get("creator")), text(data.get("material")), images);
            case "VEHICLE" -> new Vehicle("", name, description, price, sellerId,
                    text(data.get("model")), intValue(data.get("odometer"), 0), images);
            case "FASHION" -> new Fashion("", name, description, price, sellerId,
                    text(data.get("brand")), text(data.get("material")), images);
            case "JEWELRY" -> new Jewelry("", name, description, price, sellerId,
                    text(data.get("material")), numberValue(data.get("weight"), 0.0), images);
            case "OTHER" -> new OtherItem("", name, description, price, sellerId, images);
            default -> throw new IllegalArgumentException("Unknown item type: " + itemType);
        };
    }

    private void validateCommon(ItemCategory category, String name, String description, double price, int duration) {
        if (!ValidationUtil.isValidItemName(name)) {
            throw new IllegalArgumentException("Ten san pham khong hop le");
        }
        if (!ValidationUtil.isValidDescription(description)) {
            throw new IllegalArgumentException("Mo ta san pham khong hop le");
        }
        String priceError = ItemValidationUtil.getStartingPriceErrorMessage(category, price);
        if (priceError != null) {
            throw new IllegalArgumentException(priceError);
        }
        String durationError = ItemValidationUtil.getDurationErrorMessage(category, duration);
        if (durationError != null) {
            throw new IllegalArgumentException(durationError);
        }
    }

    private void validateTimeWindow(long startTime, long endTime) {
        long now = System.currentTimeMillis();
        if (startTime < now - 60_000L) {
            throw new IllegalArgumentException("Thoi gian bat dau phai tu thoi diem hien tai tro di");
        }
        if (endTime <= startTime) {
            throw new IllegalArgumentException("Thoi gian ket thuc phai sau thoi gian bat dau");
        }
    }

    private void validateCategoryFields(String itemType, Map<String, Object> data) {
        switch (itemType) {
            case "ELECTRONICS" -> {
                if (!ItemValidationUtil.isValidBrand(text(data.get("brand")))) {
                    throw new IllegalArgumentException("Hang san xuat khong hop le");
                }
                if (!ItemValidationUtil.isValidWarrantyPeriod(firstText(data, "warranty", "warrantyPeriod"))) {
                    throw new IllegalArgumentException("Thoi gian bao hanh khong hop le");
                }
            }
            case "ART" -> {
                if (!ItemValidationUtil.isValidBrand(text(data.get("creator")))) {
                    throw new IllegalArgumentException("Ten nguoi tao khong hop le");
                }
                if (!ItemValidationUtil.isValidMaterial(text(data.get("material")))) {
                    throw new IllegalArgumentException("Chat lieu khong hop le");
                }
            }
            case "VEHICLE" -> {
                if (!ItemValidationUtil.isValidBrand(text(data.get("model")))) {
                    throw new IllegalArgumentException("Doi xe khong hop le");
                }
                if (!ItemValidationUtil.isValidOdometer(intValue(data.get("odometer"), -1))) {
                    throw new IllegalArgumentException("So km khong hop le");
                }
            }
            case "FASHION" -> {
                if (!ItemValidationUtil.isValidBrand(text(data.get("brand")))) {
                    throw new IllegalArgumentException("Hang khong hop le");
                }
                if (!ItemValidationUtil.isValidMaterial(text(data.get("material")))) {
                    throw new IllegalArgumentException("Chat lieu khong hop le");
                }
            }
            case "JEWELRY" -> {
                if (!ItemValidationUtil.isValidMaterial(text(data.get("material")))) {
                    throw new IllegalArgumentException("Chat lieu khong hop le");
                }
                if (!ItemValidationUtil.isValidWeight(numberValue(data.get("weight"), -1.0))) {
                    throw new IllegalArgumentException("Trong luong khong hop le");
                }
            }
            case "OTHER" -> {
            }
            default -> throw new IllegalArgumentException("Loai san pham khong hop le");
        }
    }

    private ItemCategory parseCategory(String itemType) {
        try {
            return ItemCategory.valueOf(itemType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Loai san pham khong hop le: " + itemType);
        }
    }

    private void requireSeller(RegularUser seller) throws PermissionDeniedException {
        if (seller == null) {
            throw new PermissionDeniedException("Ban phai dang nhap bang tai khoan nguoi ban");
        }
        if (!seller.isSeller()) {
            throw new PermissionDeniedException("Ban khong phai Seller");
        }
    }

    private RegularUser requireRegularUser(User user) throws PermissionDeniedException {
        if (!(user instanceof RegularUser regularUser)) {
            throw new PermissionDeniedException("Ban phai dang nhap bang tai khoan nguoi ban");
        }
        return regularUser;
    }

    private Number requiredNumber(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof Number number) {
                return number;
            }
        }
        throw new IllegalArgumentException("Thieu du lieu so: " + String.join("/", keys));
    }

    private double optionalNumber(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            Object value = data.get(key);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        }
        return 0.0;
    }

    private String firstText(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            String value = text(data.get(key));
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private double numberValue(Object value, double fallback) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString().trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString().trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return new ArrayList<>();
    }
}
