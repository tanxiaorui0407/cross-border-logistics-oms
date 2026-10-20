package com.logistics.oms.module.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手工录入轨迹（OPS）。
 */
@Data
public class TrackEventReq {

    /** 自定义事件码，默认 CUSTOM。 */
    private String eventCode;

    private String eventDesc;

    private String location;

    /** 不填默认当前时间。 */
    private LocalDateTime eventTime;
}
