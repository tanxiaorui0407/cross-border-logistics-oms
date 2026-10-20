package com.logistics.oms.module.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 运价规则（价卡）：渠道 + 目的国唯一。
 */
@Data
@TableName("rate_rule")
public class RateRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    private String destCountry;

    private BigDecimal firstWeight;

    private BigDecimal firstPrice;

    private BigDecimal stepWeight;

    private BigDecimal stepPrice;

    /** 燃油附加费率（%）。 */
    private BigDecimal fuelRate;

    /** 挂号费。 */
    private BigDecimal regFee;

    private String currency;

    private LocalDate effectiveDate;

    private LocalDate expiredDate;

    private LocalDateTime createdAt;
}
