# Ngân hàng câu hỏi kỹ thuật – Demo cuối kỳ (MSS301)

## A. Kiến trúc Microservices

**1. Microservices là gì? Khác Monolithic thế nào?**
Chia ứng dụng thành các service nhỏ, mỗi service làm một nghiệp vụ, có mã nguồn, DB và vòng đời triển khai riêng, giao tiếp qua mạng (REST/message). Monolithic là một khối duy nhất, một DB, deploy chung. Ở dự án: User, Food, Order là 3 service độc lập.

| | Monolithic | Microservices |
|---|---|---|
| Deploy | Cả khối | Từng service |
| Scale | Scale cả app | Scale riêng service nặng |
| Lỗi | 1 lỗi có thể sập cả hệ thống | Cô lập lỗi theo service |
| Công nghệ | Đồng nhất | Tự do từng service |
| Nhược điểm | Khó mở rộng, code phình to | Phức tạp: mạng, nhất quán dữ liệu, vận hành |

**2. Vì sao mỗi service một database (Database per Service)?**
Để các service độc lập (đổi schema không ảnh hưởng nhau), tránh coupling qua DB. Dự án: `user_mss301_db`, `food_mss301_db`, `order_mss301_db`. Cái giá phải trả: không có JOIN/transaction chéo service; Order phải gọi API để lấy thông tin User/Food, và lưu sẵn `foodName`, `unitPrice` trong `OrderItem` (snapshot) để đơn cũ không đổi khi giá món thay đổi.

**3. Các service giao tiếp với nhau thế nào?**
- Client → Gateway → service: HTTP REST.
- Service ↔ service: **đồng bộ** bằng OpenFeign (Order → User, Order → Food), địa chỉ lấy từ Eureka theo tên (`@FeignClient(name="food-service")`).
- Có thể mở rộng **bất đồng bộ** bằng message broker (Kafka/RabbitMQ) cho sự kiện như "đơn đã thanh toán".

**4. Nếu Food Service sập thì sao?**
Order Service dùng **Resilience4j Circuit Breaker** (`@CircuitBreaker(name="default", fallbackMethod="createOrderFallback")`). Sau khi tỉ lệ lỗi ≥ 50% trong 10 lần gọi gần nhất, mạch mở, gọi fallback ngay (không chờ timeout) và tạo đơn trạng thái `PENDING_VERIFICATION`. Sau 10 s chuyển half-open để thử lại. Demo: `docker-compose stop food-service` rồi tạo order.

**5. Vì sao cần API Gateway?**
Một điểm vào duy nhất: routing, xác thực JWT tập trung, CORS, load balancing, ẩn địa chỉ nội bộ. Client chỉ biết `:8080`, không cần biết service nằm ở đâu.

**6. Nhược điểm / thách thức của Microservices?**
Độ trễ mạng, nhất quán dữ liệu cuối cùng (eventual consistency), distributed transaction (cần Saga), debug/log phân tán (cần tracing như Zipkin), vận hành phức tạp (đó là lý do dùng Docker/Compose/K8s).

**7. Làm sao đảm bảo nhất quán dữ liệu giữa Order và Payment/Invoice?**
Không dùng transaction chung; dùng mẫu **Saga** (chuỗi giao dịch cục bộ + bù trừ). Dự án hiện tại đơn giản hóa bằng trạng thái đơn (`CREATED → PAID`...) và fallback.

## B. Vai trò của Spring Cloud

| Thành phần | Vai trò | Ở dự án |
|---|---|---|
| **Eureka Server** (Discovery) | "Danh bạ" dịch vụ: service đăng ký tên + IP:port, client tra cứu | `discovery-server` (:8761) |
| **Eureka Client** | Tự đăng ký + gửi heartbeat 30 s/lần | user/food/order/gateway |
| **Spring Cloud Gateway** | Routing theo predicate `Path=/api/users/**`, filter, CORS | `api-gateway` (:8080) |
| **Spring Cloud LoadBalancer** | Cân bằng tải client-side khi `lb://USER-SERVICE` có nhiều instance (round-robin) | Gateway + Feign |
| **OpenFeign** | Gọi REST bằng interface khai báo, thay RestTemplate | `UserClient`, `FoodClient` |
| **Resilience4j** | Circuit breaker, chịu lỗi | Order Service |

