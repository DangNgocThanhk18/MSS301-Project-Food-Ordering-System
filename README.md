# Food Ordering System - Microservices

## 1. Project Objective

An online food ordering system built using **Microservices Architecture**, supporting:

* User management
* Food browsing
* Order creation
* Payment processing

## 2. Team Members

| Member   | Responsibility                                  |
| -------- | ----------------------------------------------- |
| Member 1 | User Service, Order Service, API Gateway        |
| Member 2 | Food Service, Payment Service, Discovery Server |

## 3. General Architecture

```text
                         Client
                           |
                           v
                    +-------------+
                    | API Gateway |
                    +------+------+
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
     User Service     Food Service     Order Service
          |                |                |
       MySQL             MySQL             MySQL
                                             |
                                             v
                                      Payment Service
                                             |
                                           MySQL


                    +------------------+
                    | Eureka Discovery |
                    +------------------+
```

### Main Components

* **API Gateway** — Entry point for client requests.
* **Eureka Discovery** — Service registration and discovery.
* **User Service** — Manages users and accounts.
* **Food Service** — Manages restaurants, food, and menu information.
* **Order Service** — Manages carts, orders, and order status.
* **Payment Service** — Handles payment requests and transactions.
* **MySQL** — Stores data for each business service.
