package com.logistics.oms.module.quote.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 运费试算请求：目的国 + 重量 + 尺寸。
 */
@Data
public class QuoteReq {

    @NotBlank(message = "目的国不能为空")
    private String destCountry;

    @NotNull(message = "实重不能为空")
    private BigDecimal actualWeight;

    private BigDecimal lengthCm;
    private BigDecimal widthCm;
    private BigDecimal heightCm;
}
