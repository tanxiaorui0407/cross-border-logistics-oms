package com.logistics.oms.module.bill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单主表：客户 + 账期唯一。
 */
@Data
@TableName("bill")
public class Bill {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String billNo;

    private Long customerId;

    /** 账期 yyyy-MM。 */
    private String period;

    private BigDecimal totalAmount;

    private String currency;

    /** OPEN / PAID。 */
    private String status;

    private LocalDateTime createdAt;
}
