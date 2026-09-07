package xyz.herz.ep.proj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 项目管理模块独立测试启动类。
 * <p>扫描 proj + fin:
 * <ul>
 *   <li>proj:本模块实体/Repository/Handler(项目/任务/工时/费用/现金流/活动成本/模板)</li>
 *   <li>fin:FinPostingService(实现 FinPostingFacade,供 proj Handler 触发 GL 收入确认/费用入账)</li>
 * </ul>
 * 不依赖 ep-boot 层和其他业务模块(crm/erp/mfg 等)。
 * 客户/员工/成本中心用快照(id+name),不 REF 跨模块实体,保持 proj 独立可测。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.proj", "xyz.herz.ep.fin"})
@EntityScan(basePackages = {"xyz.herz.ep.proj", "xyz.herz.ep.fin"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.proj", "xyz.herz.ep.fin"})
@EruptScan("xyz.herz.ep.proj")
public class ProjTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProjTestApplication.class, args);
    }
}
