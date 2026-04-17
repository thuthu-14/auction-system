package util;

public class ValidationUtil {

    public static boolean isValidUsername(String username) {
        return username != null &&
                username.length() >= 3 &&
                username.length() <= 50 &&
                username.matches("^[a-zA-Z0-9_]+$");
    }

    public static boolean isValidPassword(String password) {
        return password != null &&
                password.length() >= 6;
    }

    public static boolean isValidEmail(String email) {
        return email != null &&
                email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public static boolean isValidBidAmount(double amount) {
        return amount > 0 && !Double.isNaN(amount) && !Double.isInfinite(amount);
    }

    public static boolean isValidPrice(double price) {
        return price > 0 && !Double.isNaN(price) && !Double.isInfinite(price);
    }

    public static boolean isValidAuctionDuration(int minutes) {
        return minutes > 0 && minutes <= 10080; // Max 7 days
    }

    public static boolean isValidItemName(String name) {
        return name != null &&
                name.trim().length() >= 3 &&
                name.trim().length() <= 200;
    }

    public static boolean isValidDescription(String description) {
        return description != null &&
                description.trim().length() <= 1000;
    }
}
