package xyz.herz.ep.mall.handler;

import xyz.erupt.annotation.fun.EruptButtonHandler;
import xyz.herz.ep.mall.entity.MallProductSku;
import xyz.herz.ep.mall.entity.MallProductSpu;
import xyz.herz.ep.mall.jpa.MallProductSkuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Mall SPU BUTTON Handler: 以 {@link MallProductSpu#getPrice()} 为基准价,
 * 根据 skuCount + priceDeltaPercents 批量生成 {@link MallProductSku}。
 *
 * <p>用法: 在 SPU 表单的 skuCount / priceDeltaPercents 两个 EditType.BUTTON 字段
 * 上填值,点按钮 → 自动 skuRepo.saveAll(...)。
 */
@Component
public class MallSpuAutoSkuButtonHandler implements EruptButtonHandler<MallProductSpu> {

    @Autowired MallProductSkuRepository skuRepo;

    /** Erupt 入口: 表单 BUTTON 字段已绑定到 spu, 直接转发到业务 exec。 */
    @Override
    @Transactional
    public String click(MallProductSpu spu, String[] params) {
        return exec(spu.getSkuCount(), spu.getPriceDeltaPercents(), spu);
    }

    /**
     * 纯业务 exec 方法(供测试 + click 调用)。
     *
     * @param skuCount          生成 SKU 数,必须 ≥ 1
     * @param priceDeltaPercents 逗号分隔的百分比数组(如 "10,-10,0"),长度应 == skuCount
     * @param spu               目标 SPU,必须已持久化且 price 非空 > 0
     * @return 成功提示含生成条数
     * @throws IllegalArgumentException 长度/数目不匹配、内容不可解析、对象空/id 空
     * @throws IllegalStateException    spu.price 为空或 ≤ 0
     */
    public String exec(Integer skuCount, String priceDeltaPercents, MallProductSpu spu) {
        if (spu == null) throw new IllegalArgumentException("SPU 不能为空");
        if (skuCount == null || skuCount <= 0) {
            throw new IllegalArgumentException("生成 SKU 数必须 ≥ 1(实际 skuCount=" + skuCount + ")");
        }
        if (spu.getId() == null) {
            throw new IllegalArgumentException("SPU 必须先保存(需绑定 product_id 外键)");
        }
        if (priceDeltaPercents == null || priceDeltaPercents.isBlank()) {
            throw new IllegalArgumentException("价格浮动%数组不能为空(例: \"10,-10,0\")");
        }
        BigDecimal base = spu.getPrice();
        if (base == null) {
            throw new IllegalStateException("SPU 基准价 price 不能为空,无法生成 SKU 价格");
        }
        if (BigDecimal.ZERO.compareTo(base) >= 0) {
            throw new IllegalStateException("SPU 基准价 price 必须 > 0(实际 price=" + base + ")");
        }

        String[] parts = priceDeltaPercents.split("\\s*,\\s*");
        if (parts.length != skuCount) {
            throw new IllegalArgumentException(
                "浮动数组长度与 skuCount 不匹配: skuCount=" + skuCount
                    + ", 实际 deltas 数=" + parts.length + "(" + priceDeltaPercents + ")");
        }

        int[] deltas = new int[skuCount];
        for (int i = 0; i < skuCount; i++) {
            try {
                deltas[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "第 " + (i+1) + " 个浮动值 \"" + parts[i] + "\" 不是合法整数百分比");
            }
        }

        String baseCode = spu.getCode() == null ? "SPU" : spu.getCode();
        List<MallProductSku> skus = new ArrayList<>(skuCount);
        for (int i = 0; i < skuCount; i++) {
            int delta = deltas[i];
            BigDecimal ratio = BigDecimal.valueOf(100L + delta);
            BigDecimal price = base.multiply(ratio).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            MallProductSku s = new MallProductSku();
            s.setProduct(spu);
            s.setCode(baseCode + "-SKU-" + (i + 1));
            s.setSpecs(delta + "%");
            s.setPrice(price);
            s.setStock(0);
            skus.add(s);
        }
        skuRepo.saveAll(skus);
        return "成功生成 " + skus.size() + " 条 SKU";
    }
}
