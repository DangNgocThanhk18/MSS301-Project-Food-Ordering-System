package fu.mss301.order.service;

import fu.mss301.order.entity.Invoice;
import fu.mss301.order.entity.Order;
import fu.mss301.order.repository.InvoiceRepository;
import fu.mss301.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
    }

    public List<Invoice> findAll() { return invoiceRepository.findAll(); }

    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + id));
    }

    public Invoice findByOrderId(Long orderId) {
        return invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Invoice not found for order: " + orderId));
    }

    public Invoice createInvoice(Long orderId, String paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (invoiceRepository.findByOrderId(orderId).isPresent()) {
            throw new RuntimeException("Invoice already exists for order: " + orderId);
        }

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        invoice.setAmount(order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO);
        invoice.setPaymentMethod(paymentMethod != null ? paymentMethod : "CASH");
        invoice.setStatus("PAID");
        invoice.setIssuedAt(LocalDateTime.now());

        // Cập nhật order status
        order.setStatus("PAID");
        orderRepository.save(order);

        return invoiceRepository.save(invoice);
    }

    public Invoice updateStatus(Long id, String status) {
        Invoice inv = findById(id);
        inv.setStatus(status);
        return invoiceRepository.save(inv);
    }
}
