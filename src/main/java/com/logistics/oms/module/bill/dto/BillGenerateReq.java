package com.logistics.oms.module.bill.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class BillGenerateReq {

    @NotNull(message = "客户不能为空")
    private Long customerId;

    /** 账期 yyyy-MM。 */
    @NotBlank(message = "账期不能为空")
    private String period;
}
