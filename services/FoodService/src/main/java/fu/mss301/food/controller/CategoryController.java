package fu.mss301.food.controller;

import fu.mss301.food.entity.Category;
import fu.mss301.food.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<Category> getAll() { return categoryService.findAll(); }

    @GetMapping("/{id}")
    public Category getById(@PathVariable Long id) { return categoryService.findById(id); }

    @PostMapping
    public Category create(@RequestBody Category c) { return categoryService.create(c); }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @RequestBody Category c) {
        return categoryService.update(id, c);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { categoryService.delete(id); }
}
