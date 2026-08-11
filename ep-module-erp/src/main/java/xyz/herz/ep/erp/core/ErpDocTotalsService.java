package xyz.herz.ep.erp.core;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 单据汇总辅助:计算合计数量/商品额/税额/优惠额/总金额,
 * 统一由 DataProxy#beforeAdd / beforeUpdate 调用,避免用户手工改只读字段。
 */
@Component
public class ErpDocTotalsService {

    public interface DocLine {
        BigDecimal count();
        BigDecimal productPrice();
        BigDecimal taxPercent();
        // = 回写字段:taxPrice / totalPrice
        void setTaxPrice(BigDecimal t);
        void setTotalPrice(BigDecimal t);
    }

    public record Totals(
            BigDecimal totalCount,
            BigDecimal totalProductPrice,
            BigDecimal totalTaxPrice,
            BigDecimal discountPrice,
            BigDecimal totalPrice
    ) { }

    public Totals calc(List<? extends DocLine> lines, BigDecimal discountPercent /* 0-100 */) {
        BigDecimal count = BigDecimal.ZERO;
        BigDecimal product = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (DocLine l : lines) {
            BigDecimal c = nvl(l.count());
            BigDecimal p = nvl(l.productPrice());
            BigDecimal lineProduct = c.multiply(p);
            BigDecimal tp = nvl(l.taxPercent());
            BigDecimal lineTax = lineProduct.multiply(tp)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineProduct.add(lineTax);
            l.setTaxPrice(lineTax);
            l.setTotalPrice(lineTotal);
            count = count.add(c);
            product = product.add(lineProduct);
            tax = tax.add(lineTax);
        }
        BigDecimal dp = BigDecimal.ZERO;
        if (discountPercent != null && discountPercent.signum() > 0) {
            BigDecimal base = product.add(tax);
            dp = base.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }
        BigDecimal total = product.add(tax).subtract(dp);
        return new Totals(count, product, tax, dp, total);
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
