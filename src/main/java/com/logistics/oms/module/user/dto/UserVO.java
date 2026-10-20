package com.logistics.oms.module.user.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    private Long id;
    private String username;
    private String role;
    private Long customerId;
    private String customerName;
    private Integer status;
    private LocalDateTime createdAt;
}
