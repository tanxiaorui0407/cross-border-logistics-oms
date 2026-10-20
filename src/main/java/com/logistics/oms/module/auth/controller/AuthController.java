package com.logistics.oms.module.auth.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.auth.dto.LoginReq;
import com.logistics.oms.module.auth.dto.LoginResp;
import com.logistics.oms.module.auth.service.AuthService;
import com.logistics.oms.security.LoginUser;
import com.logistics.oms.security.LoginUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginResp> login(@RequestBody @Valid LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/me")
    public Result<LoginResp.UserInfo> me() {
        LoginUser user = LoginUserContext.requireLogin();
        return Result.ok(new LoginResp.UserInfo(
                user.userId(), user.username(), user.role(), user.customerId()));
    }
}
