package fu.mss301.order.service;
import fu.mss301.order.client.FoodClient;
import fu.mss301.order.client.UserClient;
import fu.mss301.order.dto.FoodDTO;
import fu.mss301.order.dto.UserDTO;
import fu.mss301.order.entity.Order;
import fu.mss301.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final FoodClient foodClient;

    public OrderService(OrderRepository orderRepository,
                        UserClient userClient,
                        FoodClient foodClient) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.foodClient = foodClient;
    }

    @CircuitBreaker(name = "default", fallbackMethod = "createOrderFallback")
    public Order createOrder(Long userId, Long foodId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be > 0");
        }

        // Gọi REST sang USER-SERVICE
        UserDTO user = userClient.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found: " + userId);
        }

        // Gọi REST sang FOOD-SERVICE
        FoodDTO food = foodClient.getFoodById(foodId);
        if (food == null) {
            throw new RuntimeException("Food not found: " + foodId);
        }

        BigDecimal unitPrice = food.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        Order order = new Order();
        order.setUserId(user.getId());
        order.setFoodId(food.getId());
        order.setQuantity(quantity);
        order.setUnitPrice(unitPrice);
        order.setTotalPrice(totalPrice);
        order.setStatus("CREATED");

        return orderRepository.save(order);
    }

    // fallback khi circuit breaker OPEN
    public Order createOrderFallback(Long userId, Long foodId, Integer quantity, Throwable t) {
        throw new RuntimeException("Order creation failed (fallback). Reason: " + t.getMessage());
    }

    public List<Order> findAll() { return orderRepository.findAll(); }

    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public List<Order> findByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order updateStatus(Long id, String status) {
        Order o = findById(id);
        o.setStatus(status);
        return orderRepository.save(o);
    }

    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found: " + id);
        }
        orderRepository.deleteById(id);
    }
}
