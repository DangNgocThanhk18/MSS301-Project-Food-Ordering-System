package fu.mss301.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class LocalProfileApplicationTests {

    @Test
    void contextLoadsWithEmbeddedDatabase() {
    }
}
