package com.logistics.oms.module.quote.strategy;

import org.springframework.stereotype.Component;

/**
 * 专线：体积重系数 8000（专线以实重为主）。
 */
@Component
public class LineFreightStrategy implements FreightStrategy {

    @Override
    public boolean supports(String transportType) {
        return "LINE".equalsIgnoreCase(transportType);
    }

    @Override
    public int defaultVolDivisor() {
        return 8000;
    }
}
