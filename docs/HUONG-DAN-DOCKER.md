# Hướng dẫn build & run bằng Docker

## 1. Yêu cầu
- Docker Desktop (hoặc Docker Engine) + Docker Compose v2 (`docker compose version`)
- RAM trống ≥ 4 GB; các cổng rảnh: **8080, 8081, 8082, 8083, 8761, 3307**
- Không cần cài JDK/Maven/MySQL: mọi thứ được build và chạy trong container.

## 2. Kiến trúc triển khai

```text
                 localhost:8080
Client ───────────────▶ api-gateway ──┐
                                      │ lb://  (tra Eureka)
   localhost:8761 ──▶ discovery-server│
                                      ├──▶ user-service  :8081 ─┐
                                      ├──▶ food-service  :8082 ─┼─▶ mysql :3306
                                      └──▶ order-service :8083 ─┘   (3 DB riêng)
                                              │  OpenFeign
                                              └──▶ user-service, food-service
          Tất cả nằm trong Docker network: food-net
```

| Container | Image gốc | Port host:container | Ghi chú |
|---|---|---|---|
| mysql | mysql:8.0 | 3307:3306 | volume `mysql-data`, tự tạo 3 DB |
| discovery-server | build từ `discovery-server/DiscoveryServer` | 8761:8761 | Eureka Dashboard |
| user-service | `services/UserService` | 8081:8081 | `user_mss301_db` |
| food-service | `services/FoodService` | 8082:8082 | `food_mss301_db` |
| order-service | `services/OrderService` | 8083:8083 | `order_mss301_db`, gọi Feign |
| api-gateway | `api-gateway/ApiGatewayService` | 8080:8080 | cổng vào duy nhất |

## 3. Dockerfile (multi-stage)
Mỗi service dùng cùng một mẫu:
1. **Stage build**: `maven:3.9-eclipse-temurin-17` → `mvn package -DskipTests` ra file `.jar`.
2. **Stage runtime**: `eclipse-temurin:17-jre-alpine` chỉ chứa JRE + `.jar` (image nhỏ, không chứa source/Maven).

Copy `pom.xml` và chạy `dependency:go-offline` trước khi copy `src` để tận dụng **layer cache**: sửa code không phải tải lại thư viện.

## 4. Cấu hình trong Docker
Trong container, `localhost` là chính container đó, nên `application.properties` (đang trỏ `localhost`) được **ghi đè bằng biến môi trường** trong `docker-compose.yml` (relaxed binding của Spring Boot):

| Biến môi trường | Ghi đè property | Giá trị |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:mysql://mysql:3306/<db>?...` |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `eureka.client.service-url.defaultZone` | `http://discovery-server:8761/eureka/` |
| `EUREKA_INSTANCE_PREFER_IP_ADDRESS` | `eureka.instance.prefer-ip-address` | `true` |

`mysql`, `discovery-server`… là **tên service** – Docker DNS tự phân giải trong network `food-net`.
Thứ tự khởi động dùng `depends_on` + `healthcheck` (`service_healthy`):
`mysql, discovery-server → user, food → order → api-gateway`.

## 5. Build & Run (một lệnh)

```bash
git clone https://github.com/DangNgocThanhk18/MSS301-Project-Food-Ordering-System.git
cd MSS301-Project-Food-Ordering-System
docker-compose up --build
```
Lần đầu mất khoảng 5–10 phút (tải image + Maven). Các lần sau nhanh hơn nhờ cache.
Chạy nền: `docker-compose up -d --build`.

## 6. Kiểm tra
```bash
docker-compose ps                         # tất cả phải "healthy"
```
- Eureka: http://localhost:8761 → thấy `API-GATEWAY`, `USER-SERVICE`, `FOOD-SERVICE`, `ORDER-SERVICE`
- Gateway: `curl http://localhost:8080/actuator/health`
- Routes: `curl http://localhost:8080/actuator/gateway/routes`
- Swagger từng service: http://localhost:8081/swagger-ui.html (8082, 8083 tương tự)

## 7. Demo API qua Gateway (:8080)
```bash
# 1) Đăng ký user
curl -X POST http://localhost:8080/api/users/register -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","phone":"0901234567","address":"HCM","password":"123456"}'

# 2) Đăng nhập
curl -X POST http://localhost:8080/api/users/login -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"123456"}'

# 3) Tạo category
curl -X POST http://localhost:8080/api/categories -H "Content-Type: application/json" \
  -d '{"name":"Noodles","description":"Món nước"}'

# 4) Tạo món ăn (categoryId = 1)
curl -X POST http://localhost:8080/api/foods -H "Content-Type: application/json" \
  -d '{"name":"Pho Bo","description":"Beef noodle","price":50000,"categoryId":1}'

# 5) Tạo đơn hàng (Order Service gọi Feign sang User + Food)
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" \
  -d '{"userId":1,"shippingAddress":"HCM","phone":"0901234567","note":"it cay","items":[{"foodId":1,"quantity":2}]}'

# 6) Xem danh sách
curl http://localhost:8080/api/orders
```
Kết quả mong đợi: đơn có `totalPrice = 100000` (2 × 50000) → chứng minh Order Service gọi được User/Food qua Eureka + Feign.

## 8. Lệnh hay dùng
```bash
docker-compose logs -f order-service      # xem log 1 service
docker-compose restart food-service       # restart 1 service
docker-compose stop food-service          # giả lập sập service → test Circuit Breaker
docker-compose down                       # dừng, giữ dữ liệu
docker-compose down -v                    # dừng + xóa dữ liệu MySQL
docker-compose build --no-cache           # build lại từ đầu
```

## 9. Lỗi thường gặp
| Triệu chứng | Nguyên nhân / cách xử lý |
|---|---|
| `port is already allocated` | Cổng đang bị chiếm → tắt chương trình cũ (VD: chạy local bằng Maven) hoặc đổi port trái trong compose |
| Service không thấy trên Eureka | Chờ 30–60 s; kiểm tra `docker-compose logs <service>` |
| `503 Load balancer does not contain an instance` | Service đích chưa đăng ký xong, đợi thêm rồi thử lại |
| `Communications link failure` (MySQL) | MySQL chưa sẵn sàng; `docker-compose up` sẽ tự chờ nhờ healthcheck |
| Muốn build lại sau khi sửa code | `docker-compose up --build` |

## 10. Screenshot / video demo
Đặt vào `docs/screenshots/`: (1) `docker-compose ps`, (2) Eureka Dashboard có 4 service, (3) Postman/curl gọi qua Gateway, (4) Swagger.
