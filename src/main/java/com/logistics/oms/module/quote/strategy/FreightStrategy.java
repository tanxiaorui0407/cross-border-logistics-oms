package com.logistics.oms.module.quote.strategy;

/**
 * 计费策略（策略模式）：不同运输类型的体积重计算系数不同。
 * 实际使用优先级：渠道配置 vol_divisor > 策略默认系数。
 */
public interface FreightStrategy {

    boolean supports(String transportType);

    /** 策略默认体积重系数。 */
    int defaultVolDivisor();
}
