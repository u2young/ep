package xyz.herz.ep.boot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

@SpringBootTest(classes = EruptApplication.class,
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Rollback
class EruptApplicationTests {
    @Test
    void contextLoads() { }
}
