package fu.mss301.food.repository;
import fu.mss301.food.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByCategoryId(Long categoryId);
    List<Food> findByNameContainingIgnoreCase(String keyword);
    @Query("SELECT f.category.name, COUNT(f) FROM Food f GROUP BY f.category.name")
    List<Object[]> countFoodsGroupByCategory();
}
