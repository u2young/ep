package xyz.herz.ep.boot.report;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import xyz.erupt.jpa.model.BaseModel;

/**
 * 报表配置实体(自管,不依赖 erupt-report 原生 Bi 模型)。
 * <p>
 * ep-boot pom 已与各业务模块一致引入 lombok(provided,父 pom managed version),
 * 故使用 @Getter/@Setter 替代手写访问器。
 */
@Getter @Setter
@Entity
@Table(name = "ep_report")
public class EruptReportEntity extends BaseModel {

    @Column(length = 80, nullable = false, unique = true)
    private String code;

    @Column(length = 200, nullable = false)
    private String name;

    @Column(name = "biz_module", length = 40, nullable = false)
    private String bizModule;

    @Column(name = "chart_type", length = 20, nullable = false)
    private String chartType;

    @Column(name = "sql_statement", length = 4000, nullable = false)
    private String sqlStatement;

    @Column(length = 500)
    private String remark;
}
