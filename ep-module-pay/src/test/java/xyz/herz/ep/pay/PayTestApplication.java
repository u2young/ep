package xyz.herz.ep.pay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 薪酬管理模块独立测试启动类。
 * 扫描 pay + hr(员工 REF) + fin(GL 过账 FinPostingFacade)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.pay", "xyz.herz.ep.hr", "xyz.herz.ep.fin"})
@EntityScan(basePackages = {"xyz.herz.ep.pay", "xyz.herz.ep.hr", "xyz.herz.ep.fin"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.pay", "xyz.herz.ep.hr", "xyz.herz.ep.fin"})
@EruptScan({"xyz.herz.ep.pay", "xyz.herz.ep.hr", "xyz.herz.ep.fin"})
public class PayTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayTestApplication.class, args);
    }
}
