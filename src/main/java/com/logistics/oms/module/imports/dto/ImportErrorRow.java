package com.logistics.oms.module.imports.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 导入错误报告导出行。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorRow {

    @ExcelProperty("Excel行号")
    private Integer rowNo;

    @ExcelProperty("错误原因")
    private String errorMsg;
}
