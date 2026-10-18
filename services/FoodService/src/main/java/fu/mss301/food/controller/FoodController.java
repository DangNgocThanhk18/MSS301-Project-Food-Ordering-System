package fu.mss301.food.controller;
import fu.mss301.food.dto.FoodDTO;
import fu.mss301.food.entity.Food;
import fu.mss301.food.service.FoodService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @GetMapping
    public List<FoodDTO> getAll() { return foodService.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<FoodDTO> getById(@PathVariable Long id) {
        return foodService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public List<FoodDTO> getByCategory(@PathVariable Long categoryId) {
        return foodService.findByCategory(categoryId);
    }

    @GetMapping("/search")
    public List<FoodDTO> search(@RequestParam String keyword) {
        return foodService.search(keyword);
    }

    @PostMapping
    public FoodDTO create(@RequestBody Food food,
                          @RequestParam(required = false) Long categoryId) {
        return foodService.create(food, categoryId);
    }

    @PutMapping("/{id}")
    public FoodDTO update(@PathVariable Long id, @RequestBody Food food,
                          @RequestParam(required = false) Long categoryId) {
        return foodService.update(id, food, categoryId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        foodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
