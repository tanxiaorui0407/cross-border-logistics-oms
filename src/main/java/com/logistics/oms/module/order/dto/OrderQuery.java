package com.logistics.oms.module.order.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 订单分页查询条件。
 */
@Data
public class OrderQuery {

    private String status;

    /** 单号 / 品名模糊。 */
    private String keyword;

    /** OPS/ADMIN 可按客户过滤。 */
    private Long customerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    private long page = 1;
    private long size = 10;
}
