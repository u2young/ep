package xyz.herz.ep.ast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 资产管理模块独立测试启动类。
 * <p>扫描 ast + fin:
 * <ul>
 *   <li>ast:本模块实体/Repository/Handler/Calculator(资产/类别/位置/折旧/转移/维修)</li>
 *   <li>fin:FinPostingFacade 实现 FinPostingService(折旧 GL 过账 ASSET_DEPRECIATION/DISPOSAL)</li>
 * </ul>
 * 不依赖 ep-boot 层和 crm/erp 等业务模块:会计科目用 category.accountCode 快照,
 * 保管人用 custodianName 快照,不 REF UPMS,保持 ast 独立可测。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.ast", "xyz.herz.ep.fin"})
@EntityScan(basePackages = {"xyz.herz.ep.ast", "xyz.herz.ep.fin"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.ast", "xyz.herz.ep.fin"})
@EruptScan("xyz.herz.ep.ast")
public class AstTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(AstTestApplication.class, args);
    }
}
