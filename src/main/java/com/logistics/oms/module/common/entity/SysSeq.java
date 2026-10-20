package com.logistics.oms.module.common.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 发号器：订单号/账单号序列，UPDATE + SELECT 原子递增。
 */
@Data
@TableName("sys_seq")
public class SysSeq {

    @TableId
    private String name;

    @TableField("seq_value")
    private Long value;

    private LocalDateTime updatedAt;
}
