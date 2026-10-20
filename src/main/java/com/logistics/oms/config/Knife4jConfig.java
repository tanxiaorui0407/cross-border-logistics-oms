package com.logistics.oms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API 文档（Knife4j / springdoc）。
 * 访问路径：/doc.html
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI omsOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("跨境物流订单管理系统 API")
                .description("覆盖 下单 → 计费 → 状态流转 → 轨迹追踪 → 对账 全生命周期")
                .version("1.0.0"));
    }
}
