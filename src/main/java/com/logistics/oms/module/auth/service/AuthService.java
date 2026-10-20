package com.logistics.oms.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.module.auth.dto.LoginReq;
import com.logistics.oms.module.auth.dto.LoginResp;
import com.logistics.oms.module.user.entity.SysUser;
import com.logistics.oms.module.user.mapper.SysUserMapper;
import com.logistics.oms.security.JwtUtil;
import com.logistics.oms.security.LoginUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(SysUserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResp login(LoginReq req) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())
                .eq(SysUser::getStatus, 1));
        // 用户不存在与密码错误返回同一提示，避免枚举用户名
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw BusinessException.param("用户名或密码错误");
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole(), user.getCustomerId());
        LoginResp resp = new LoginResp();
        resp.setToken(jwtUtil.generateToken(loginUser));
        resp.setUser(new LoginResp.UserInfo(user.getId(), user.getUsername(), user.getRole(), user.getCustomerId()));
        return resp;
    }
}
