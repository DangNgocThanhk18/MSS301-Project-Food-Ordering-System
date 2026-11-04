package fu.mss301.order.service;

import fu.mss301.order.client.FoodClient;
import fu.mss301.order.client.UserClient;
import fu.mss301.order.dto.*;
import fu.mss301.order.entity.Order;
import fu.mss301.order.entity.OrderItem;
import fu.mss301.order.repository.InvoiceRepository;
import fu.mss301.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserClient userClient;
    private final FoodClient foodClient;
    private final InvoiceRepository invoiceRepository; //
    public OrderService(OrderRepository orderRepository, UserClient userClient, FoodClient foodClient, InvoiceRepository invoiceRepository) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
        this.foodClient = foodClient;
        this.invoiceRepository = invoiceRepository;
    }

    @CircuitBreaker(name = "default", fallbackMethod = "createOrderFallback")
    public Order createOrder(OrderRequest request) {
        // 1. Kiểm tra User
        UserDTO user = userClient.getUserById(request.getUserId());
        if (user == null) throw new RuntimeException("User not found");

        Order order = new Order();
        order.setUserId(user.getId());
        order.setStatus("CREATED");

        // 2. Gán thông tin giao hàng (Lấy từ OrderRequest)
        order.setShippingAddress(request.getShippingAddress());
        order.setPhone(request.getPhone());
        order.setNote(request.getNote());

        // 3. Duyệt danh sách món để tính tiền
        BigDecimal totalOrderPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be > 0");
            }

            FoodDTO food = foodClient.getFoodById(itemReq.getFoodId());
            if (food == null) throw new RuntimeException("Food not found: " + itemReq.getFoodId());

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setFoodId(food.getId());
            orderItem.setFoodName(food.getName());
            orderItem.setQuantity(itemReq.getQuantity());
            orderItem.setUnitPrice(food.getPrice());

            // Cộng dồn tiền
            BigDecimal itemTotal = food.getPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            totalOrderPrice = totalOrderPrice.add(itemTotal);

            orderItems.add(orderItem);
        }

        order.setTotalPrice(totalOrderPrice);
        order.setItems(orderItems);
        return orderRepository.save(order);
    }

    // Fallback khi Food hoặc User Service bị sập
    public Order createOrderFallback(OrderRequest request, Throwable t) {
        System.err.println("Circuit Breaker: " + t.getMessage());
        Order fallbackOrder = new Order();
        fallbackOrder.setUserId(request.getUserId());
        fallbackOrder.setTotalPrice(BigDecimal.ZERO);
        fallbackOrder.setStatus("PENDING_VERIFICATION"); // Cần Admin xử lý tay

        // Vẫn lưu thông tin giao hàng vào đơn tạm
        fallbackOrder.setShippingAddress(request.getShippingAddress());
        fallbackOrder.setPhone(request.getPhone());
        fallbackOrder.setNote(request.getNote());

        List<OrderItem> items = new ArrayList<>();
        for (OrderItemRequest itemReq : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(fallbackOrder);
            item.setFoodId(itemReq.getFoodId());
            item.setFoodName("Unavailable");
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(BigDecimal.ZERO); // Không có giá
            items.add(item);
        }
        fallbackOrder.setItems(items);
        return orderRepository.save(fallbackOrder);
    }

    public List<OrderDTO> findAll() {
        return orderRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<OrderDTO> findByUser(Long userId) {
        return orderRepository.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    public OrderDTO findById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        return toDTO(order);
    }

    // Hàm phụ để lấy Entity (Dùng cho VNPay)
    public Order findEntityById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
    }

    public OrderDTO updateStatus(Long id, String status) {
        Order o = findEntityById(id);
        o.setStatus(status);
        Order savedOrder = orderRepository.save(o);
        return toDTO(savedOrder);
    }
    // 1. Lấy danh sách đơn hàng chờ Shipper nhận (Trạng thái PAID, chưa có shipper)
    public List<OrderDTO> findAvailableOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> "PAID".equals(o.getStatus()) && o.getShipperId() == null)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // 2. Shipper nhận đơn
    public OrderDTO assignShipper(Long orderId, Long shipperId) {
        Order order = findEntityById(orderId);
        if (order.getShipperId() != null) {
            throw new RuntimeException("Đơn hàng này đã được shipper khác nhận.");
        }
        order.setShipperId(shipperId);
        order.setStatus("DELIVERING"); // Chuyển sang đang giao
        return toDTO(orderRepository.save(order));
    }

    // 3. Lấy danh sách đơn Shipper đang giao
    public List<OrderDTO> findByShipper(Long shipperId) {
        return orderRepository.findByShipperId(shipperId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    // Hàm chuyển đổi
    private OrderDTO toDTO(Order o) {
        OrderDTO dto = new OrderDTO();
        dto.setId(o.getId());
        dto.setUserId(o.getUserId());
        dto.setTotalPrice(o.getTotalPrice());
        dto.setStatus(o.getStatus());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setShippingAddress(o.getShippingAddress());
        dto.setPhone(o.getPhone());
        dto.setNote(o.getNote());
        dto.setShipperId(o.getShipperId());

        // LẤY PAYMENT METHOD TỪ INVOICE
        invoiceRepository.findByOrderId(o.getId()).ifPresent(inv -> {
            dto.setPaymentMethod(inv.getPaymentMethod());
        });

        if (o.getItems() != null) {
            dto.setItems(o.getItems().stream()
                    .map(item -> {
                        OrderItemDTO itemDto = new OrderItemDTO();
                        itemDto.setId(item.getId());
                        itemDto.setFoodId(item.getFoodId());
                        itemDto.setFoodName(item.getFoodName());
                        itemDto.setQuantity(item.getQuantity());
                        itemDto.setUnitPrice(item.getUnitPrice());
                        if (item.getUnitPrice() != null) {
                            itemDto.setSubTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                        }
                        return itemDto;
                    })
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
