package com.logistics.oms.module.quote.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 比价结果项。
 */
@Data
public class QuoteItemVO {

    private Long channelId;
    private String channelName;
    private String transportType;
    private Integer etaDays;
    private BigDecimal volWeight;
    private BigDecimal billableWeight;
    private BigDecimal freight;
    private String currency;
}
