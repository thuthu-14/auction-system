package server.model;

import java.io.Serializable;
import java.util.UUID;

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L; // Đảm bảo tính ổn định khi gửi qua Socket

    private String id;
    private String userId;      // Username của người nhận thông báo
    private String type;        // Loại thông báo: "WIN", "OUTBID", "SYSTEM"
    private String title;       // Tiêu đề
    private String description; // Nội dung chi tiết
    private String timeAgo;     // Thời gian hiển thị (VD: "Vừa xong", "10 phút trước")
    private String buttonText;  // Chữ trên nút bấm (VD: "Thanh toán ngay")
    private String referenceId; // ID tham chiếu (Mã đấu giá, mã giao dịch...)
    private boolean isRead;     // Trạng thái đã đọc hay chưa

    // Constructor rỗng (Bắt buộc phải có cho thư viện JSON như Jackson/Gson)
    public Notification() {
    }

    // Constructor đầy đủ
    public Notification(String userId, String type, String title, String description,
                        String timeAgo, String buttonText, String referenceId) {
        this.id = UUID.randomUUID().toString(); // Tự động tạo ID duy nhất
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.timeAgo = timeAgo;
        this.buttonText = buttonText;
        this.referenceId = referenceId;
        this.isRead = false; // Mặc định khi mới tạo là chưa đọc
    }

    // --- GETTERS & SETTERS ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}