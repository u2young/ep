package xyz.herz.ep.landing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 落地页模块独立测试启动类。
 * 仅扫描 landing 包,不依赖 ep-boot 层。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.landing"})
@EntityScan(basePackages = {"xyz.herz.ep.landing"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.landing"})
@EruptScan("xyz.herz.ep.landing")
public class LandingTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(LandingTestApplication.class, args);
    }
}
