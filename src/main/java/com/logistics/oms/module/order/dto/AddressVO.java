package com.logistics.oms.module.order.dto;

import lombok.Data;

@Data
public class AddressVO {

    private String addrType;
    private String name;
    private String phone;
    private String country;
    private String city;
    private String street;
    private String zip;
}
