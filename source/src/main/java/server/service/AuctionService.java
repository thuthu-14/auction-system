import java.io.IOException;

public class AuctionService {

    public static Auction createAuction(RegularUser seller, Item item, int durationMinutes)
            throws PermissionDeniedException, IOException, ClassNotFoundException {

        // TODO: validate seller and item
        // TODO: validate price and duration
        // TODO: create auction
        return null;
    }

    public static List<Auction> getActiveAuctions()
            throws IOException, ClassNotFoundException {
        return null;
    }

    public static Auction getAuctionById(String auctionId)
            throws IOException, ClassNotFoundException {
        return null;
    }

    public static void finishAuction(String auctionId)
            throws IOException, ClassNotFoundException {
        // TODO
    }
}