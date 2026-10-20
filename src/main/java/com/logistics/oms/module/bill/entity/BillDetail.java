package com.logistics.oms.module.bill.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账单明细：一张账单聚合一个客户一个账期内的全部有效订单。
 */
@Data
@TableName("bill_detail")
public class BillDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long billId;

    private Long orderId;

    private String orderNo;

    private String goodsDesc;

    private BigDecimal freight;

    private String currency;

    private LocalDateTime createdAt;
}
