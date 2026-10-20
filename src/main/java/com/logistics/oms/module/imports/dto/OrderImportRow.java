package com.logistics.oms.module.imports.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Excel 导入行（字段全 String，便于行级校验并给出精确错误提示）。
 */
@Data
public class OrderImportRow {

    @ExcelProperty("客户编码")
    private String customerCode;

    @ExcelProperty("品名")
    private String goodsDesc;

    @ExcelProperty("件数")
    private String pieces;

    @ExcelProperty("实重kg")
    private String actualWeight;

    @ExcelProperty("长cm")
    private String lengthCm;

    @ExcelProperty("宽cm")
    private String widthCm;

    @ExcelProperty("高cm")
    private String heightCm;

    @ExcelProperty("目的国")
    private String destCountry;

    @ExcelProperty("渠道编码")
    private String channelCode;

    @ExcelProperty("寄件人")
    private String senderName;

    @ExcelProperty("寄件人电话")
    private String senderPhone;

    @ExcelProperty("寄件人城市")
    private String senderCity;

    @ExcelProperty("寄件人街道")
    private String senderStreet;

    @ExcelProperty("寄件人邮编")
    private String senderZip;

    @ExcelProperty("收件人")
    private String receiverName;

    @ExcelProperty("收件人电话")
    private String receiverPhone;

    @ExcelProperty("收件人国家")
    private String receiverCountry;

    @ExcelProperty("收件人城市")
    private String receiverCity;

    @ExcelProperty("收件人街道")
    private String receiverStreet;

    @ExcelProperty("收件人邮编")
    private String receiverZip;
}
