package server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RequestPayloadUtilTest {

    @Test
    public void requiredAuctionId_nullOrBlank_throws() {
        assertThrows(Exception.class, () -> RequestPayloadUtil.requiredAuctionId((String) null));
        assertThrows(Exception.class, () -> RequestPayloadUtil.requiredAuctionId("  "));
    }
}

