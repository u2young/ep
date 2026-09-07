package xyz.herz.ep.sal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 销售管理模块独立测试启动类。
 * 扫描 sal(自包含:报价单/销售订单/送货单/销售员/销售伙伴,无跨模块依赖)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.sal"})
@EntityScan(basePackages = {"xyz.herz.ep.sal"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.sal"})
@EruptScan({"xyz.herz.ep.sal"})
public class SalTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SalTestApplication.class, args);
    }
}