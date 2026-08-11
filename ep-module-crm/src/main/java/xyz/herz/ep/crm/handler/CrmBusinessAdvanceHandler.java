package xyz.herz.ep.crm.handler;

import xyz.herz.ep.crm.entity.CrmBusiness;
import xyz.herz.ep.crm.entity.CrmBusinessStatus;
import xyz.herz.ep.crm.jpa.CrmBusinessRepository;
import xyz.herz.ep.crm.jpa.CrmBusinessStatusRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import xyz.erupt.annotation.fun.OperationHandler;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商机推进/回退阶段。
 * 实现:拉取 typeId 下所有 CrmBusinessStatus,按 sort 排好序,找到当前 statusId 的 index,index ±1 得到下一/上一阶段。
 * endStatus 非 null(已结束)的商机,拒绝推进/回退。
 */
@Component
public class CrmBusinessAdvanceHandler implements OperationHandler<CrmBusiness, String> {

    @Resource private CrmBusinessRepository businessRepo;
    @Resource private CrmBusinessStatusRepository statusRepo;

    @Override
    @Transactional
    public String exec(List<CrmBusiness> data, String param, String[] eruptParams) {
        boolean forward = eruptParams == null || eruptParams.length == 0 || "FORWARD".equalsIgnoreCase(eruptParams[0]);
        int ok = 0; StringBuilder sb = new StringBuilder();
        Map<Long, List<CrmBusinessStatus>> cache = new java.util.HashMap<>();
        for (CrmBusiness b : data) {
            try {
                if (b.getEndStatus() != null) { sb.append("商机[").append(b.getName()).append("] 已结束,拒绝推进/回退\n"); continue; }
                if (b.getStatusTypeId() == null) { sb.append("商机[").append(b.getName()).append("] 未设置状态组\n"); continue; }
                List<CrmBusinessStatus> list = cache.computeIfAbsent(b.getStatusTypeId(), statusRepo::findByTypeId);
                list.sort(Comparator.comparingInt(CrmBusinessStatus::getSort));
                int curIdx = findCurrentIndex(list, b.getStatusId());
                int targetIdx = forward ? curIdx + 1 : curIdx - 1;
                if (targetIdx < 0 || targetIdx >= list.size()) {
                    sb.append("商机[").append(b.getName()).append("] 已是").append(forward ? "最后" : "最前").append("阶段\n");
                    continue;
                }
                b.setStatusId(list.get(targetIdx).getId());
                businessRepo.save(b);
                ok++;
            } catch (Exception e) {
                sb.append("商机[").append(b == null ? "-" : b.getName())
                    .append("] 失败: ").append(e.getMessage()).append('\n');
            }
        }
        return (forward ? "推进" : "回退") + "完成:成功 " + ok + " 条。\n" + sb;
    }

    private int findCurrentIndex(List<CrmBusinessStatus> list, Long statusId) {
        if (statusId == null) return -1;
        for (int i = 0; i < list.size(); i++) if (statusId.equals(list.get(i).getId())) return i;
        return -1;
    }
}
