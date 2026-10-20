package com.logistics.oms.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.logistics.oms.module.user.entity.SysUser;
import com.logistics.oms.module.user.mapper.SysUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 首次启动初始化演示账号（幂等）：
 * admin(ADMIN) / ops(OPS) / alice(CUSTOMER,客户1) / bob(CUSTOMER,客户2)，密码均为 123456。
 * 数据库脚本不落密码，避免硬编码散列。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        createIfAbsent("admin", "ADMIN", null);
        createIfAbsent("ops", "OPS", null);
        createIfAbsent("alice", "CUSTOMER", 1L);
        createIfAbsent("bob", "CUSTOMER", 2L);
    }

    private void createIfAbsent(String username, String role, Long customerId) {
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRole(role);
        user.setCustomerId(customerId);
        user.setStatus(1);
        userMapper.insert(user);
    }
}
