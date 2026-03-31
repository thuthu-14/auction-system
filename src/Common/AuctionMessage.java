package Common;

import java.io.Serializable;

public class AuctionMessage implements Serializable {
    private String type; // VD: LOGIN, REGISTER, PLACE_BID, UPDATE_LIST, NOTIFY_WINNER
    private Object data; // Chứa đối tượng User, Item hoặc List<Item>

    public AuctionMessage(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public String getType() { return type; }
    public Object getData() { return data; }
}
