package util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void formatTimestamp_formatsHoursMinutesSeconds() {
        assertEquals("00:00:00", DateTimeUtil.formatTimestamp(0));
        assertEquals("01:02:03", DateTimeUtil.formatTimestamp(3_723_000L));
    }

    @Test
    void remainingTimeReturnsZeroWhenExpired() {
        long expired = System.currentTimeMillis() - 1_000L;

        assertEquals(0, DateTimeUtil.getMinutesRemaining(expired));
        assertEquals(0, DateTimeUtil.getSecondsRemaining(expired));
        assertEquals("00:00", DateTimeUtil.formatTimeRemaining(expired));
        assertTrue(DateTimeUtil.isExpired(expired));
    }

    @Test
    void remainingTimeForFutureEndTimeIsPositive() {
        long future = System.currentTimeMillis() + 125_000L;

        assertTrue(DateTimeUtil.getSecondsRemaining(future) > 0);
        assertTrue(DateTimeUtil.getMinutesRemaining(future) >= 1);
        assertFalse(DateTimeUtil.isExpired(future));
        assertTrue(DateTimeUtil.formatTimeRemaining(future).matches("\\d{2}:\\d{2}"));
    }

    @Test
    void millisecondsToMinutesTruncatesTowardZero() {
        assertEquals(2, DateTimeUtil.millisecondsToMinutes(125_000L));
        assertEquals(0, DateTimeUtil.millisecondsToMinutes(59_999L));
    }
}
