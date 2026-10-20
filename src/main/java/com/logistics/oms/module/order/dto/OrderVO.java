package com.logistics.oms.module.order.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 VO：列表（轻量，无地址/轨迹）与详情（含地址/轨迹/可流转状态）。
 */
@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long customerId;
    private String customerName;
    private Long channelId;
    private String channelName;
    private String transportType;
    private String status;
    private String statusDesc;
    private String goodsDesc;
    private Integer pieces;
    private BigDecimal actualWeight;
    private BigDecimal volWeight;
    private BigDecimal billableWeight;
    private BigDecimal freight;
    private String currency;
    private LocalDateTime createdAt;

    /** 详情字段。 */
    private AddressVO sender;
    private AddressVO receiver;
    private List<TrackEventVO> trackEvents;
    /** 当前登录角色可触发的目标状态。 */
    private List<String> allowedTransitions;
}
