## 1. Mô tả bài toán

Auction System là hệ thống đấu giá trực tuyến được xây dựng theo mô hình Client - Server. Ứng dụng cho phép người dùng đăng ký tài khoản, đăng nhập, xem danh sách phiên đấu giá, đặt giá, quản lý ví tiền, nhận thông báo thời gian thực và theo dõi lịch sử đấu giá.

Hệ thống hỗ trợ nhiều vai trò người dùng:

- **Bidder**: người mua, tham gia đặt giá trong các phiên đấu giá.
- **Seller**: người bán, tạo và quản lý sản phẩm/phiên đấu giá.
- **Admin**: quản trị viên, quản lý người dùng và các phiên đấu giá trong hệ thống.


Phạm vi hệ thống bao gồm:

- Quản lý tài khoản người dùng.
- Quản lý sản phẩm đấu giá theo danh mục.
- Tạo, xem, tìm kiếm và quản lý phiên đấu giá.
- Đặt giá và cập nhật giá theo thời gian thực.
- Quản lý ví, nạp/rút tiền và liên kết tài khoản ngân hàng.
- Gửi thông báo khi có sự kiện đấu giá.
- Quản lý người dùng và phiên đấu giá dành cho admin.

## 2. Công nghệ sử dụng

- **Ngôn ngữ lập trình**: Java 17
- **Giao diện người dùng**: JavaFX, FXML, CSS
- **Kiến trúc**: Client - Server
- **Giao tiếp mạng**: Java Socket
- **Cơ sở dữ liệu**: SQLite
- **Quản lý thư viện/build**: Maven
- **Xử lý dữ liệu JSON**: Gson
- **Logging**: SLF4J
- **Kiểm thử**: JUnit, Mockito, H2, Testcontainers
- **Design Pattern sử dụng**:
    - Singleton
    - Observer
    - Repository
    - Service Layer
- **Xử lý đồng thời**:
    - Java Multi-threading
    - Thread Pool
    - Cơ chế đồng bộ khi nhiều người dùng đặt 

## 3. Yêu cầu cài đặt

Trước khi chạy chương trình, cần cài đặt:

- JDK 17 trở lên
- Maven hoặc sử dụng Maven Wrapper có sẵn trong project
- IDE hỗ trợ Java như IntelliJ IDEA, Eclipse hoặc VS Code
- Hệ điều hành Windows/Linux/macOS đều có thể chạy

Kiểm tra phiên bản Java:

