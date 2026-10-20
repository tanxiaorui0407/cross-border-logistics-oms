package com.logistics.oms.module.channel.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.channel.entity.RateRule;
import com.logistics.oms.module.channel.service.RateRuleService;
import com.logistics.oms.security.RequireRole;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "价卡管理")
@RestController
@RequestMapping
public class RateRuleController {

    private final RateRuleService rateRuleService;

    public RateRuleController(RateRuleService rateRuleService) {
        this.rateRuleService = rateRuleService;
    }

    /** 价卡查询（ADMIN 管理页用）。 */
    @GetMapping("/api/admin/rate-rules")
    @RequireRole("ADMIN")
    public Result<Object> page(@RequestParam(defaultValue = "1") long page,
                               @RequestParam(defaultValue = "100") long size,
                               @RequestParam(required = false) Long channelId,
                               @RequestParam(required = false) String destCountry) {
        return Result.ok(rateRuleService.page(page, size, channelId, destCountry));
    }

    @GetMapping("/api/admin/rate-rules/channel/{channelId}")
    @RequireRole("ADMIN")
    public Result<List<RateRule>> listByChannel(@PathVariable Long channelId) {
        return Result.ok(rateRuleService.listByChannel(channelId));
    }

    @PostMapping("/api/admin/rate-rules")
    @RequireRole("ADMIN")
    public Result<RateRule> create(@RequestBody RateRule rule) {
        return Result.ok(rateRuleService.create(rule));
    }

    @PutMapping("/api/admin/rate-rules/{id}")
    @RequireRole("ADMIN")
    public Result<RateRule> update(@PathVariable Long id, @RequestBody RateRule rule) {
        return Result.ok(rateRuleService.update(id, rule));
    }

    @DeleteMapping("/api/admin/rate-rules/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        rateRuleService.delete(id);
        return Result.ok();
    }
}
