package com.logistics.oms.module.order.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TrackEventVO {

    private String eventCode;
    private String eventDesc;
    private String location;
    private LocalDateTime eventTime;
    private String operator;
}
