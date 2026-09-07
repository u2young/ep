package xyz.herz.ep.mfg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import xyz.erupt.core.annotation.EruptScan;

/**
 * 制造模块独立测试启动类。
 * <p>扫描 mfg + erp + common:
 * <ul>
 *   <li>mfg:本模块实体/Repository/Handler</li>
 *   <li>erp:InventoryChangeFacade 实现(ErpStockService)+ ErpProduct/Warehouse/Supplier 实体/Repository</li>
 *   <li>common:共享基类(若存在)</li>
 * </ul>
 * 不依赖 ep-boot 层和其他业务模块(fin/proj/sup/ast)。
 */
@SpringBootApplication(scanBasePackages = {"xyz.herz.ep.mfg", "xyz.herz.ep.erp", "xyz.herz.ep.common"})
@EntityScan(basePackages = {"xyz.herz.ep.mfg", "xyz.herz.ep.erp", "xyz.herz.ep.common"})
@EnableJpaRepositories(basePackages = {"xyz.herz.ep.mfg", "xyz.herz.ep.erp", "xyz.herz.ep.common"})
@EruptScan("xyz.herz.ep.mfg")
public class MfgTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(MfgTestApplication.class, args);
    }
}
