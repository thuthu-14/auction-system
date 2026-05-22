package server.exception;

public enum ServerErrorType {
    AUTHENTICATION("Lỗi xác thực"),
    PERMISSION("Không có quyền"),
    VALIDATION("Dữ liệu không hợp lệ"),
    BID("Lỗi đặt giá"),
    DATA("Lỗi dữ liệu"),
    NETWORK("Lỗi kết nối"),
    SYSTEM("Lỗi hệ thống");

    private final String defaultMessage;

    ServerErrorType(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
