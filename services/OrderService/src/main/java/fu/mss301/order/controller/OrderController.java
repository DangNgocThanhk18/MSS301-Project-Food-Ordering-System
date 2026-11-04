package fu.mss301.order.controller;

import fu.mss301.order.dto.OrderDTO;
import fu.mss301.order.dto.OrderRequest;
import fu.mss301.order.entity.Order;
import fu.mss301.order.service.OrderService;
import fu.mss301.order.service.VNPayService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final VNPayService vnPayService;

    public OrderController(OrderService orderService, VNPayService vnPayService) {
        this.orderService = orderService;
        this.vnPayService = vnPayService;
    }

    @GetMapping
    public List<OrderDTO> getAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(orderService.findById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}")
    public List<OrderDTO> getByUser(@PathVariable Long userId) {
        return orderService.findByUser(userId);
    }

    @PostMapping
    public Order create(@RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    @PatchMapping("/{id}/status")
    public OrderDTO updateStatus(@PathVariable Long id, @RequestParam String status) {
        return orderService.updateStatus(id, status);
    }
    // API cho Shipper
    @GetMapping("/available")
    public List<OrderDTO> getAvailableOrders() {
        return orderService.findAvailableOrders();
    }

    @PatchMapping("/{id}/assign-shipper")
    public OrderDTO assignShipper(@PathVariable Long id, @RequestParam Long shipperId) {
        return orderService.assignShipper(id, shipperId);
    }

    @GetMapping("/shipper/{shipperId}")
    public List<OrderDTO> getOrdersByShipper(@PathVariable Long shipperId) {
        return orderService.findByShipper(shipperId);
    }
    // SỬA: Dùng findEntityById thay vì findById
    @PostMapping("/{id}/create-vnpay-url")
    public Map<String, String> createVNPayUrl(@PathVariable Long id) {
        try {
            Order order = orderService.findEntityById(id); // <-- SỬA Ở ĐÂY
            String paymentUrl = vnPayService.createPaymentUrl(
                    order.getId(),
                    order.getTotalPrice().longValue(),
                    "Thanh toan don hang " + order.getId()
            );
            return Map.of("url", paymentUrl);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo URL VNPay: " + e.getMessage());
        }
    }
}