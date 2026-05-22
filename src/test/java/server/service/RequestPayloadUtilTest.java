package server.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RequestPayloadUtilTest {

    @Test
    public void requiredAuctionId_nullOrBlank_throws() {
        assertThrows(Exception.class, () -> RequestPayloadUtil.requiredAuctionId((String) null));
        assertThrows(Exception.class, () -> RequestPayloadUtil.requiredAuctionId("  "));
    }

    @Test
    public void requiredAuctionId_extractsAndTrimsFromMap() {
        assertEquals("A1", RequestPayloadUtil.requiredAuctionId(Map.of("auctionId", "  A1  ")));
    }

    @Test
    public void requiredAuctionId_rejectsPlaceholderValues() {
        assertThrows(IllegalArgumentException.class, () -> RequestPayloadUtil.requiredAuctionId("--"));
        assertThrows(IllegalArgumentException.class, () -> RequestPayloadUtil.requiredAuctionId("null"));
        assertThrows(IllegalArgumentException.class,
                () -> RequestPayloadUtil.requiredAuctionId(Map.of("auctionId", " -- ")));
    }

    @Test
    public void requiredDouble_acceptsNumbersAndNumericStrings() {
        assertEquals(12.5, RequestPayloadUtil.requiredDouble(Map.of("amount", 12.5), "amount"), 0.001);
        assertEquals(99.0, RequestPayloadUtil.requiredDouble(Map.of("amount", " 99 "), "amount"), 0.001);
    }

    @Test
    public void requiredDouble_missingOrInvalidValue_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> RequestPayloadUtil.requiredDouble(Map.of(), "amount"));
        assertThrows(NumberFormatException.class,
                () -> RequestPayloadUtil.requiredDouble(Map.of("amount", "abc"), "amount"));
    }

    @Test
    public void optionalDouble_returnsFallbackWhenMissing() {
        assertEquals(7.0, RequestPayloadUtil.optionalDouble(Map.of(), "amount", 7.0), 0.001);
        assertEquals(3.5, RequestPayloadUtil.optionalDouble(Map.of("amount", "3.5"), "amount", 7.0), 0.001);
    }

    @Test
    public void booleanValue_parsesBooleanAndText() {
        assertTrue(RequestPayloadUtil.booleanValue(true));
        assertTrue(RequestPayloadUtil.booleanValue("true"));
        assertFalse(RequestPayloadUtil.booleanValue("false"));
        assertFalse(RequestPayloadUtil.booleanValue(null));
    }

    @Test
    public void objectMapAndStringMap_rejectNonMaps() {
        assertThrows(IllegalArgumentException.class, () -> RequestPayloadUtil.objectMap("not-map"));
        assertThrows(IllegalArgumentException.class, () -> RequestPayloadUtil.stringMap("not-map"));
    }
}

