package xyz.herz.ep.boot.print;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.jpa.model.BaseModel;

/**
 * 打印模板配置实体(自管,不依赖 erupt-print 原生 EruptPrintTpl)。
 * 为 ep-boot test 验收提供 3 模板 + 渲染服务 count/code 抽样依据。
 * <p>
 * ep-boot pom 已与各业务模块一致引入 lombok(provided,父 pom managed version)。
 */
@Getter @Setter
@Entity
@Table(name = "ep_print_template")
public class EruptPrintTemplate extends BaseModel {

    @Column(length = 80, nullable = false, unique = true)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(name = "biz_module", length = 40, nullable = false)
    private String bizModule;

    @Column(name = "tpl_key", length = 200)
    private String tplKey;

    @Column(length = 4000)
    private String remark;
}
