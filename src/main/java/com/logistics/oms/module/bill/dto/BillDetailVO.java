package com.logistics.oms.module.bill.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BillDetailVO {

    private Long orderId;
    private String orderNo;
    private String goodsDesc;
    private BigDecimal freight;
    private String currency;
}
