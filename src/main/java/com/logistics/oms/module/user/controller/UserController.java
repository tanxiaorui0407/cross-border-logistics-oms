package com.logistics.oms.module.user.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.user.dto.UserReq;
import com.logistics.oms.module.user.dto.UserVO;
import com.logistics.oms.module.user.service.UserService;
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

import javax.validation.Valid;
import java.util.Map;

@Tag(name = "用户管理(ADMIN)")
@RestController
@RequestMapping("/api/admin/users")
@RequireRole("ADMIN")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<Object> page(@RequestParam(defaultValue = "1") long page,
                               @RequestParam(defaultValue = "10") long size,
                               @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, keyword));
    }

    @PostMapping
    public Result<UserVO> create(@RequestBody @Valid UserReq req) {
        return Result.ok(userService.create(req));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id, @RequestBody @Valid UserReq req) {
        return Result.ok(userService.update(id, req));
    }

    @PostMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
