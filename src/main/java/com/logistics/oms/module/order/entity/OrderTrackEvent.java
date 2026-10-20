package com.logistics.oms.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单轨迹事件：状态机系统事件 + 操作员手工录入。
 */
@Data
@TableName("order_track_event")
public class OrderTrackEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private String eventCode;

    private String eventDesc;

    private String location;

    private LocalDateTime eventTime;

    private String operator;

    private LocalDateTime createdAt;
}
