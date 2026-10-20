package com.logistics.oms.module.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class TransitionReq {

    @NotBlank(message = "目标状态不能为空")
    private String targetStatus;
}
