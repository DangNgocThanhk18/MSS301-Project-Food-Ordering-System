# Food Ordering System — Microservices

Hệ thống đặt món ăn trực tuyến gồm API Gateway, Eureka Discovery Server và ba business service: User, Food, Order. Mỗi business service có cơ sở dữ liệu riêng (MySQL khi chạy Docker, H2 in-memory khi chạy cục bộ) và Order Service gọi User/Food qua OpenFeign.

## Kiến trúc và cổng dịch vụ

| Thành phần | Cổng trên máy host | Ghi chú |
| --- | ---: | --- |
| API Gateway | 8080 | Điểm truy cập API chính |
| Eureka Discovery | 8761 | Dashboard và service discovery |
| User Service | 8081 | Đăng ký, đăng nhập, người dùng |
| Food Service | 8082 | Danh mục và món ăn |
| Order Service | 8083 | Đơn hàng và hoá đơn |
| MySQL | Không công khai | Chỉ dùng trong Docker network `food-net` |

## Chạy nhanh bằng Docker

Yêu cầu Docker Desktop đang chạy. Đây là cách chạy khuyến nghị vì Docker tự tạo MySQL, chờ health check và khởi động các service theo đúng thứ tự.

```powershell
docker compose up -d --build
docker compose ps
```

Kết quả mong đợi: cả sáu container có trạng thái `healthy`.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
# { "status": "UP" }
```

Mở Eureka Dashboard tại http://localhost:8761. Bốn service được đăng ký là `API-GATEWAY`, `USER-SERVICE`, `FOOD-SERVICE` và `ORDER-SERVICE`.

MySQL không mở cổng ra máy host để tránh xung đột với các dự án khác. Khi cần truy cập DB trong container:

```powershell
docker compose exec mysql mysql -uroot -p123456
```

## Demo end-to-end qua Gateway

Chỉ hai endpoint `/api/users/register` và `/api/users/login` cùng health check là công khai. Các API nghiệp vụ khác cần header `Authorization: Bearer <token>`.

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

## Chạy cục bộ bằng Maven

Ba business service mặc định dùng profile `local` với H2 in-memory, nên không cần cài hay chạy MySQL tại `localhost:3306`. Dữ liệu local sẽ được xoá khi service dừng. Nếu Docker Compose đang chạy, hãy chạy `docker compose down` trước để giải phóng các cổng 8080–8083.

Chạy năm terminal theo thứ tự sau:

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

Yêu cầu JDK 17+; kiểm tra Maven Wrapper bằng `./mvnw.cmd -version` trong một thư mục service. Các test của business service dùng H2 in-memory qua profile `test`, nên không cần MySQL:

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
├── api-gateway/ApiGatewayService
├── discovery-server/DiscoveryServer
├── services/UserService
├── services/FoodService
├── services/OrderService
├── docker-compose.yml
└── docs/
```
