package com.logistics.oms.module.customer.controller;

import com.logistics.oms.common.Result;
import com.logistics.oms.module.customer.entity.Customer;
import com.logistics.oms.module.customer.service.CustomerService;
import com.logistics.oms.security.LoginUser;
import com.logistics.oms.security.LoginUserContext;
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

@Tag(name = "客户管理")
@RestController
@RequestMapping
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/api/customers")
    public Result<List<Customer>> listActive() {
        return Result.ok(customerService.listActive());
    }

    /** CUSTOMER 角色查看自己的客户档案。 */
    @GetMapping("/api/customers/me")
    public Result<Customer> me() {
        LoginUser user = LoginUserContext.requireLogin();
        if (user.customerId() == null) {
            return Result.fail(400, "当前账号未关联客户");
        }
        Customer customer = customerService.listActive().stream()
                .filter(c -> c.getId().equals(user.customerId()))
                .findFirst().orElse(null);
        return Result.ok(customer);
    }

    @GetMapping("/api/admin/customers")
    @RequireRole("ADMIN")
    public Result<Object> page(@RequestParam(defaultValue = "1") long page,
                               @RequestParam(defaultValue = "10") long size,
                               @RequestParam(required = false) String keyword) {
        return Result.ok(customerService.page(page, size, keyword));
    }

    @PostMapping("/api/admin/customers")
    @RequireRole("ADMIN")
    public Result<Customer> create(@RequestBody Customer customer) {
        return Result.ok(customerService.create(customer));
    }

    @PutMapping("/api/admin/customers/{id}")
    @RequireRole("ADMIN")
    public Result<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        return Result.ok(customerService.update(id, customer));
    }

    @DeleteMapping("/api/admin/customers/{id}")
    @RequireRole("ADMIN")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }
}
