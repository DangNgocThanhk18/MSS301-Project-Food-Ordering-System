package fu.mss301.food.service;
import fu.mss301.food.dto.FoodDTO;
import fu.mss301.food.entity.Category;
import fu.mss301.food.entity.Food;
import fu.mss301.food.repository.CategoryRepository;
import fu.mss301.food.repository.FoodRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final CategoryRepository categoryRepository;

    public FoodService(FoodRepository foodRepository, CategoryRepository categoryRepository) {
        this.foodRepository = foodRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<FoodDTO> findAll() {
        return foodRepository.findAll().stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public Optional<FoodDTO> findById(Long id) {
        return foodRepository.findById(id).map(this::toDTO);
    }

    public List<FoodDTO> findByCategory(Long categoryId) {
        return foodRepository.findByCategoryId(categoryId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public List<FoodDTO> search(String keyword) {
        return foodRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public FoodDTO create(Food food, Long categoryId) {
        if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
            food.setCategory(cat);
        }
        return toDTO(foodRepository.save(food));
    }

    public FoodDTO update(Long id, Food food, Long categoryId) {
        Food existing = foodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Food not found: " + id));
        existing.setName(food.getName());
        existing.setDescription(food.getDescription());
        existing.setPrice(food.getPrice());
        existing.setImageUrl(food.getImageUrl());
        if (categoryId != null) {
            Category cat = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
            existing.setCategory(cat);
        }
        return toDTO(foodRepository.save(existing));
    }

    public void delete(Long id) {
        if (!foodRepository.existsById(id)) {
            throw new RuntimeException("Food not found: " + id);
        }
        foodRepository.deleteById(id);
    }

    private FoodDTO toDTO(Food f) {
        return new FoodDTO(
                f.getId(), f.getName(), f.getDescription(), f.getPrice(), f.getImageUrl(),
                f.getCategory() != null ? f.getCategory().getId() : null,
                f.getCategory() != null ? f.getCategory().getName() : null
        );
    }
}
