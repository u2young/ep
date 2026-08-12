package xyz.herz.ep.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.erupt.core.annotation.EruptScan;

/**
 * CRM 模块独立测试启动类。
 * 仅扫描 CRM + common 包，不依赖 ep-boot 层。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.crm", "xyz.herz.ep.common"})
@EntityScan(basePackages = {"xyz.herz.ep.crm", "xyz.herz.ep.common"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.crm", "xyz.herz.ep.common"})
@EruptScan("xyz.herz.ep.crm")
public class CrmTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(CrmTestApplication.class, args);
    }
}
