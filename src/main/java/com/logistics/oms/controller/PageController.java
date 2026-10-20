package com.logistics.oms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 页面路由（Thymeleaf）。页面登录门禁由前端 JS 控制，数据全部走受保护 API。
 */
@Controller
public class PageController {

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/orders")
    public String orders() {
        return "orders";
    }

    @GetMapping("/order/{id}")
    public String orderDetail(@PathVariable Long id) {
        return "order-detail";
    }

    @GetMapping("/quote")
    public String quote() {
        return "quote";
    }

    @GetMapping("/import")
    public String imports() {
        return "import";
    }

    @GetMapping("/bills")
    public String bills() {
        return "bills";
    }

    @GetMapping("/admin/users")
    public String users() {
        return "users";
    }

    @GetMapping("/admin/customers")
    public String customers() {
        return "customers";
    }

    @GetMapping("/admin/channels")
    public String channels() {
        return "channels";
    }

    @GetMapping("/admin/rates")
    public String rates() {
        return "rates";
    }
}
