package Common;
import java.time.LocalDateTime;
public class Art extends Item {
    public Art(String id, String name, String desc, double price, LocalDateTime start, LocalDateTime end, String sId) {
        super(id, name, desc, price, start, end, sId);
    }
    @Override public String getCategory() { return "Art"; }
}