**Câu hỏi thường gặp**
- *Service Discovery giải quyết vấn đề gì?* IP/port container thay đổi liên tục (scale, restart) nên không hardcode được; gọi theo **tên logic**, Eureka trả địa chỉ hiện tại.
- *`lb://USER-SERVICE` nghĩa là gì?* `lb` = load-balanced; Gateway hỏi Eureka danh sách instance của `USER-SERVICE` rồi chọn một.
- *Thêm instance thứ 2 thì sao?* `docker-compose up -d --scale food-service=2` (bỏ `container_name` và port cố định của service đó) → Eureka có 2 instance, LoadBalancer chia tải. 
- *Eureka sập thì sao?* Client có cache registry cục bộ nên vẫn gọi được một thời gian; nhưng instance mới không đăng ký được. Production chạy cụm Eureka nhiều node.
- *Vì sao `prefer-ip-address=true`?* Để đăng ký bằng IP container thay vì hostname ngẫu nhiên, các service chắc chắn gọi được nhau.
- *Gateway khác Eureka thế nào?* Eureka chỉ lưu "ai ở đâu"; Gateway nhận request và chuyển tiếp dựa trên thông tin đó.
- *JWT chạy thế nào?* User Service ký token (HS256) khi login; Gateway verify chữ ký + hạn rồi truyền `X-User-Id`, `X-User-Role` xuống service; service tin Gateway (`permitAll`). Secret phải giống nhau ở hai nơi (production: để trong biến môi trường/secret).

## C. Triển khai bằng Docker

**1. Docker là gì? Khác máy ảo (VM)?**
Docker đóng gói app + môi trường chạy vào **container**, dùng chung kernel của host nên nhẹ, khởi động nhanh; VM có cả hệ điều hành riêng nên nặng hơn.

**2. Image vs Container?** Image là khuôn chỉ-đọc (build từ Dockerfile); container là thể hiện đang chạy của image.

**3. Vì sao dùng multi-stage build?**
Stage 1 dùng Maven+JDK để build; stage 2 chỉ chép file `.jar` vào JRE alpine → image nhỏ (~200 MB thay vì ~700 MB), không lộ source, an toàn hơn.

**4. Vì sao copy `pom.xml` trước `src`?** Tận dụng layer cache: dependencies chỉ tải lại khi `pom.xml` đổi.

**5. docker-compose để làm gì?** Mô tả và chạy nhiều container cùng lúc (services, network, volume, thứ tự) bằng một file YAML và một lệnh `docker-compose up`.

**6. Các service gọi nhau thế nào trong Docker?**
Compose tạo network `food-net`, có DNS nội bộ: gọi bằng **tên service** (`mysql`, `discovery-server`). Không dùng `localhost` vì trong container nó trỏ về chính container đó. Đây là lý do phải ghi đè URL Eureka và MySQL bằng biến môi trường.

**7. `ports` khác `expose`?** `ports: "8080:8080"` mở cổng ra máy host; chỉ các service cần truy cập từ ngoài (Gateway, Eureka dashboard) mới thật sự cần. Service nội bộ trong network vẫn gọi nhau được dù không publish port (production nên chỉ publish Gateway).

**8. `depends_on` có đảm bảo service sẵn sàng không?**
Mặc định chỉ đảm bảo thứ tự *khởi động*. Dự án dùng `condition: service_healthy` kết hợp `healthcheck` (actuator/health, mysqladmin ping) để chờ thật sự sẵn sàng.

**9. Dữ liệu MySQL có mất khi xóa container không?** Không, nhờ **volume** `mysql-data`. Chỉ mất khi `docker-compose down -v`.

**10. Cấu hình khác nhau giữa local và Docker xử lý sao?** Dùng biến môi trường (twelve-factor): code/jar giữ nguyên, môi trường quyết định cấu hình. Spring Boot tự map `SPRING_DATASOURCE_URL` → `spring.datasource.url`.

**11. Làm sao scale / lên production?** Scale bằng `--scale`; production dùng Kubernetes/Docker Swarm, secret manager, reverse proxy HTTPS, không publish cổng DB, CI/CD build image đẩy lên registry.

## D. Câu hỏi "bẫy" nên chuẩn bị
- *Vì sao Order lưu `foodName`, `unitPrice`?* Snapshot lịch sử + giảm phụ thuộc runtime.
- *Gateway có là single point of failure?* Có → chạy nhiều instance sau load balancer (Nginx/cloud LB).
- *Bảo mật thế nào khi service tin Gateway?* Service nội bộ không publish port ra ngoài, chỉ Gateway công khai; có thể thêm kiểm tra header/mTLS.
- *Vì sao 3 service dùng chung 1 MySQL container?* Tiết kiệm tài nguyên cho đồ án; vẫn là 3 database/schema tách biệt. Production nên tách instance DB.
