package com.logistics.oms.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.logistics.oms.common.BusinessException;
import com.logistics.oms.common.PageResult;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.mapper.CustomerMapper;
import com.logistics.oms.module.user.dto.UserReq;
import com.logistics.oms.module.user.dto.UserVO;
import com.logistics.oms.module.user.entity.SysUser;
import com.logistics.oms.module.user.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Set<String> ROLES = Set.of("ADMIN", "OPS", "CUSTOMER");

    private final SysUserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper userMapper, CustomerMapper customerMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public PageResult<UserVO> page(long current, long size, String keyword) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(SysUser::getUsername, keyword);
        }
        w.orderByDesc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size), w);
        return PageResult.of(page, this::toVO);
    }

    public UserVO create(UserReq req) {
        validate(req);
        if (userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, req.getUsername())) > 0) {
            throw BusinessException.param("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        user.setCustomerId(req.getRole().equals("CUSTOMER") ? req.getCustomerId() : null);
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        userMapper.insert(user);
        return toVO(userMapper.selectById(user.getId()));
    }

    public UserVO update(Long id, UserReq req) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        validate(req);
        user.setRole(req.getRole());
        user.setCustomerId(req.getRole().equals("CUSTOMER") ? req.getCustomerId() : null);
        user.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        userMapper.updateById(user);
        return toVO(userMapper.selectById(id));
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw BusinessException.param("新密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        if (userMapper.deleteById(id) == 0) {
            throw BusinessException.notFound("用户不存在");
        }
    }

    private void validate(UserReq req) {
        if (!ROLES.contains(req.getRole())) {
            throw BusinessException.param("非法角色，仅支持 ADMIN / OPS / CUSTOMER");
        }
        if ("CUSTOMER".equals(req.getRole()) && req.getCustomerId() == null) {
            throw BusinessException.param("CUSTOMER 角色必须关联客户");
        }
        if (req.getCustomerId() != null && customerMapper.selectById(req.getCustomerId()) == null) {
            throw BusinessException.param("关联客户不存在");
        }
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setCustomerId(user.getCustomerId());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        if (user.getCustomerId() != null) {
            Customer customer = customerMapper.selectById(user.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getCompanyName());
            }
        }
        return vo;
    }
}
