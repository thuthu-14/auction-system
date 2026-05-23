package server.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BidChargeCalculatorTest {
    @Test
    void calculateChargeAmount_returnsIncrementOnly() {
        BidChargeCalculator calculator = new BidChargeCalculator();

        assertEquals(150.0, calculator.calculateChargeAmount(150.0, 0.0), 0.001);
        assertEquals(30.0, calculator.calculateChargeAmount(150.0, 120.0), 0.001);
        assertEquals(0.0, calculator.calculateChargeAmount(120.0, 120.0), 0.001);
    }
}
