package Common;

public class Bidder extends User {
    private double maxPrice;     // Giá tối đa có thể trả (cho Auto-bid)
    private double bidIncrement; // Bước giá tự động

    public Bidder(String id, String username, String password) {
        super(id, username, password, "BIDDER");
    }

    // Getters/Setters cho chức năng Auto-Bidding
    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }
    public double getBidIncrement() { return bidIncrement; }
    public void setBidIncrement(double bidIncrement) { this.bidIncrement = bidIncrement; }
}
