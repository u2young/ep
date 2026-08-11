package xyz.herz.ep.erp.core;

import xyz.erupt.annotation.fun.DataProxy;

/**
 * 余额/流水 DataProxy:任何手工新增/编辑/删除都直接拒绝。
 * 库存只能通过 {@link ErpStockService#change(InventoryChangeFacade.ChangeRequest)} 统一改。
 */
public class ErpStockBalanceProxy implements DataProxy<Object> {

    private static final String ERR = "库存余额 / 流水不允许直接在表单里修改,请通过对应单据(采购入库/销售出库/其他出入库等)审核后自动变更。";

    @Override public void beforeAdd(Object o) { throw new IllegalStateException(ERR); }
    @Override public void beforeUpdate(Object o) { throw new IllegalStateException(ERR); }
    @Override public void beforeDelete(Object o) { throw new IllegalStateException(ERR); }
}
