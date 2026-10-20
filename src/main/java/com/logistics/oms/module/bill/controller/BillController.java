package com.logistics.oms.module.bill.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.bill.dto.BillGenerateReq;
import com.logistics.oms.module.bill.dto.BillVO;
import com.logistics.oms.module.bill.service.BillService;
import com.logistics.oms.security.LoginUserContext;
import com.logistics.oms.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Tag(name = "账单对账")
@RestController
@RequestMapping("/api/bills")
@RequireRole
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    @Operation(summary = "账单分页")
    @GetMapping
    public Result<Object> page(@RequestParam(defaultValue = "1") long page,
                               @RequestParam(defaultValue = "10") long size,
                               @RequestParam(required = false) Long customerId,
                               @RequestParam(required = false) String period) {
        return Result.ok(billService.page(page, size, customerId, period, LoginUserContext.requireLogin()));
    }

    @Operation(summary = "账单详情（含明细）")
    @GetMapping("/{id}")
    public Result<BillVO> detail(@PathVariable Long id) {
        return Result.ok(billService.detail(id, LoginUserContext.requireLogin()));
    }

    @Operation(summary = "按月生成账单")
    @PostMapping("/generate")
    @RequireRole({"ADMIN", "OPS"})
    public Result<BillVO> generate(@RequestBody @Valid BillGenerateReq req) {
        return Result.ok(billService.generate(req.getCustomerId(), req.getPeriod(), LoginUserContext.requireLogin()));
    }

    @Operation(summary = "标记账单已支付")
    @PostMapping("/{id}/paid")
    @RequireRole({"ADMIN", "OPS"})
    public Result<Void> markPaid(@PathVariable Long id) {
        billService.markPaid(id, LoginUserContext.requireLogin());
        return Result.ok();
    }

    @Operation(summary = "导出账单 Excel")
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        byte[] bytes = billService.export(id, LoginUserContext.requireLogin());
        String encoded = URLEncoder.encode("账单_" + id + ".xlsx", StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
