package com.logistics.oms.module.order.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.imports.service.ImportService;
import com.logistics.oms.module.order.dto.OrderCreateReq;
import com.logistics.oms.module.order.dto.OrderQuery;
import com.logistics.oms.module.order.dto.OrderVO;
import com.logistics.oms.module.order.dto.TrackEventReq;
import com.logistics.oms.module.order.dto.TransitionReq;
import com.logistics.oms.module.order.service.OrderService;
import com.logistics.oms.security.LoginUser;
import com.logistics.oms.security.LoginUserContext;
import com.logistics.oms.security.RequireRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Tag(name = "订单")
@RestController
@RequestMapping("/api/orders")
@RequireRole
public class OrderController {

    private final OrderService orderService;
    private final ImportService importService;

    public OrderController(OrderService orderService, ImportService importService) {
        this.orderService = orderService;
        this.importService = importService;
    }

    @Operation(summary = "订单分页查询")
    @GetMapping
    public Result<Object> page(@ModelAttribute OrderQuery query) {
        return Result.ok(orderService.page(query, LoginUserContext.requireLogin()));
    }

    @Operation(summary = "订单详情（含轨迹时间线）")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id, LoginUserContext.requireLogin()));
    }

    @Operation(summary = "创建订单（幂等 token 防重复提交）")
    @PostMapping
    public Result<OrderVO> create(@RequestBody @Valid OrderCreateReq req,
                                  @RequestHeader(value = "X-Idempotency-Token", required = false) String idemToken) {
        return Result.ok(orderService.create(req, LoginUserContext.requireLogin(), idemToken));
    }

    @Operation(summary = "状态流转（状态机 + 乐观锁）")
    @PostMapping("/{id}/transition")
    public Result<OrderVO> transition(@PathVariable Long id, @RequestBody @Valid TransitionReq req) {
        return Result.ok(orderService.transition(id, req, LoginUserContext.requireLogin()));
    }

    @Operation(summary = "手工录入轨迹（OPS）")
    @PostMapping("/{id}/track")
    public Result<Void> addTrack(@PathVariable Long id, @RequestBody TrackEventReq req) {
        orderService.addTrackEvent(id, req, LoginUserContext.requireLogin());
        return Result.ok();
    }

    @Operation(summary = "订单状态统计（仪表盘）")
    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        return Result.ok(orderService.stats(LoginUserContext.requireLogin()));
    }

    @Operation(summary = "Excel 批量导入订单")
    @PostMapping("/import")
    public Result<Object> importOrders(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importOrders(file, LoginUserContext.requireLogin()));
    }

    @Operation(summary = "下载导入模板")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> template() {
        byte[] bytes = importService.templateBytes();
        return excelResponse(bytes, "订单导入模板.xlsx");
    }

    @Operation(summary = "下载导入错误报告")
    @GetMapping("/import/{recordId}/errors")
    public ResponseEntity<byte[]> importErrors(@PathVariable Long recordId) {
        byte[] bytes = importService.errorReportBytes(recordId);
        return excelResponse(bytes, "导入错误报告_" + recordId + ".xlsx");
    }

    private ResponseEntity<byte[]> excelResponse(byte[] bytes, String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
