package xyz.herz.ep.erp.inventory;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统一库存变更门面。
 * <p>跨业务模块只能通过这个接口改库存(不要直接操作余额/流水表):
 * <ul>
 *     <li>Mall 销售发货/退款 → ERP/WMS 提供实现</li>
 *     <li>ERP 采购入库/销售出库/其他出入库/调拨/盘点 → 默认实现在 ep-module-erp</li>
 *     <li>WMS 库位库存确认 → ERP 仓库库存同步(按 Facade 调用)</li>
 * </ul>
 *
 * 规则:
 * <ul>
 *   <li>单个调用里的多个明细要么全成功要么全失败(事务)。</li>
 *   <li>出库时如果目标仓没有足够库存(数量+预扣)就抛 {@link InventoryShortageException},不要静默负数。</li>
 *   <li>相同 bizType+bizId 再次调用应该幂等:直接返回已有变更摘要,不重复改余额/写流水。</li>
 * </ul>
 */
public interface InventoryChangeFacade {

    /** 库存变动单 */
    record ChangeItem(
            Long productId,     // 产品
            Long warehouseId,   // 仓库
            BigDecimal qty      // 正=入库,负=出库
    ) { }

    /** 业务类型(对应流水 biz_type 字段)——统一放到 ERP 枚举里 */
    record ChangeRequest(
            String bizNo,            // 单据号(幂等键的一部分,可选空)
            Long bizId,              // 单据 ID
            int bizType,             // 枚举 ErpStockBizType.code
            List<ChangeItem> items,  // 一次请求里的所有明细
            String remark            // 备注,可选
    ) { }

    record ChangeResult(
            int affected,            // 成功处理的明细条数
            long recordFirstId       // 本次写入的第一条流水 ID(排查问题用)
    ) { }

    /**
     * 执行库存变更。余额与流水在同一事务里写,失败抛异常。
     *
     * @throws InventoryShortageException 任一条明细出库后余额<0 时抛出
     */
    ChangeResult change(ChangeRequest request);
}
