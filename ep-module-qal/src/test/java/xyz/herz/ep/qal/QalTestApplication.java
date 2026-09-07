package xyz.herz.ep.qal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 质量管理模块独立测试启动类。
 * 扫描 qal(自包含:参数标准/质检单/N/C/反馈,无跨模块依赖)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.qal"})
@EntityScan(basePackages = {"xyz.herz.ep.qal"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.qal"})
@EruptScan({"xyz.herz.ep.qal"})
public class QalTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(QalTestApplication.class, args);
    }
}
