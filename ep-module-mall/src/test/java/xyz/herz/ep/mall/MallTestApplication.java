package xyz.herz.ep.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 商城模块独立测试启动类。
 * 仅扫描 mall + common 包,不依赖 ep-boot 层。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.mall", "xyz.herz.ep.common"})
@EntityScan(basePackages = {"xyz.herz.ep.mall", "xyz.herz.ep.common"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.mall", "xyz.herz.ep.common"})
@EruptScan("xyz.herz.ep.mall")
public class MallTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(MallTestApplication.class, args);
    }
}
