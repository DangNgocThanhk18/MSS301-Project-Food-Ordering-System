package fu.mss301.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private Long userId;

    // THÔNG TIN GIAO HÀNG (Nằm ở đây mới đúng)
    private String shippingAddress;
    private String phone;
    private String note;

    // DANH SÁCH MÓN
    private List<OrderItemRequest> items;
}