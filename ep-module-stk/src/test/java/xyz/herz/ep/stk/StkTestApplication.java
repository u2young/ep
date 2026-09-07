package xyz.herz.ep.stk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 库存管理模块独立测试启动类。
 * 扫描 stk(自包含:出入库/盘点/序列号/批次/配置,无跨模块依赖)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.stk"})
@EntityScan(basePackages = {"xyz.herz.ep.stk"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.stk"})
@EruptScan({"xyz.herz.ep.stk"})
public class StkTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(StkTestApplication.class, args);
    }
}
