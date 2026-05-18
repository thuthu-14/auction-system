package server.exception;

public enum ServerErrorType {
    AUTHENTICATION("Loi xac thuc"),
    PERMISSION("Khong co quyen"),
    VALIDATION("Du lieu khong hop le"),
    BID("Loi dat gia"),
    DATA("Loi du lieu"),
    NETWORK("Loi ket noi"),
    SYSTEM("Loi he thong");

    private final String defaultMessage;

    ServerErrorType(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
