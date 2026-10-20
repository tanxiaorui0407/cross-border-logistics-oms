package com.logistics.oms.module.imports.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 导入行级错误明细。
 */
@Data
@TableName("import_error")
public class ImportError {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    /** Excel 行号（含表头）。 */
    private Integer rowNo;

    private String errorMsg;

    private LocalDateTime createdAt;
}
