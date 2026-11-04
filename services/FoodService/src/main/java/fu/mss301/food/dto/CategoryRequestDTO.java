package fu.mss301.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequestDTO {
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 50, message = "Tên danh mục không vượt quá 50 ký tự")
    private String name;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;
}
