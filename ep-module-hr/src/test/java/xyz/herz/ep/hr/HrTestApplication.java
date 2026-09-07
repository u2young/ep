package xyz.herz.ep.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 人力资源模块独立测试启动类。
 * 仅扫描 hr 包,不依赖 ep-boot 层和其他业务模块(员工/部门用 REF 本模块实体,不跨模块)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.hr"})
@EntityScan(basePackages = {"xyz.herz.ep.hr"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.hr"})
@EruptScan("xyz.herz.ep.hr")
public class HrTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(HrTestApplication.class, args);
    }
}
