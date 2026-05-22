package util;

import common.ItemCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemValidationUtilTest {

    @Test
    void startingPriceValidationUsesCategoryMinimums() {
        assertTrue(ItemValidationUtil.isValidStartingPrice(ItemCategory.OTHER, 1.0));
        assertFalse(ItemValidationUtil.isValidStartingPrice(ItemCategory.OTHER, 0.0));
        assertFalse(ItemValidationUtil.isValidStartingPrice(ItemCategory.ELECTRONICS, 49_999.0));
        assertNull(ItemValidationUtil.getStartingPriceErrorMessage(ItemCategory.ELECTRONICS, 50_000.0));
        assertNotNull(ItemValidationUtil.getStartingPriceErrorMessage(ItemCategory.ELECTRONICS, 1.0));
        assertNotNull(ItemValidationUtil.getStartingPriceErrorMessage(ItemCategory.OTHER, 0.0));
    }

    @Test
    void bidAmountValidationUsesCurrentPriceAndIncrement() {
        assertTrue(ItemValidationUtil.isValidBidAmount(ItemCategory.OTHER, 100.0, 101.0));
        assertFalse(ItemValidationUtil.isValidBidAmount(ItemCategory.OTHER, 100.0, 0.0));
        assertFalse(ItemValidationUtil.isValidBidAmount(ItemCategory.OTHER, 100.0, 100.0));
        assertNull(ItemValidationUtil.getBidAmountErrorMessage(ItemCategory.OTHER, 100.0, 101.0));
        assertNotNull(ItemValidationUtil.getBidAmountErrorMessage(ItemCategory.OTHER, 100.0, 100.0));
        assertNotNull(ItemValidationUtil.getBidAmountErrorMessage(100.0, 110.0, 25.0));
    }

    @Test
    void durationValidationUsesCategoryBounds() {
        assertTrue(ItemValidationUtil.isValidDuration(ItemCategory.OTHER, ItemCategory.OTHER.getMinDurationMinutes()));
        assertFalse(ItemValidationUtil.isValidDuration(ItemCategory.OTHER, 1));
        assertNull(ItemValidationUtil.getDurationErrorMessage(ItemCategory.OTHER, ItemCategory.OTHER.getMinDurationMinutes()));
        assertNotNull(ItemValidationUtil.getDurationErrorMessage(ItemCategory.OTHER, 1));
        assertNotNull(ItemValidationUtil.getDurationErrorMessage(
                ItemCategory.OTHER, ItemCategory.OTHER.getMaxDurationMinutes() + 1));
    }

    @Test
    void categorySpecificFieldValidatorsRejectBlankOrOutOfRangeValues() {
        assertTrue(ItemValidationUtil.isValidBrand("Brand"));
        assertFalse(ItemValidationUtil.isValidBrand(" "));
        assertFalse(ItemValidationUtil.isValidBrand("x".repeat(101)));
        assertTrue(ItemValidationUtil.isValidMaterial("Gold"));
        assertFalse(ItemValidationUtil.isValidMaterial(null));
        assertTrue(ItemValidationUtil.isValidWeight(1.5));
        assertFalse(ItemValidationUtil.isValidWeight(0.0));
        assertFalse(ItemValidationUtil.isValidWeight(100_001.0));
        assertTrue(ItemValidationUtil.isValidOdometer(0));
        assertFalse(ItemValidationUtil.isValidOdometer(-1));
        assertFalse(ItemValidationUtil.isValidOdometer(10_000_001));
        assertTrue(ItemValidationUtil.isValidWarrantyPeriod("12 months"));
        assertFalse(ItemValidationUtil.isValidWarrantyPeriod(""));
    }

    @Test
    void categoryInfoIncludesConfiguredValues() {
        String info = ItemValidationUtil.getCategoryInfo(ItemCategory.OTHER);

        assertTrue(info.contains(ItemCategory.OTHER.getDescription()));
        assertTrue(info.contains(String.format("%.2f", ItemCategory.OTHER.getMinimumStartingBid())));
    }

    @Test
    void utilityConstructorsAreReachableForCoverage() {
        assertNotNull(new ItemValidationUtil());
        assertNotNull(new DateTimeUtil());
        assertNotNull(new JsonUtil());
        assertNotNull(new ValidationUtil());
        assertNotNull(new LoggerUtil());
    }
}
