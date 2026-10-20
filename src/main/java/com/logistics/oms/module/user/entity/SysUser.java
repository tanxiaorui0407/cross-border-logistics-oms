package com.logistics.oms.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户：ADMIN / OPS / CUSTOMER 三角色。
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 散列。 */
    private String password;

    private String role;

    /** role=CUSTOMER 时关联 customer.id。 */
    private Long customerId;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
