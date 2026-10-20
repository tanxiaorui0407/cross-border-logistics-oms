package com.logistics.oms.module.order.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 下单请求：CUSTOMER 角色 customerId 取登录态；OPS/ADMIN 需显式传 customerId。
 */
@Data
public class OrderCreateReq {

    @NotNull(message = "渠道不能为空")
    private Long channelId;

    /** OPS/ADMIN 代客下单时必填。 */
    private Long customerId;

    @NotBlank(message = "品名不能为空")
    private String goodsDesc;

    private Integer pieces;

    @NotNull(message = "实重不能为空")
    private BigDecimal actualWeight;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;

    @NotBlank(message = "目的国不能为空")
    private String destCountry;

    @Valid
    @NotNull(message = "寄件地址不能为空")
    private AddressReq sender;

    @Valid
    @NotNull(message = "收件地址不能为空")
    private AddressReq receiver;
}
