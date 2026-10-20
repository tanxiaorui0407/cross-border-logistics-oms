package com.logistics.oms.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单地址：一单两址（SENDER 寄件人 / RECEIVER 收件人）。
 */
@Data
@TableName("order_address")
public class OrderAddress {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String addrType;

    private String name;

    private String phone;

    private String country;

    private String city;

    private String street;

    private String zip;

    private LocalDateTime createdAt;
}
