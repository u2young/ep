package xyz.herz.ep.mp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 公众号(MP)模块独立测试启动类。
 * 仅扫描 MP + common 包,不依赖 ep-boot 层。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.mp", "xyz.herz.ep.common"})
@EntityScan(basePackages = {"xyz.herz.ep.mp", "xyz.herz.ep.common"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.mp", "xyz.herz.ep.common"})
@EruptScan("xyz.herz.ep.mp")
public class MpTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(MpTestApplication.class, args);
    }
}
