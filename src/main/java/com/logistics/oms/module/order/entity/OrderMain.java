package com.logistics.oms.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表。
 */
@Data
@TableName("order_main")
public class OrderMain {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long customerId;

    private Long channelId;

    /** 状态机状态：DRAFT ... SIGNED。 */
    private String status;

    /** 乐观锁版本号。 */
    private Integer version;

    /** 下单幂等 token（唯一索引兜底）。 */
    private String idemToken;

    private String goodsDesc;

    private Integer pieces;

    private BigDecimal actualWeight;

    private BigDecimal lengthCm;

    private BigDecimal widthCm;

    private BigDecimal heightCm;

    private BigDecimal volWeight;

    private BigDecimal billableWeight;

    private BigDecimal freight;

    private String currency;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
