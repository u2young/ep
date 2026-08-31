package xyz.herz.ep.crm.entity;

import xyz.herz.ep.crm.core.CrmEnumChoiceFetchHandler;
import xyz.herz.ep.crm.core.CrmStateDataProxy;
import xyz.herz.ep.crm.handler.CrmContractAutoPlanButtonHandler;
import xyz.herz.ep.crm.handler.CrmContractEffectHandler;
import xyz.herz.ep.crm.handler.CrmContractVoidHandler;
import xyz.erupt.annotation.Erupt;
import xyz.erupt.annotation.EruptField;
import xyz.erupt.annotation.sub_erupt.Power;
import xyz.erupt.annotation.sub_erupt.RowOperation;
import xyz.erupt.annotation.sub_field.Edit;
import xyz.erupt.annotation.sub_field.EditType;
import xyz.erupt.annotation.sub_field.View;
import xyz.erupt.annotation.sub_field.sub_edit.ButtonType;
import xyz.erupt.annotation.sub_field.sub_edit.ChoiceType;
import xyz.erupt.annotation.sub_field.sub_edit.DateType;
import xyz.erupt.annotation.sub_field.sub_edit.NumberType;
import xyz.erupt.annotation.sub_field.sub_edit.Search;
import xyz.erupt.jpa.model.BaseModel;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

/**
 * 合同(P1 扩展)。
 *
 * <p>关联客户(必填)、商机(可空)。状态机:
 * <ul>
 *   <li>0 草稿(DRAFT)  — 新建默认,可编辑</li>
 *   <li>1 生效(EFFECTIVE) — 通过【生效】按钮置位,不可回退到草稿</li>
 *   <li>2 作废(VOID) — 通过【作废】按钮置位,终态不可逆</li>
 * </ul>
 *
 * <p>状态字段 {@code status} 通过 {@link ContractStateProxy} 锁定,禁止表单直接编辑;
 * 必须通过 {@code @RowOperation} 的「生效」「作废」按钮触发 Handler 变更。
 *
 * <p>合同生效后才能创建回款计划与回款记录(见 {@link CrmReceivablePlan} / {@link CrmReceivableRecord})。
 */
@Getter
@Setter
@Table(name = "crm_contract")
@Entity
@Erupt(
    name = "合同",
    dataProxy = CrmContract.ContractStateProxy.class,
    power = @Power(importable = true, export = true),
    rowOperation = {
        @RowOperation(
            code = "EFFECT", title = "生效", icon = "fa fa-check-circle",
            operationHandler = CrmContractEffectHandler.class
        ),
        @RowOperation(
            code = "VOID", title = "作废", icon = "fa fa-ban",
            operationHandler = CrmContractVoidHandler.class
        )
    }
)
public class CrmContract extends BaseModel {

    @EruptField(
        views = @View(title = "合同编号", sortable = true),
        edit = @Edit(title = "合同编号", notNull = true, search = @Search, desc = "全局唯一")
    )
    private String no;

    @EruptField(
        views = @View(title = "合同名称", sortable = true),
        edit = @Edit(title = "合同名称", notNull = true, search = @Search)
    )
    private String name;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @EruptField(
        views = @View(title = "客户", column = "name"),
        edit = @Edit(title = "客户", notNull = true)
    )
    private CrmCustomer customer;

    @ManyToOne
    @JoinColumn(name = "business_id")
    @EruptField(
        views = @View(title = "商机", column = "name"),
        edit = @Edit(title = "商机", desc = "可选,通常赢单商机才能关联合同")
    )
    private CrmBusiness business;

    @EruptField(
        views = @View(title = "合同金额", sortable = true),
        edit = @Edit(title = "合同金额", type = EditType.NUMBER, notNull = true)
    )
    private BigDecimal amount;

