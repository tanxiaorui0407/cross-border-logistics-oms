package com.logistics.oms.module.bill.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BillVO {

    private Long id;
    private String billNo;
    private Long customerId;
    private String customerName;
    private String period;
    private BigDecimal totalAmount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
    private Integer orderCount;

    private List<BillDetailVO> details;
}
