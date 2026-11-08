# Food Ordering System — Microservices

Đồ án môn **MSS301 – Microservices with Spring Cloud & Docker**: hệ thống đặt món ăn trực tuyến xây dựng theo kiến trúc microservices.

## Mục tiêu

- Chia bài toán đặt món ăn thành các service độc lập: **User**, **Food**, **Order** (gồm hoá đơn và tích hợp thanh toán VNPay sandbox).
- Áp dụng Spring Cloud: **Service Discovery** (Eureka), **API Gateway** (Spring Cloud Gateway), gọi service-to-service bằng **OpenFeign** và chịu lỗi bằng **Resilience4j Circuit Breaker**.
- Mỗi business service có cơ sở dữ liệu riêng (*database per service*).
- Đóng gói bằng **Docker** và chạy toàn hệ thống bằng một lệnh `docker compose up`.

## Thành viên nhóm

| Thành viên | GitHub | Phụ trách |
| --- | --- | --- |
| Huy | `Lehuyy-MAt` | API Gateway, Discovery Server, User Service |
| Thành | `DangNgocThanhk18` | Food Service, Order Service (hoá đơn, VNPay) |
| Cả nhóm | | Tài liệu (`docs/`), README, Docker / Docker Compose |

## Kiến trúc tổng quát

![Kiến trúc hệ thống](docs/architecture.png)

- Client chỉ gọi **API Gateway** (`:8080`). Gateway xác thực JWT rồi chuyển request đến service đích theo tên đã đăng ký trên Eureka (`lb://USER-SERVICE`, `lb://FOOD-SERVICE`, `lb://ORDER-SERVICE`).
- **Order Service** gọi **User Service** (kiểm tra người đặt) và **Food Service** (lấy tên, giá món) bằng OpenFeign. Nếu một trong hai service lỗi, Circuit Breaker chuyển sang phương án dự phòng.
- Công nghệ: Java 17, Spring Boot 3.2.5, Spring Cloud 2023.0.3, Spring Data JPA, Spring Security (BCrypt), JWT (jjwt), MySQL 8 (Docker) / H2 (chạy local), Springdoc OpenAPI (Swagger UI), Docker Compose.

## Cổng dịch vụ

| Thành phần | Cổng trên máy host | Ghi chú |
| --- | ---: | --- |
| API Gateway | 8080 | Điểm truy cập API chính |
| Eureka Discovery | 8761 | Dashboard và service discovery |
| User Service | 8081 | Đăng ký, đăng nhập, người dùng |
| Food Service | 8082 | Danh mục và món ăn |
| Order Service | 8083 | Đơn hàng, hoá đơn, VNPay, shipper |
| MySQL | Không công khai | Chỉ dùng trong Docker network `food-net` (3 database: `user_mss301_db`, `food_mss301_db`, `order_mss301_db`) |

Swagger UI của từng service: `http://localhost:8081/swagger-ui.html`, `:8082/swagger-ui.html`, `:8083/swagger-ui.html`.

## Chạy nhanh bằng Docker

Yêu cầu Docker Desktop đang chạy. Đây là cách chạy khuyến nghị vì Docker tự tạo MySQL, chờ health check và khởi động các service theo đúng thứ tự (MySQL + Discovery → User + Food → Order → Gateway).

```powershell
docker compose up -d --build
docker compose ps
```

