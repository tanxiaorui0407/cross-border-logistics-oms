package com.logistics.oms.module.channel.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.channel.entity.Channel;
import com.logistics.oms.module.channel.service.ChannelService;
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

@Tag(name = "渠道管理")
@RestController
@RequestMapping
public class ChannelController {

    private final ChannelService channelService;

    public ChannelController(ChannelService channelService) {
        this.channelService = channelService;
    }

    /** 启用渠道列表（下单/试算下拉框用，所有登录角色可见）。 */
    @GetMapping("/api/channels")
    public Result<List<Channel>> listActive() {
        return Result.ok(channelService.listActive());
    }

    @GetMapping("/api/admin/channels")
    @RequireRole("ADMIN")
    public Result<Object> page(@RequestParam(defaultValue = "1") long page,
                               @RequestParam(defaultValue = "10") long size,
                               @RequestParam(required = false) String keyword) {
        return Result.ok(channelService.page(page, size, keyword));
    }

    @PostMapping("/api/admin/channels")
    @RequireRole("ADMIN")
    public Result<Channel> create(@RequestBody Channel channel) {
        return Result.ok(channelService.create(channel));
    }

    @PutMapping("/api/admin/channels/{id}")
    @RequireRole("ADMIN")
    public Result<Channel> update(@PathVariable Long id, @RequestBody Channel channel) {
        return Result.ok(channelService.update(id, channel));
    }

    @DeleteMapping("/api/admin/channels/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        channelService.delete(id);
        return Result.ok();
    }
}
