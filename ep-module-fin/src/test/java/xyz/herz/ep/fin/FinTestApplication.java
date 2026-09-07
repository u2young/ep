package xyz.herz.ep.fin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 财务模块独立测试启动类。
 * 仅扫描 fin 包,不依赖 ep-boot 层和其他业务模块(客户/供应商用 partyId+partyName 快照)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.fin"})
@EntityScan(basePackages = {"xyz.herz.ep.fin"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.fin"})
@EruptScan("xyz.herz.ep.fin")
public class FinTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinTestApplication.class, args);
    }
}
