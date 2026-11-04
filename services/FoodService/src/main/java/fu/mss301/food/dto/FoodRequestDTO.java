package fu.mss301.food.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class FoodRequestDTO {
    @NotBlank(message = "Tên món không được để trống")
    @Size(max = 100, message = "Tên món không vượt quá 100 ký tự")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    private String imageUrl;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;
}
