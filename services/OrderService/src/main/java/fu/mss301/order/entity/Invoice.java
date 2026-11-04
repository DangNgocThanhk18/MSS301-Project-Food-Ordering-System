package fu.mss301.order.entity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "invoice_number", nullable = false, unique = true)
    private String invoiceNumber;

    @Column(name = "amount",precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method")
    private String paymentMethod; // CASH, CARD, MOMO...
    @Column(name = "status")
    private String status; // UNPAID, PAID, REFUNDED

    @Column(name = "issued_at")
    private LocalDateTime issuedAt = LocalDateTime.now();
}
