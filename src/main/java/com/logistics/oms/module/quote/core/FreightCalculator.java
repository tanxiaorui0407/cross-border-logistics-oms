package com.logistics.oms.module.quote.core;

import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.quote.strategy.FreightStrategy;
import com.logistics.oms.module.quote.strategy.FreightStrategyFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 运费计算引擎（核心）。
 *
 * <pre>
 * 体积重 = 长 × 宽 × 高 / 系数（系数 = 渠道配置，兜底取运输类型策略默认：空运5000/专线8000）
 * 计费重 = max(实重, 体积重)
 * 计费重 ≤ 首重  → 运费 = 首重价
 * 计费重 &gt; 首重  → 运费 = 首重价 + ceil((计费重-首重)/续重单位) × 续重单价
 * 最终运费 = 运费 × (1 + 燃油费率) + 挂号费
 * </pre>
 *
 * <p>金额全程 BigDecimal，避免 double 精度丢失。</p>
 */
@Component
public class FreightCalculator {

    private final FreightStrategyFactory strategyFactory;

    public FreightCalculator(FreightStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    /** 计算结果：体积重 / 计费重 / 最终运费。 */
    public static class CalcResult {
        private final BigDecimal volWeight;
        private final BigDecimal billableWeight;
        private final BigDecimal freight;

        public CalcResult(BigDecimal volWeight, BigDecimal billableWeight, BigDecimal freight) {
            this.volWeight = volWeight;
            this.billableWeight = billableWeight;
            this.freight = freight;
        }

        public BigDecimal volWeight() {
            return volWeight;
        }

        public BigDecimal billableWeight() {
            return billableWeight;
        }

        public BigDecimal freight() {
            return freight;
        }
    }

    public CalcResult calc(Channel channel, RateRule rule,
                           BigDecimal actualWeight,
                           BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm) {
        FreightStrategy strategy = strategyFactory.get(channel.getTransportType());
        BigDecimal divisor = channel.getVolDivisor() == null
                ? BigDecimal.valueOf(strategy.defaultVolDivisor())
                : BigDecimal.valueOf(channel.getVolDivisor());

        BigDecimal volWeight = null;
        if (lengthCm != null && widthCm != null && heightCm != null) {
            volWeight = lengthCm.multiply(widthCm).multiply(heightCm)
                    .divide(divisor, 3, RoundingMode.HALF_UP);
        }

        BigDecimal billable = actualWeight;
        if (volWeight != null && volWeight.compareTo(billable) > 0) {
            billable = volWeight;
        }

        BigDecimal base;
        if (billable.compareTo(rule.getFirstWeight()) <= 0) {
            base = rule.getFirstPrice();
        } else {
            BigDecimal extra = billable.subtract(rule.getFirstWeight());
            BigDecimal steps = extra.divide(rule.getStepWeight(), 0, RoundingMode.CEILING);
            base = rule.getFirstPrice().add(steps.multiply(rule.getStepPrice()));
        }

        BigDecimal fuel = base.multiply(rule.getFuelRate())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal freight = base.add(fuel).add(rule.getRegFee())
                .setScale(2, RoundingMode.HALF_UP);

        return new CalcResult(volWeight, billable, freight);
    }
}