    @EruptField(
        views = @View(title = "签订日期", sortable = true),
        edit = @Edit(title = "签订日期", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE))
    )
    private LocalDate signedDate;

    @EruptField(
        views = @View(title = "开始日期", sortable = true),
        edit = @Edit(title = "开始日期", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE))
    )
    private LocalDate startDate;

    @EruptField(
        views = @View(title = "结束日期", sortable = true),
        edit = @Edit(title = "结束日期", type = EditType.DATE, dateType = @DateType(type = DateType.Type.DATE))
    )
    private LocalDate endDate;

    @EruptField(
        views = @View(title = "状态", sortable = true),
        edit = @Edit(
            title = "状态", type = EditType.CHOICE,
            desc = "通过【生效】/【作废】按钮变更,禁止表单直接编辑",
            choiceType = @ChoiceType(
                fetchHandler = CrmEnumChoiceFetchHandler.class,
                fetchHandlerParams = {"ContractStatus"}
            )
        )
    )
    private Integer status = 0;

    // =================== BUTTON 辅助输入字段(均 @Transient 不持久化) ===================

    /** 分几期生成回款计划。默认 3 期。 */
    @Transient
    @EruptField(
        views = @View(title = "自动分期-期数", show = false),
        edit = @Edit(
            title = "分几期",
            type = EditType.BUTTON,
            desc = "填写期数后点击右侧按钮,自动按合同金额均分生成 N 条回款计划",
            numberType = @NumberType(min = 1, max = 60),
            buttonType = @ButtonType(
                handler = CrmContractAutoPlanButtonHandler.class,
                icon = "fa fa-magic",
                confirm = "按当前填写的期数/起始日/间隔月,均分合同金额生成回款计划,确认继续?",
                style = "primary"
            )
        )
    )
    private Integer planPeriods = 3;

    /** 第一期计划回款起始日。默认今天。 */
    @Transient
    @EruptField(
        views = @View(title = "自动分期-起始日", show = false),
        edit = @Edit(
            title = "首期计划回款日",
            type = EditType.BUTTON,
            desc = "第一期的计划回款日期,后续各期按间隔月数递增",
            dateType = @DateType(type = DateType.Type.DATE),
            buttonType = @ButtonType(
                handler = CrmContractAutoPlanButtonHandler.class,
                icon = "fa fa-magic",
                style = "primary"
            )
        )
    )
    private LocalDate planStartDate = LocalDate.now();

    /** 每期间隔月数。默认 1 月。 */
    @Transient
    @EruptField(
        views = @View(title = "自动分期-间隔月", show = false),
        edit = @Edit(
            title = "每期间隔(月)",
            type = EditType.BUTTON,
            desc = "相邻两期计划回款日的间隔月数",
            numberType = @NumberType(min = 1, max = 60),
            buttonType = @ButtonType(
                handler = CrmContractAutoPlanButtonHandler.class,
                icon = "fa fa-magic",
                style = "primary"
            )
        )
    )
    private Integer planIntervalMonths = 1;

    @Lob
    @EruptField(views = @View(title = "备注"), edit = @Edit(title = "备注", type = EditType.TEXTAREA))
    private String remark;

    /**
     * 合同状态机 DataProxy:锁定 status 字段,禁止表单直接修改。
     * 只允许默认值 0(草稿)通过表单写入,其他状态变更必须走行按钮。
     */
    public static class ContractStateProxy extends CrmStateDataProxy<CrmContract> {
        @Override
        protected String stateFieldName() { return "status"; }

        @Override
        protected Object[] allowedDirectEditStatuses() { return new Object[]{0}; }

        @Override
        public void beforeUpdate(CrmContract target) {
            // 只允许 status=0(草稿) 在表单内出现;非 0 直接报错
            Integer s = target.getStatus();
            if (s != null && s != 0) {
                throw new IllegalArgumentException(
                    "合同状态 status 禁止表单直接修改,请使用【生效】/【作废】按钮变更。"
                );
            }
        }
    }
}
