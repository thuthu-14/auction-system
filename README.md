# Bài tập lớn môn lập trình nâng cao

### 📋 Bảng phân công nhiệm vụ

| Thành viên          | Vai trò đảm nhiệm | Nhiệm vụ chi tiết & File phụ trách | Mục tiêu đầu ra |
|:--------------------| :--- | :--- | :--- |
| **Phạm Yến Nhi**    | Xây dựng Hạ tầng & Đa luồng | - Xây dựng các hàm tiện ích dùng chung (common/*, util/*).<br>- Cấu hình quản lý luồng xử lý (ThreadPoolManager.java).<br>- Cơ chế khóa đồng bộ dữ liệu (SyncLockManager.java). | Đảm bảo hệ thống vận hành ổn định, không bị xung đột khi nhiều người cùng đấu giá một lúc. |
| **Đinh Hà Ngân**    | Xử lí Logic Nghiệp vụ (Backend) | - Khởi tạo và quản lý kết nối Server (ServerMain, AuctionServer).<br>- Xử lý luồng yêu cầu từ khách hàng (ClientHandler).<br>- Viết logic xác thực và nghiệp vụ đấu giá (AuthService, AuctionService, BidService). | Xây dựng "bộ não" của hệ thống, quyết định quy tắc thắng/thua và đăng ký người dùng. |
| **Nguyễn Thị Thu**  | Xử lí Dữ liệu & Sự kiện | - Thiết kế cấu trúc dữ liệu (server/model/*).<br>- Quản lý lưu trữ và xử lý ngoại lệ (storage/*, exception/*).<br>- Cài đặt thông báo thời gian thực (observer/*) và bộ đếm giờ kết thúc (SchedulerService). | Đảm bảo dữ liệu được lưu trữ an toàn và thông tin đấu giá được cập nhật ngay lập tức đến mọi người dùng. |
| **Bùi Hà Bảo Ngọc** | Xây dựng Giao diện (Frontend) | - Phát triển ứng dụng phía khách hàng (client/*).<br>- Thiết kế layout và bố cục màn hình (fxml/*).<br>- Chỉnh sửa thẩm mỹ và hiệu ứng người dùng (css/*). | Tạo ra giao diện trực quan, dễ sử dụng, giúp người dùng tương tác mượt mà với Server. |


## 🛠 Công nghệ sử dụng
* **Ngôn ngữ chính:** Java 
* **Giao diện:** JavaFX & CSS
* **Kiến trúc:** Client-Server (Socket Programming)
* **Design Patterns:** Observer, Singleton, 
* **Quản lý luồng:** Java Multi-threading (ThreadPool)