```bash
java -version
Kiểm tra Maven Wrapper:
./mvnw -version
Trên Windows PowerShell:
.\mvnw.cmd -version

4. Cấu trúc thư mục
auction-system/
├── data/
│   └── auction.db
├── logs/
│   └── application.log
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── client/
│   │   │   │   ├── controller/
│   │   │   │   ├── exception/
│   │   │   │   ├── logic/
│   │   │   │   ├── network/
│   │   │   │   ├── service/
│   │   │   │   ├── ui/
│   │   │   │   ├── util/
│   │   │   │   ├── ClientLauncher.java
│   │   │   │   └── ClientMain.java
│   │   │   ├── common/
│   │   │   ├── navigation/
│   │   │   ├── server/
│   │   │   │   ├── concurrency/
│   │   │   │   ├── exception/
│   │   │   │   ├── handler/
│   │   │   │   ├── model/
│   │   │   │   ├── observer/
│   │   │   │   ├── repository/
│   │   │   │   ├── service/
│   │   │   │   ├── storage/
│   │   │   │   ├── AuctionServer.java
│   │   │   │   ├── ClientHandler.java
│   │   │   │   └── ServerMain.java
│   │   │   └── util/
│   │   └── resources/
│   │       ├── CSS/
│   │       └── fxml/
│   └── test/
│       └── java/
├── target/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md

5. Mô tả các module chính
client
Chứa toàn bộ phần giao diện và xử lý phía người dùng.
Các thành phần chính:
•
controller/: xử lý sự kiện giao diện JavaFX.
•
logic/: xử lý logic hiển thị và nghiệp vụ phía client.
•
network/: quản lý kết nối socket tới server.
•
service/: gửi request và nhận response từ server.
•
ui/: các thành phần giao diện tái sử dụng.
•
ClientMain.java: điểm khởi chạy giao diện client.
•
ClientLauncher.java: class launcher để đóng gói chạy bằng file .jar.
server
Chứa toàn bộ phần xử lý nghiệp vụ phía server.
Các thành phần chính:
•
AuctionServer.java: khởi động socket server.
•
ClientHandler.java: xử lý từng client kết nối tới server.
•
handler/: phân loại và xử lý message từ client.
•
service/: xử lý nghiệp vụ đấu giá, tài khoản, ví, thông báo.
•
repository/: thao tác dữ liệu với SQLite.
•
model/: các lớp mô hình như User, Auction, Bid, Item.
•
observer/: xử lý thông báo và sự kiện thời gian thực.
•
concurrency/: xử lý đa luồng và đặt giá đồng thời.
•
storage/: cấu hình database, khởi tạo schema, quản lý ảnh.
•
ServerMain.java: điểm khởi chạy server.
common
Chứa các lớp dùng chung giữa client và server:
•
Message.java
•
MessageType.java
•
Constants.java
•
UserRole.java
•
AuctionStatus.java
•
ItemCategory.java
resources
Chứa tài nguyên giao diện:
•
fxml/: các màn hình JavaFX.
•
CSS/: file CSS, font, ảnh giao diện.
data
Chứa cơ sở dữ liệu SQLite:
data/auction.db
Database sẽ được server tự khởi tạo schema khi chạy.
logs
Chứa file log của ứng dụng:
logs/application.log

6. Vị trí các file .jar
Sau khi build project bằng Maven:
.\mvnw.cmd clean package
Các file .jar sẽ nằm trong thư mục:
target/
Các file quan trọng:
target/auction-system-1.0-SNAPSHOT.jar
target/auction-system-1.0-SNAPSHOT-client.jar
Trong đó:
•
auction-system-1.0-SNAPSHOT.jar: file jar chính được Maven tạo ra.
•
auction-system-1.0-SNAPSHOT-client.jar: file jar client đã được đóng gói kèm dependencies, dùng để chạy giao diện client.

7. Hướng dẫn chạy chương trình
Bước 1: Build project
Trên Windows PowerShell:
.\mvnw.cmd clean package
Trên Linux/macOS:
./mvnw clean package
Bước 2: Chạy Server
Server phải được chạy trước Client.
Cách 1: Chạy bằng Maven
.\mvnw.cmd exec:java -Dexec.mainClass="server.ServerMain"
Cách 2: Chạy trực tiếp trong IDE
Mở file:
src/main/java/server/ServerMain.java
Sau đó chọn Run ServerMain.
Khi server chạy thành công, hệ thống sẽ lắng nghe tại:
Host: localhost
Port: 5000
Thông tin cấu hình nằm trong:
src/main/java/common/Constants.java
Bước 3: Chạy Client
Sau khi server đã chạy, mở terminal mới và chạy client.
Cách 1: Chạy bằng Maven JavaFX
.\mvnw.cmd javafx:run
Cách 2: Chạy bằng file .jar
java -jar target\auction-system-1.0-SNAPSHOT-client.jar
Cách 3: Chạy trực tiếp trong IDE
Mở file:
src/main/java/client/ClientMain.java
Sau đó chọn Run ClientMain.

8. Tài khoản và dữ liệu ban đầu
Khi server khởi động, hệ thống sẽ:
•
Khởi tạo database schema nếu chưa tồn tại.
•
Tạo dữ liệu mặc định thông qua InitializeDataService.
•
Lưu dữ liệu vào SQLite tại:
data/auction.db
Nếu muốn reset dữ liệu, có thể xóa file:
data/auction.db
Sau đó chạy lại server để hệ thống tạo lại database.
9. Danh sách chức năng đã hoàn thành
Chức năng người dùng chung
•
Đăng ký tài khoản.
•
Đăng nhập.
•
Đăng xuất.
•
Cập nhật thông tin cá nhân.
•
Xem thông báo.
•
Quản lý ví cá nhân.
•
Nạp tiền vào ví.
•
Rút tiền khỏi ví.
•
Liên kết tài khoản ngân hàng.
•
Xem lịch sử giao dịch.
Chức năng Bidder
•
Xem danh sách phiên đấu giá.
•
Xem chi tiết phiên đấu giá.
•
Tìm kiếm sản phẩm đấu giá.
•
Lọc sản phẩm theo danh mục.
•
Đặt giá trong phiên đấu giá.
•
Xem lịch sử đấu giá đã tham gia.
•
Nhận thông báo khi bị vượt giá.
•
Nhận thông báo khi phiên đấu giá kết thúc.
•
Đăng ký trở thành Seller.
Chức năng Seller
•
Tạo sản phẩm đấu giá.
•
Thêm thông tin sản phẩm theo từng danh mục:
◦
Electronics
◦
Fashion
◦
Jewelry
◦
Vehicle
◦
Art/Collectibles
◦
Other
•
Upload ảnh sản phẩm.
•
Quản lý các phiên đấu giá của mình.
•
Xem chi tiết phiên đấu giá.
•
Xem lịch sử đặt giá của từng phiên.
•
Xem thông tin người thắng đấu giá.
•
Xem thống kê hoạt động bán hàng.
•
Nhận thông báo liên quan đến phiên đấu giá.
Chức năng Admin
•
Xem trang quản trị.
•
Quản lý danh sách người dùng.
•
Khóa/mở khóa tài khoản người dùng.
•
Quản lý các phiên đấu giá.
•
Xóa phiên đấu giá vi phạm.
•
Theo dõi trạng thái hoạt động của hệ thống.
Chức năng hệ thống
•
Giao tiếp Client - Server bằng Socket.
•
Xử lý nhiều client kết nối đồng thời.
•
Đặt giá đồng thời bằng cơ chế thread-safe.
•
Cập nhật giá realtime cho các client.
•
Gửi thông báo realtime.
•
Tự động cập nhật trạng thái phiên đấu giá.
•
Lưu trữ dữ liệu bằng SQLite.
•
Ghi log hoạt động hệ thống.
•
Kiểm thử bằng unit test và integration test.
10. Kiểm thử
Chạy toàn bộ test:
.\mvnw.cmd test
Hoặc trên Linux/macOS:
./mvnw test
Project có các nhóm test chính:
•
Test controller phía client.
•
Test service phía client.
•
Test network/message handler.
•
Test service phía server.
•
Test repository.
•
Test model.
•
Test xử lý đồng thời khi đặt giá.
•
Test nghiệp vụ ví và hoàn tiền.
•
Test utility/common classes.









12. Link báo cáo và video demo
•
Báo cáo PDF: Cập nhật link báo cáo tại đây
•
Video demo: Cập nhật link video demo tại đây


13. Ghi chú khi chạy
•
Luôn chạy Server trước Client.
•
Server mặc định chạy tại localhost:5000.
•
Nếu Client không kết nối được, kiểm tra Server đã chạy chưa.
•
Nếu muốn chạy nhiều Client, mở nhiều terminal hoặc chạy nhiều instance Client trong IDE.
•
Nếu lỗi database, kiểm tra thư mục data/ và file auction.db.
•
Nếu lỗi JavaFX, kiểm tra JDK và dependencies Maven đã được tải đầy đủ chưa.
14. Tác giả
Bài tập lớn môn Lập trình nâng cao.
Đề tài: Hệ thống đấu giá trực tuyến - Auction System.