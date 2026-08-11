package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmCustomer;
import xyz.herz.ep.crm.jpa.CrmCustomerRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.List;

/**
 * 单独的公海认领 Handler(主要是 operationHandler class 不同用于 erupt 区别);实现委托到 TransferHandler。
 */
@Component
public class CrmCustomerClaimHandler implements OperationHandler<CrmCustomer, String> {

    @Resource private CrmCustomerTransferHandler delegate;

    @Override
    @Transactional
    public String exec(List<CrmCustomer> data, String param, String[] eruptParams) {
        return delegate.exec(data, param, new String[]{"CLAIM"});
    }
}