Kết quả mong đợi: cả sáu container có trạng thái `healthy`. Lần chạy đầu có thể mất vài phút do tải image và Maven dependencies.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
# { "status": "UP" }
```

Mở Eureka Dashboard tại http://localhost:8761. Bốn service được đăng ký là `API-GATEWAY`, `USER-SERVICE`, `FOOD-SERVICE` và `ORDER-SERVICE`.

MySQL không mở cổng ra máy host để tránh xung đột với các dự án khác. Khi cần truy cập DB trong container:

```powershell
docker compose exec mysql mysql -uroot -p123456
```

Hướng dẫn chi tiết (build, lệnh hữu ích, xử lý sự cố): [docs/HUONG-DAN-DOCKER.md](docs/HUONG-DAN-DOCKER.md).

### Dữ liệu mẫu (chỉ khi chạy Docker)

Profile `docker` tự nạp dữ liệu mẫu từ `data-docker.sql` của từng service: 4 danh mục, 12 món ăn, 6 người dùng và 4 đơn hàng mẫu. Mật khẩu của **tất cả** tài khoản mẫu là `password123`.

| Email | Vai trò | Ghi chú |
| --- | --- | --- |
| `admin@foodorder.com` | ADMIN | |
| `shipper@foodorder.com` | SHIPPER | id = 2, đang giao đơn mẫu #2 |
| `lan@example.com`, `khoa@example.com`, `ha@example.com` | CUSTOMER | id lần lượt 3, 4, 5 |
| `locked@example.com` | CUSTOMER | Tài khoản bị khoá — đăng nhập bị từ chối |

Khi chạy local bằng Maven (H2 in-memory) không có dữ liệu mẫu; hãy đăng ký tài khoản mới.

## Xác thực qua Gateway

User Service ký JWT (HS256, hiệu lực 1 giờ) khi đăng nhập. Gateway kiểm tra chữ ký và hạn của token, sau đó gắn các header `X-User-Id`, `X-User-Role`, `X-User-Email` cho service phía sau. Thiếu hoặc sai token → Gateway trả `401` (kèm header `X-Auth-Error`).

Các đường dẫn **không cần token** (Gateway so khớp đúng đường dẫn, không xét phương thức HTTP):

- `/api/users/register`, `/api/users/login`
- `/api/foods`, `/api/categories` (đường dẫn gốc; `/api/foods/{id}`, `/api/foods/search`, … vẫn cần token)
- `/actuator/health`

Mọi API còn lại cần header `Authorization: Bearer <token>`.

## Demo end-to-end qua Gateway

Chạy đoạn sau trong PowerShell sau khi các container đã `healthy`:

```powershell
$base = 'http://localhost:8080'
$suffix = Get-Random -Minimum 100000000 -Maximum 999999999
$email = "demo$suffix@example.com"
$phone = "0$suffix"

# 1. Register và login
$user = Invoke-RestMethod -Method Post -Uri "$base/api/users/register" `
  -ContentType 'application/json' `
  -Body (@{
    name = 'Demo User'; email = $email; phone = $phone
    address = 'Ho Chi Minh City'; password = 'password123'
  } | ConvertTo-Json)

$login = Invoke-RestMethod -Method Post -Uri "$base/api/users/login" `
  -ContentType 'application/json' `
  -Body (@{ email = $email; password = 'password123' } | ConvertTo-Json)
$headers = @{ Authorization = "Bearer $($login.token)" }

# 2. Category và food
$category = Invoke-RestMethod -Method Post -Uri "$base/api/categories" `
  -Headers $headers -ContentType 'application/json' `
  -Body (@{ name = "Noodles $suffix"; description = "Noodle category $suffix" } | ConvertTo-Json)

$food = Invoke-RestMethod -Method Post -Uri "$base/api/foods?categoryId=$($category.id)" `
  -Headers $headers -ContentType 'application/json' `
  -Body (@{ name = 'Pho Bo'; description = 'Beef noodle soup'; price = 50000 } | ConvertTo-Json)

# 3. Create order
$order = Invoke-RestMethod -Method Post -Uri "$base/api/orders" `
  -Headers $headers -ContentType 'application/json' `
  -Body (@{
    userId = $user.id; shippingAddress = 'Ho Chi Minh City'; phone = $phone
    note = 'No chili'; items = @(@{ foodId = $food.id; quantity = 2 })
  } | ConvertTo-Json -Depth 4)

