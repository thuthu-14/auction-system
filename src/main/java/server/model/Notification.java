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
    private long createdAt = System.currentTimeMillis(); // Thời điểm tạo thông báo
    private String buttonText;  // Chữ trên nút bấm (VD: "Thanh toán ngày")
    private String referenceId; // ID tham chiếu (Mã đấu giá, mã giao dịch...)
    private boolean isRead;     // Trạng thái đã đọc hay chưa


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
        this.createdAt = System.currentTimeMillis();
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
        return formatTimeAgo();
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String formatTimeAgo() {
        if (createdAt <= 0) {
            return timeAgo != null ? timeAgo : "V\u1eeba xong";
        }

        long diffSeconds = Math.max(0, (System.currentTimeMillis() - createdAt) / 1000);
        if (diffSeconds < 60) {
            return "V\u1eeba xong";
        }

        long minutes = diffSeconds / 60;
        if (minutes < 60) {
            return minutes + " ph\u00fat tr\u01b0\u1edbc";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + " gi\u1edd tr\u01b0\u1edbc";
        }

        long days = hours / 24;
        if (days < 30) {
            return days + " ng\u00e0y tr\u01b0\u1edbc";
        }

        long months = days / 30;
        if (months < 12) {
            return months + " th\u00e1ng tr\u01b0\u1edbc";
        }

        return (months / 12) + " n\u0103m tr\u01b0\u1edbc";
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
