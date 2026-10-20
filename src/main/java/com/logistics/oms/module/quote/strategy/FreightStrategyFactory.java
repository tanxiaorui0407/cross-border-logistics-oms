package com.logistics.oms.module.quote.strategy;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 策略工厂：按运输类型选择计费策略；未知类型兜底 5000。
 */
@Component
public class FreightStrategyFactory {

    private final List<FreightStrategy> strategies;

    public FreightStrategyFactory(List<FreightStrategy> strategies) {
        this.strategies = strategies;
    }

    public FreightStrategy get(String transportType) {
        for (FreightStrategy s : strategies) {
            if (s.supports(transportType)) {
                return s;
            }
        }
        return new FreightStrategy() {
            @Override
            public boolean supports(String transportType) {
                return true;
            }

            @Override
            public int defaultVolDivisor() {
                return 5000;
            }
        };
    }
}
