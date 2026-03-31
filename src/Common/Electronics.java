// File Electronics.java
package Common;
import java.time.LocalDateTime;
public class Electronics extends Item {
    public Electronics(String id, String name, String desc, double price, LocalDateTime start, LocalDateTime end, String sId) {
        super(id, name, desc, price, start, end, sId);
    }
    @Override public String getCategory() { return "Electronics"; }
}

