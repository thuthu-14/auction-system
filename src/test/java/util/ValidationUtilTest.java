package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void usernameValidationRequiresLengthAndAllowedCharacters() {
        assertTrue(ValidationUtil.isValidUsername("user_123"));
        assertFalse(ValidationUtil.isValidUsername(null));
        assertFalse(ValidationUtil.isValidUsername("ab"));
        assertFalse(ValidationUtil.isValidUsername("x".repeat(51)));
        assertFalse(ValidationUtil.isValidUsername("bad-name"));
    }

    @Test
    void passwordEmailAndNumericValidatorsRejectInvalidValues() {
        assertTrue(ValidationUtil.isValidPassword("secret"));
        assertFalse(ValidationUtil.isValidPassword("short"));
        assertFalse(ValidationUtil.isValidPassword(null));

        assertTrue(ValidationUtil.isValidEmail("a@b.com"));
        assertFalse(ValidationUtil.isValidEmail("not-email"));
        assertFalse(ValidationUtil.isValidEmail(null));

        assertTrue(ValidationUtil.isValidBidAmount(1.0));
        assertFalse(ValidationUtil.isValidBidAmount(0.0));
        assertFalse(ValidationUtil.isValidBidAmount(Double.NaN));
        assertFalse(ValidationUtil.isValidBidAmount(Double.POSITIVE_INFINITY));

        assertTrue(ValidationUtil.isValidPrice(1.0));
        assertFalse(ValidationUtil.isValidPrice(0.0));
        assertFalse(ValidationUtil.isValidPrice(Double.NaN));
    }

    @Test
    void auctionDurationItemNameAndDescriptionValidationUseBounds() {
        assertTrue(ValidationUtil.isValidAuctionDuration(1));
        assertTrue(ValidationUtil.isValidAuctionDuration(10_080));
        assertFalse(ValidationUtil.isValidAuctionDuration(0));
        assertFalse(ValidationUtil.isValidAuctionDuration(10_081));

        assertTrue(ValidationUtil.isValidItemName("Valid item"));
        assertFalse(ValidationUtil.isValidItemName(null));
        assertFalse(ValidationUtil.isValidItemName("ab"));
        assertFalse(ValidationUtil.isValidItemName("x".repeat(201)));

        assertTrue(ValidationUtil.isValidDescription(""));
        assertTrue(ValidationUtil.isValidDescription("x".repeat(1000)));
        assertFalse(ValidationUtil.isValidDescription(null));
        assertFalse(ValidationUtil.isValidDescription("x".repeat(1001)));
    }
}
