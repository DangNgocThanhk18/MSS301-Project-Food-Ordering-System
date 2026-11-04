package fu.mss301.food.service;
import fu.mss301.food.dto.CategoryDTO;
import fu.mss301.food.entity.Category;
import fu.mss301.food.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryDTO findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));
        return toDTO(category);
    }

    // Hàm phụ để map Entity -> DTO
    private CategoryDTO toDTO(Category category) {
        return new CategoryDTO(category.getId(), category.getName(), category.getDescription());
    }

    // Cập nhật luôn hàm findAll() để trả về List<CategoryDTO>
    public List<CategoryDTO> findAll() {
        return categoryRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public Category create(Category category) {
        if (categoryRepository.existsByName(category.getName())) {
            throw new RuntimeException("Category name already exists: " + category.getName());
        }
        return categoryRepository.save(category);
    }

    public CategoryDTO update(Long id, CategoryDTO c) {
        // 1. Tìm Entity trong DB (dùng repository, không dùng hàm findById đã trả về DTO)
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        // 2. Cập nhật thông tin mới
        existing.setName(c.getName());
        existing.setDescription(c.getDescription());

        // 3. Lưu lại vào DB
        Category savedCategory = categoryRepository.save(existing);

        // 4. Trả về DTO
        return toDTO(savedCategory);
    }
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found: " + id));

        // Kiểm tra xem danh mục có món ăn nào không
        if (!category.getFoods().isEmpty()) {
            throw new RuntimeException("Không thể xóa danh mục đang chứa " + category.getFoods().size() + " món ăn. Vui lòng xóa hoặc chuyển các món ăn sang danh mục khác trước.");
        }
        categoryRepository.deleteById(id);
    }
}
