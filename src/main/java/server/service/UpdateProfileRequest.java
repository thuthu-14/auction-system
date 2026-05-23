package server.service;

import java.util.Map;

public final class UpdateProfileRequest {
    private final String name;
    private final String phone;
    private final String address;
    private final String password;

    private UpdateProfileRequest(String name, String phone, String address, String password) {
        this.name = name;
        this.phone = phone;
        this.address = address;
        this.password = password;
    }

    public static UpdateProfileRequest from(Object data) {
        if (!(data instanceof Map<?, ?> payload)) {
            return new UpdateProfileRequest("", "", "", "");
        }
        return new UpdateProfileRequest(
                text(payload.get("name")),
                text(payload.get("phone")),
                text(payload.get("address")),
                text(payload.get("password"))
        );
    }

    public String name() {
        return name;
    }

    public String phone() {
        return phone;
    }

    public String address() {
        return address;
    }

    public String password() {
        return password;
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
