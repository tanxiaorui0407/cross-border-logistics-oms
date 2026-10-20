package com.logistics.oms.module.bill.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账单导出行。
 */
@Data
public class BillExportRow {

    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("品名")
    private String goodsDesc;

    @ExcelProperty("计费重(kg)")
    private BigDecimal billableWeight;

    @ExcelProperty("运费")
    private BigDecimal freight;

    @ExcelProperty("币种")
    private String currency;
}
