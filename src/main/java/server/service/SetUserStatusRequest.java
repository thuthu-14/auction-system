package server.service;

import java.io.IOException;
import java.util.Map;

public final class SetUserStatusRequest {
    private final String userId;
    private final boolean active;

    private SetUserStatusRequest(String userId, boolean active) {
        this.userId = userId;
        this.active = active;
    }

    public static SetUserStatusRequest from(Object data) throws IOException {
        Map<String, Object> payload = RequestPayloadUtil.objectMap(data);
        String userId = RequestPayloadUtil.text(payload.get("userId"));
        if (userId == null) {
            throw new IOException("User not found");
        }
        return new SetUserStatusRequest(userId, RequestPayloadUtil.booleanValue(payload.get("active")));
    }

    public String userId() {
        return userId;
    }

    public boolean active() {
        return active;
    }
}
