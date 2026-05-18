package client.exception;

public enum ClientErrorType {
    VALIDATION("Du lieu khong hop le"),
    NETWORK("Loi ket noi"),
    NAVIGATION("Loi dieu huong"),
    AUTHENTICATION("Loi dang nhap"),
    DATA("Loi du lieu"),
    UI("Loi giao dien"),
    UNKNOWN("Loi he thong");

    private final String defaultTitle;

    ClientErrorType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String defaultTitle() {
        return defaultTitle;
    }
}
