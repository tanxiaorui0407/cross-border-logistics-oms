package com.logistics.oms.module.imports.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Excel 导入记录（导入留痕）。
 */
@Data
@TableName("import_record")
public class ImportRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileName;

    private String operator;

    private Integer total;

    private Integer success;

    private Integer failed;

    private LocalDateTime createdAt;
}
