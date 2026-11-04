package fu.mss301.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderItemDTO {
    private Long id;
    private Long foodId;
    private String foodName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subTotal; // quantity * unitPrice
}
