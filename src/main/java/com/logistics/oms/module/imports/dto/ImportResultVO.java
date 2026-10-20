package com.logistics.oms.module.imports.dto;

import lombok.Data;

import java.util.List;

/**
 * 导入结果：总数 / 成功 / 失败 + 行级错误列表（并落库留痕）。
 */
@Data
public class ImportResultVO {

    private Long recordId;
    private String fileName;
    private Integer total;
    private Integer success;
    private Integer failed;
    private List<ImportErrorVO> errors;

    @Data
    public static class ImportErrorVO {
        private Integer rowNo;
        private String errorMsg;

        public ImportErrorVO() {
        }

        public ImportErrorVO(Integer rowNo, String errorMsg) {
            this.rowNo = rowNo;
            this.errorMsg = errorMsg;
        }
    }
}
