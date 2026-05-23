package server.service;

import java.util.Map;

public final class UpgradeSellerRequest {
    private final String password;
    private final String shopName;
    private final String phone;
    private final String address;
    private final String email;

    private UpgradeSellerRequest(String password, String shopName, String phone, String address, String email) {
        this.password = password;
        this.shopName = shopName;
        this.phone = phone;
        this.address = address;
        this.email = email;
    }

    public static UpgradeSellerRequest from(Object data) {
        Map<String, String> payload = RequestPayloadUtil.stringMap(data);
        return new UpgradeSellerRequest(
                payload.get("password"),
                payload.get("shopName"),
                payload.get("phone"),
                payload.get("address"),
                payload.get("email")
        );
    }

    public String password() {
        return password;
    }

    public String shopName() {
        return shopName;
    }

    public String phone() {
        return phone;
    }

    public String address() {
        return address;
    }

    public String email() {
        return email;
    }
}
