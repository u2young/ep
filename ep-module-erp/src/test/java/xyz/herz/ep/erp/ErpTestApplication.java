package xyz.herz.ep.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * ERP 模块独立测试启动类。
 * 仅扫描 ERP + common 包，不依赖 ep-boot 层。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.erp", "xyz.herz.ep.common"})
@EntityScan(basePackages = {"xyz.herz.ep.erp", "xyz.herz.ep.common"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.erp", "xyz.herz.ep.common"})
@EruptScan("xyz.herz.ep.erp")
public class ErpTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErpTestApplication.class, args);
    }
}
