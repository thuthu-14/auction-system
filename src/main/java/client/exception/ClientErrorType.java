package client.exception;

public enum ClientErrorType {
    VALIDATION("Dữ liệu không hợp lệ"),
    NETWORK("Lỗi kết nối"),
    NAVIGATION("Lỗi điều hướng"),
    AUTHENTICATION("Lỗi đăng nhập"),
    DATA("Lỗi dữ liệu"),
    UI("Lỗi giao diện"),
    UNKNOWN("Lỗi hệ thống");

    private final String defaultTitle;

    ClientErrorType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String defaultTitle() {
        return defaultTitle;
    }
}
