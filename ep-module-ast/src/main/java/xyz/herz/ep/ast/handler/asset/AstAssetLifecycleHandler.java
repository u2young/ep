package xyz.herz.ep.ast.handler.asset;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import xyz.erupt.annotation.fun.OperationHandler;
import xyz.herz.ep.ast.core.AstDepreciationCalculator;
import xyz.herz.ep.ast.entity.asset.AstAsset;
import xyz.herz.ep.ast.entity.category.AstAssetCategory;
import xyz.herz.ep.ast.enums.AstDictEnums.AssetStatus;
import xyz.herz.ep.fin.enums.FinDictEnums.JournalSourceType;
import xyz.herz.ep.fin.facade.FinPostingFacade;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 资产生命周期行按钮处理器(提交/计提折旧/出售/报废,四合一)。
 * <p>状态迁移:
 * <ul>
 *   <li>COMMIT:DRAFT(0) → AVAILABLE(1),初始化 currentValue=purchaseAmount、累计折旧=0。</li>
 *   <li>DEPRECIATE:AVAILABLE/PARTIALLY_DEPRECIATED → 计提一期折旧,
 *       委托 {@link AstDepreciationCalculator}(计算+过账 ASSET_DEPRECIATION)。</li>
 *   <li>SELL:非终态 → SOLD(4),GL 过账 ASSET_DISPOSAL 转出固定资产。</li>
 *   <li>SCRAP:非终态 → SCRAPPED(5),GL 过账 ASSET_DISPOSAL 报废转出。</li>
 * </ul>
 */
@Component
public class AstAssetLifecycleHandler implements OperationHandler<Object, Object> {

    public static final String CODE_COMMIT = "ast.asset.commit";
    public static final String CODE_DEPRECIATE = "ast.asset.depreciate";
    public static final String CODE_SELL = "ast.asset.sell";
    public static final String CODE_SCRAP = "ast.asset.scrap";

    @PersistenceContext private EntityManager em;
    private final AstDepreciationCalculator depreciationCalculator;

    @Autowired(required = false)
    private FinPostingFacade postingFacade;

    public AstAssetLifecycleHandler(AstDepreciationCalculator depreciationCalculator) {
        this.depreciationCalculator = depreciationCalculator;
    }

    @Override
    @Transactional
    public String exec(List<Object> data, Object formValue, String[] eruptParams) {
        String code = (eruptParams != null && eruptParams.length > 0) ? eruptParams[0] : CODE_COMMIT;
        int ok = 0, fail = 0;
        StringBuilder sb = new StringBuilder();
        for (Object row : data) {
            if (!(row instanceof AstAsset doc)) {
                fail++; sb.append("仅支持固定资产; "); continue;
            }
            try {
                switch (code) {
                    case CODE_COMMIT -> applyCommit(doc);
                    case CODE_DEPRECIATE -> applyDepreciate(doc);
                    case CODE_SELL -> applySell(doc);
                    case CODE_SCRAP -> applyScrap(doc);
                    default -> throw new IllegalStateException("未知操作码: " + code);
                }
                em.merge(doc);
                ok++;
            } catch (Exception ex) {
                fail++;
                sb.append("资产#").append(doc.getId()).append(": ").append(ex.getMessage()).append("; ");
            }
        }
        String head = String.format("成功 %d 失败 %d (code=%s)", ok, fail, code);
        return fail == 0 ? head : head + " ❌ " + sb;
    }

    private void applyCommit(AstAsset doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st != AssetStatus.DRAFT.code) {
            throw new IllegalStateException("只有草稿状态可提交,当前状态=" + st);
        }
        doc.setStatus(AssetStatus.AVAILABLE.code);
        doc.setCurrentValue(doc.getPurchaseAmount());
        if (doc.getTotalDepreciation() == null) doc.setTotalDepreciation(BigDecimal.ZERO);
        if (doc.getDepreciatedPeriods() == null) doc.setDepreciatedPeriods(0);
        if (doc.getSalvageValue() == null) doc.setSalvageValue(BigDecimal.ZERO);
    }

    private void applyDepreciate(AstAsset doc) {
        depreciationCalculator.applyDepreciation(doc, LocalDate.now());
    }

    private void applySell(AstAsset doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == AssetStatus.DRAFT.code || st == AssetStatus.SOLD.code
                || st == AssetStatus.SCRAPPED.code) {
            throw new IllegalStateException("草稿/已出售/已报废不可出售,当前状态=" + st);
        }
        postDisposalGl(doc, "出售");
        doc.setStatus(AssetStatus.SOLD.code);
    }

    private void applyScrap(AstAsset doc) {
        int st = doc.getStatus() != null ? doc.getStatus() : -1;
        if (st == AssetStatus.DRAFT.code || st == AssetStatus.SOLD.code
                || st == AssetStatus.SCRAPPED.code) {
            throw new IllegalStateException("草稿/已出售/已报废不可报废,当前状态=" + st);
        }
        postDisposalGl(doc, "报废");
        doc.setStatus(AssetStatus.SCRAPPED.code);
    }

    /**
     * 处置 GL 过账(ASSET_DISPOSAL)。
     * 借:累计折旧 totalDep(accDepCode) + 借:折旧费用 净值(depExpenseCode) = 贷:固定资产原值 base(faCode)。
     * 仅在三个科目 code 齐全时过账,否则跳过(不阻断状态变更)。
     */
    private void postDisposalGl(AstAsset doc, String action) {
        if (postingFacade == null) return;
        AstAssetCategory cat = doc.getCategory();
        if (cat == null) return;
        String faCode = cat.getFixedAssetAccountCode();
        String accDep = cat.getAccumulatedDepreciationAccountCode();
        String depExpense = cat.getDepreciationExpenseAccountCode();
        if (faCode == null || accDep == null || depExpense == null) return;

        BigDecimal base = nz(doc.getPurchaseAmount());
        BigDecimal totalDep = nz(doc.getTotalDepreciation());
        BigDecimal netBookValue = base.subtract(totalDep);

        List<FinPostingFacade.PostingLine> lines = new ArrayList<>();
        if (totalDep.signum() > 0) {
            lines.add(new FinPostingFacade.PostingLine(accDep, totalDep, null,
                null, null, null, doc.getCostCenterId(), action + "转出累计折旧"));
        }
        if (netBookValue.signum() > 0) {
            lines.add(new FinPostingFacade.PostingLine(depExpense, netBookValue, null,
                null, null, null, doc.getCostCenterId(), action + "净值转出"));
        }
        lines.add(new FinPostingFacade.PostingLine(faCode, null, base,
            null, null, null, doc.getCostCenterId(), action + "转出固定资产"));

        FinPostingFacade.PostingRequest req = new FinPostingFacade.PostingRequest(
            JournalSourceType.ASSET_DISPOSAL.code,
            doc.getId(),
            doc.getAssetNo() + "-" + action,
            LocalDate.now(),
            lines,
            action + " " + doc.getAssetNo()
        );
        Long journalId = postingFacade.post(req);
        if (journalId != null) doc.setLastJournalId(journalId);
    }

    private static BigDecimal nz(BigDecimal b) { return b == null ? BigDecimal.ZERO : b; }
}
