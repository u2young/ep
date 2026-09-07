package xyz.herz.ep.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 应用层打包启动入口。
 * - 职责:启动类 + 配置 + 依赖装配(import 哪些业务 module);
 * - 不写业务代码,只做 Spring Boot 装配;
 * - @EruptScan 扫 xyz.herz.ep.** 就会把所有 module(crm/erp...)里的 Erupt 实体都识别进来;
 * - Spring 的 @EntityScan / @EnableJpaRepositories 同样扫全业务包,确保跨模块 Repository/Entity 全部装载。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = "xyz.herz.ep")
@EntityScan(basePackages = "xyz.herz.ep")
@EnableJpaRepositories(basePackages = "xyz.herz.ep")
@EruptScan("xyz.herz.ep")
public class EruptApplication {
    public static void main(String[] args) {
        SpringApplication.run(EruptApplication.class, args);
    }
}
