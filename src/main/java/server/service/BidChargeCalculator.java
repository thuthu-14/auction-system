package server.service;

public class BidChargeCalculator {
    public double calculateChargeAmount(double amount, double previousUserBidAmount) {
        return amount - previousUserBidAmount;
    }
}
