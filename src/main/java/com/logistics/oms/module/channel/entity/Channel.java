package com.logistics.oms.module.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物流渠道：国际快递 / 邮政小包 / 专线。
 */
@Data
@TableName("channel")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channelCode;

    private String channelName;

    /** EXPRESS / POST / LINE，对应计费策略。 */
    private String transportType;

    private String carrier;

    /** 参考时效（天），用于比价排序。 */
    private Integer etaDays;

    /** 体积重系数：空运 5000 / 专线 8000，可配置。 */
    private Integer volDivisor;

    private String currency;

    /** 1 启用 / 0 停用。 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
