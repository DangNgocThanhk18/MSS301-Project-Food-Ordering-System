package fu.mss301.order.client;
import fu.mss301.order.dto.FoodDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "food-service")
public interface FoodClient {

    @GetMapping("/api/foods/{id}")
    FoodDTO getFoodById(@PathVariable("id") Long id);
}
