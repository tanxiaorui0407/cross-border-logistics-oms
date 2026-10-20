package com.logistics.oms.module.quote.strategy;

import org.springframework.stereotype.Component;

/**
 * 邮政小包：体积重系数 5000。
 */
@Component
public class PostFreightStrategy implements FreightStrategy {

    @Override
    public boolean supports(String transportType) {
        return "POST".equalsIgnoreCase(transportType);
    }

    @Override
    public int defaultVolDivisor() {
        return 5000;
    }
}
