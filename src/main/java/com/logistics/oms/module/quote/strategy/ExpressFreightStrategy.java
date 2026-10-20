package com.logistics.oms.module.quote.strategy;

import org.springframework.stereotype.Component;

/**
 * 国际快递：体积重系数 5000。
 */
@Component
public class ExpressFreightStrategy implements FreightStrategy {

    @Override
    public boolean supports(String transportType) {
        return "EXPRESS".equalsIgnoreCase(transportType);
    }

    @Override
    public int defaultVolDivisor() {
        return 5000;
    }
}
