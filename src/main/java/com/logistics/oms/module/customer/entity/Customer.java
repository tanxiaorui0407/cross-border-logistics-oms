package com.logistics.oms.module.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String customerCode;

    private String companyName;

    private String contact;

    /** ISO 国家码。 */
    private String country;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
