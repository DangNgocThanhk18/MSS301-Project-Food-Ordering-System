# Food Ordering System - Microservices

## 1. Project Objective

An online food ordering system built using **Microservices Architecture**, supporting:

- User management
- Food browsing
- Order creation

## 2. Team Members

| Member   | Responsibility                                  |
| -------- | ----------------------------------------------- |
| Huy      | API Gateway, Discovery Server, User Service     |
| Thành    | Food Service, Order Service                     |

Both members are responsible for **docs** and **README.md**.

## 3. General Architecture

```text
                         Client
                           |
                           v
                    +-------------+
                    | API Gateway |
                    |   :8080     |
                    +------+------+
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     User Service     Food Service     Order Service
        :8081            :8082            :8083
          |                |                |
       user_db           food_db          order_db
                       (Category,Food)   (Order,Invoice)
                           ^                |
                           |                |
                           +----- REST -----+
                              (OpenFeign)


                    +------------------+
                    | Eureka Discovery |
                    |      :8761       |
                    +------------------+
```

### Main Components

- **API Gateway** — Entry point for client requests, routes to services via Eureka.
- **Eureka Discovery** — Service registration and discovery.
- **User Service** — Manages users and accounts (`User`).
- **Food Service** — Manages categories and food (`Category`, `Food`).
- **Order Service** — Manages orders and invoices (`Order`, `Invoice`); calls REST to User & Food.
- **H2 / MySQL** — Stores data for each business service (separate DB per service).

## 4. How to Run Locally

### 4.1. Requirements

- **JDK 17** (`java -version` ≥ 17)
- **Maven 3.8+** (`mvn -version`)
- Free ports: **8761, 8080, 8081, 8082, 8083**
- (Optional) MySQL 8.x if you don't want to use the default H2 in-memory DB

### 4.2. Clone & Build

```bash
git clone <repo-url>
cd food-ordering-microservices

# Optional: build all modules at once (if parent pom exists)
mvn clean install -DskipTests
```

### 4.3. Start Services (in this order)

Open **5 terminals** and run each command in its own terminal:

**Terminal 1 — Discovery Server**
```bash
cd discovery-server
mvn spring-boot:run
```
→ Open http://localhost:8761 (Eureka Dashboard must load).

**Terminal 2 — API Gateway**
```bash
cd api-gateway
mvn spring-boot:run
```

**Terminal 3 — User Service**
```bash
cd services/service-user
mvn spring-boot:run
```

**Terminal 4 — Food Service**
```bash
cd services/service-food
mvn spring-boot:run
```

**Terminal 5 — Order Service**
```bash
cd services/service-order
mvn spring-boot:run
```

Wait ~30 seconds, then open **http://localhost:8761** — you should see **4 instances** registered:

```
API-GATEWAY
USER-SERVICE
FOOD-SERVICE
ORDER-SERVICE
```

### 4.4. Quick Health Check

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# List Gateway routes
curl http://localhost:8080/actuator/gateway/routes

# Services through Gateway
curl http://localhost:8080/api/users
curl http://localhost:8080/api/foods
curl http://localhost:8080/api/orders
```

### 4.5. Demo End-to-End (through API Gateway :8080)

```bash
# 1) Create user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","phone":"0901234567","address":"HCM"}'

# 2) Create category
curl -X POST http://localhost:8080/api/categories \
  -H "Content-Type: application/json" \
  -d '{"name":"Noodles","description":"Món nước"}'

# 3) Create food in category 1
curl -X POST "http://localhost:8080/api/foods?categoryId=1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Pho Bo","description":"Beef noodle","price":50000}'

# 4) Create order (Order Service calls REST to User + Food via Feign)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId":1,"foodId":1,"quantity":2}'

# 5) Create invoice for order 1
curl -X POST "http://localhost:8080/api/invoices?orderId=1&paymentMethod=CARD"

# 6) List all orders
curl http://localhost:8080/api/orders
```

**Expected result after step 5:**
- Order 1: `status = "PAID"`, `totalPrice = 100000`
- Invoice 1: `amount = 100000`, `status = "PAID"`

### 4.6. (Optional) Switch to MySQL

By default each service uses **H2 in-memory**. To use MySQL, in each service's `src/main/resources/application.properties`, replace:

```properties
spring.datasource.url=jdbc:h2:mem:<dbname>;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

with:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/<dbname>?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=123456
```

And add `mysql-connector-j` dependency to that service's `pom.xml`.

DB names per service:

| Service       | DB name    |
| ------------- | ---------- |
| User Service  | `user_db`  |
| Food Service  | `food_db`  |
| Order Service | `order_db` |

### 4.7. Stop All Services

Press `Ctrl+C` in each of the 5 terminals.

## 5. Technologies

| Component            | Version                |
| -------------------- | ---------------------- |
| Java                 | 17                     |
| Spring Boot          | 3.2.5                  |
| Spring Cloud         | 2023.0.3               |
| Eureka Server/Client | from Spring Cloud BOM  |
| Spring Cloud Gateway | from Spring Cloud BOM  |
| OpenFeign            | from Spring Cloud BOM  |
| Resilience4j         | 2.2.0                  |
| Database             | H2 (default) / MySQL   |
| Build Tool           | Maven                  |

## 6. Project Structure

```text
food-ordering-microservices/
├── discovery-server/
├── api-gateway/
├── services/
│   ├── service-user/
│   ├── service-food/
│   └── service-order/
├── docs/
│   ├── request-flow.md
│   └── screenshots/
└── README.md
```
