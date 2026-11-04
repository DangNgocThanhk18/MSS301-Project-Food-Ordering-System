# Hướng dẫn chạy bằng Docker

## Yêu cầu

- Docker Desktop (hoặc Docker Engine) và Docker Compose v2.
- Các cổng `8080`, `8081`, `8082`, `8083` và `8761` còn trống.
- Không cần cài JDK, Maven hoặc MySQL trên máy host.

MySQL chỉ chạy trong Docker network `food-net`; project không publish cổng MySQL ra host. Điều này tránh xung đột với MySQL hoặc các dự án Docker khác trên máy.

## Khởi động

Tại thư mục gốc của repository:

```powershell
docker compose up -d --build
docker compose ps
```

Lần chạy đầu có thể mất vài phút vì Docker tải image và Maven dependencies. Compose đợi health check theo thứ tự:

```text
MySQL + Discovery Server
        ↓
User Service + Food Service
        ↓
Order Service
        ↓
API Gateway
```

Kết quả của `docker compose ps` phải cho thấy sáu service đều `healthy`.

## Thành phần

| Service | Host port | Internal port | Vai trò |
| --- | ---: | ---: | --- |
| `api-gateway` | 8080 | 8080 | Điểm vào API |
| `discovery-server` | 8761 | 8761 | Eureka Discovery |
| `user-service` | 8081 | 8081 | User và authentication |
| `food-service` | 8082 | 8082 | Category và food |
| `order-service` | 8083 | 8083 | Order, invoice và Feign |
| `mysql` | Không public | 3306 | Ba database riêng cho service |

Trong Docker network, service dùng DNS nội bộ: `mysql` và `discovery-server`. Các biến môi trường trong `docker-compose.yml` ghi đè cấu hình local để kết nối đúng các tên này.

## Kiểm tra

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8082/actuator/health
Invoke-RestMethod http://localhost:8083/actuator/health
```

Mở http://localhost:8761 để xem bốn instance `API-GATEWAY`, `USER-SERVICE`, `FOOD-SERVICE` và `ORDER-SERVICE`.

Các API nghiệp vụ qua Gateway yêu cầu JWT Bearer token. Xem phần **Demo end-to-end qua Gateway** trong [README](../README.md) để chạy đăng ký, đăng nhập, tạo category/food, tạo order và invoice.

## Truy cập MySQL trong container

```powershell
docker compose exec mysql mysql -uroot -p123456
```

Database được tạo tự động khi các service kết nối:

- `user_mss301_db`
- `food_mss301_db`
- `order_mss301_db`

## Lệnh hữu ích

```powershell
docker compose logs -f order-service
docker compose restart food-service
docker compose stop food-service
docker compose up -d --build
docker compose down
docker compose down -v
```

`docker compose down -v` xoá volume MySQL, vì vậy toàn bộ dữ liệu demo sẽ bị mất.

## Xử lý sự cố

| Triệu chứng | Cách xử lý |
| --- | --- |
| Một cổng HTTP đã bị chiếm | Kiểm tra `docker ps` hoặc tiến trình đang dùng cổng, rồi dừng đúng ứng dụng đang chiếm cổng hoặc đổi port host của service đó. |
| Service chưa có trên Eureka | Chờ container thành `healthy`, sau đó xem `docker compose logs <service>`. |
| Gateway trả `401` | Gọi `/api/users/login`, lấy `token` và gửi `Authorization: Bearer <token>`. |
| Gateway trả `503` | Service đích chưa đăng ký Eureka xong hoặc đang không healthy. |
| Không kết nối được MySQL | Xem `docker compose logs mysql`; Compose tự đợi MySQL healthy trước khi khởi động business service. |
