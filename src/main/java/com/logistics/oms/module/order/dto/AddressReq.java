package com.logistics.oms.module.order.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AddressReq {

    @NotBlank(message = "收/寄件人姓名不能为空")
    private String name;

    private String phone;

    @NotBlank(message = "国家不能为空")
    private String country;

    private String city;
    private String street;
    private String zip;
}