# 4. Create invoice và kiểm tra order
$invoice = Invoke-RestMethod -Method Post `
  -Uri "$base/api/invoices?orderId=$($order.id)&paymentMethod=CARD" -Headers $headers
$orders = Invoke-RestMethod -Uri "$base/api/orders" -Headers $headers

$order.totalPrice       # 100000.00
$invoice.status          # PAID
($orders | Where-Object id -eq $order.id).status  # PAID
```

Kiểm tra xác thực: gọi một API cần token mà không gửi header sẽ nhận `401`.

```powershell
curl.exe -i "$base/api/orders"   # HTTP/1.1 401 Unauthorized
```

Kiểm tra Circuit Breaker: dừng Food Service rồi tạo đơn hàng; Order Service trả về đơn trạng thái `PENDING_VERIFICATION` với tổng tiền 0 (cần admin xử lý tay) thay vì báo lỗi.

```powershell
docker compose stop food-service
# gọi lại POST /api/orders như bước 3
docker compose start food-service
```

## Các API chính

Toàn bộ qua Gateway `http://localhost:8080`. Cột *Token* cho biết có cần JWT hay không.

| Service | Endpoint | Token | Mô tả |
| --- | --- | :---: | --- |
| User | `POST /api/users/register` | Không | Đăng ký (role mặc định `CUSTOMER`) |
| User | `POST /api/users/login` | Không | Đăng nhập, trả `{ "token": ... }` |
| User | `GET/POST /api/users`, `GET/PUT/DELETE /api/users/{id}` | Có | Quản lý người dùng (PUT có thể khoá/mở khoá qua `active`) |
| Food | `GET /api/categories`, `GET/POST/PUT/DELETE /api/categories[/{id}]` | `GET` gốc: Không | Danh mục |
| Food | `GET /api/foods` | Không | Danh sách món |
| Food | `GET /api/foods/{id}`, `/category/{categoryId}`, `/search?keyword=&categoryId=` | Có | Chi tiết, theo danh mục, tìm kiếm |
| Food | `POST /api/foods?categoryId=`, `PUT/DELETE /api/foods/{id}` | Có | Thêm, sửa, xoá món |
| Food | `GET /api/foods/stats/count`, `/stats/categories` | Có | Thống kê số món |
| Order | `POST /api/orders` | Có | Tạo đơn (gọi User + Food qua Feign) |
| Order | `GET /api/orders`, `/api/orders/{id}`, `/api/orders/user/{userId}` | Có | Xem đơn |
| Order | `PATCH /api/orders/{id}/status?status=` | Có | Cập nhật trạng thái |
| Order | `POST /api/orders/{id}/create-vnpay-url` | Có | Tạo URL thanh toán VNPay (sandbox) |
| Order | `GET /api/orders/available`, `PATCH /api/orders/{id}/assign-shipper?shipperId=`, `GET /api/orders/shipper/{shipperId}` | Có | Shipper xem đơn chờ, nhận đơn, xem đơn của mình |
| Order | `POST /api/invoices?orderId=&paymentMethod=` | Có | Tạo hoá đơn (mặc định `COD`), đơn chuyển `PAID` |
| Order | `GET /api/invoices`, `/api/invoices/{id}`, `/api/invoices/order/{orderId}`, `PATCH /api/invoices/{id}/status?status=` | Có | Xem, cập nhật hoá đơn |

Trạng thái đơn hàng đang dùng: `CREATED`, `PAID`, `DELIVERING`, `CANCELLED`, `PENDING_VERIFICATION`.

Mô tả chi tiết từng luồng request: [docs/MSS301_request-flow.docx](docs/MSS301_request-flow.docx).

## Chạy cục bộ bằng Maven

Ba business service mặc định dùng profile `local` với H2 in-memory, nên không cần cài hay chạy MySQL tại `localhost:3306`. Dữ liệu local sẽ được xoá khi service dừng. Nếu Docker Compose đang chạy, hãy chạy `docker compose down` trước để giải phóng các cổng 8080–8083.

Chạy năm terminal theo thứ tự sau (mỗi lệnh ở một terminal riêng, bắt đầu từ thư mục gốc của repository):

