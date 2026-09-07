package xyz.herz.ep.pur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 采购管理模块独立测试启动类。
 * 扫描 pur(自包含:请购/询价/报价/收货/配置,无跨模块依赖)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.pur"})
@EntityScan(basePackages = {"xyz.herz.ep.pur"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.pur"})
@EruptScan({"xyz.herz.ep.pur"})
public class PurTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(PurTestApplication.class, args);
    }
}
