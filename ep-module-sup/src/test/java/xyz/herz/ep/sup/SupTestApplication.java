package xyz.herz.ep.sup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 服务支持模块独立测试启动类。
 * <p>扫描 sup + crm:
 * <ul>
 *   <li>sup:本模块实体/Repository/Handler(工单/SLA/优先级/分派/知识库/配置)</li>
 *   <li>crm:CrmCustomer(I-12 跨模块 REF:SupIssue.customer → CrmCustomer,optional 不强制 FK)</li>
 * </ul>
 * 不依赖 ep-boot 层和其他业务模块(fin/erp/mfg 等)。
 * 分派人/客服用快照(assigneeId+assigneeName),不 REF UPMS 用户实体,保持 sup 独立可测。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.sup", "xyz.herz.ep.crm"})
@EntityScan(basePackages = {"xyz.herz.ep.sup", "xyz.herz.ep.crm"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.sup", "xyz.herz.ep.crm"})
@EruptScan("xyz.herz.ep.sup")
public class SupTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(SupTestApplication.class, args);
    }
}