```powershell
# 1. Discovery
Set-Location discovery-server\DiscoveryServer
.\mvnw.cmd spring-boot:run

# 2. User Service
Set-Location services\UserService
.\mvnw.cmd spring-boot:run

# 3. Food Service
Set-Location services\FoodService
.\mvnw.cmd spring-boot:run

# 4. Order Service
Set-Location services\OrderService
.\mvnw.cmd spring-boot:run

# 5. Gateway
Set-Location api-gateway\ApiGatewayService
.\mvnw.cmd spring-boot:run
```

Yêu cầu JDK 17+; kiểm tra Maven Wrapper bằng `./mvnw.cmd -version` trong một thư mục service. Khi chạy local, H2 console có tại `http://localhost:<cổng-service>/h2-console` (JDBC URL: `jdbc:h2:mem:<tên>_mss301_db`, user `sa`, mật khẩu trống).

Các test hiện kiểm tra ứng dụng khởi động được (context load); business service dùng H2 in-memory qua profile `test`, nên không cần MySQL:

```powershell
$projects = @(
  'discovery-server\DiscoveryServer',
  'api-gateway\ApiGatewayService',
  'services\UserService',
  'services\FoodService',
  'services\OrderService'
)
foreach ($project in $projects) {
  Push-Location $project
  .\mvnw.cmd test
  Pop-Location
}
```

## Dừng hệ thống

```powershell
docker compose down       # Dừng container, giữ dữ liệu MySQL
docker compose down -v    # Dừng và xoá volume dữ liệu MySQL
```

## Cấu trúc thư mục

```text
.
├── api-gateway/ApiGatewayService        # Spring Cloud Gateway + JWT filter
├── discovery-server/DiscoveryServer     # Eureka Server
├── services/UserService                 # Đăng ký, đăng nhập, JWT
├── services/FoodService                 # Danh mục, món ăn
├── services/OrderService                # Đơn hàng, hoá đơn, VNPay, shipper
├── docker-compose.yml
└── docs/
    ├── MSS301_project_part_1.docx       # Phân tích, so sánh kiến trúc, sơ đồ, phân công
    ├── MSS301_request-flow.docx         # Mô tả luồng request
    ├── HUONG-DAN-DOCKER.md              # Hướng dẫn build & run bằng Docker
    ├── CAU-HOI-KY-THUAT.md              # Ngân hàng câu hỏi vấn đáp
    └── architecture.png                 # Sơ đồ kiến trúc
```

## Phạm vi hiện tại và hạn chế đã biết

Các chức năng đã mô tả trong phân tích giai đoạn 1 nhưng **chưa triển khai**: giỏ hàng (Cart), nhà hàng (Restaurant), voucher, đánh giá (Review), thông báo (Notification), Payment Service riêng. Phần thanh toán hiện nằm trong Order Service.

- Phân quyền theo vai trò (`ADMIN`/`CUSTOMER`/`SHIPPER`) chưa được áp dụng: Gateway chỉ xác thực token và truyền `X-User-Role`, các service chưa kiểm tra.
- Gateway so khớp đường dẫn công khai mà không xét phương thức HTTP, nên `POST /api/foods` và `POST /api/categories` (đường dẫn gốc) hiện không đòi token.
- `userId` của đơn hàng lấy từ body request, chưa đối chiếu với `X-User-Id` trong token.
- Hoá đơn được đặt `PAID` ngay khi tạo; VNPay mới dừng ở bước tạo URL thanh toán, chưa có callback xác minh kết quả ở backend.
- Khi Circuit Breaker kích hoạt, mọi lỗi (kể cả dữ liệu đầu vào không hợp lệ) đều dẫn đến đơn `PENDING_VERIFICATION`.
- JWT secret, mật khẩu MySQL và khoá VNPay sandbox đang đặt trực tiếp trong code/cấu hình (chỉ phù hợp demo; production cần biến môi trường hoặc secret manager).
- Các cổng 8081–8083 được publish ra host để demo/debug; production chỉ nên mở Gateway.
