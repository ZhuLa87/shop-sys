package com.zzowo.shop_sys.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Shop System API")
                        .description("""
                                購物網站 REST API 文件

                                ## 認證方式
                                所有需要登入的端點均採用 **JWT Bearer Token** 認證。
                                1. 呼叫 `POST /api/v1/auth/login` 取得 Access Token
                                2. 點擊右上角 **Authorize** 按鈕，輸入 `{accessToken}` 即可

                                ## Token 機制
                                - **Access Token**：有效期 30 分鐘，過期後需刷新
                                - **Refresh Token**：有效期 7 天，使用後立即失效（Token Rotation）

                                ## 角色權限
                                | 角色 | 說明 |
                                |------|------|
                                | CUSTOMER | 一般顧客，可瀏覽商品、管理購物車與訂單 |
                                | PRODUCT_MANAGER | 商品管理員，可新增/修改/刪除商品與查看庫存紀錄 |
                                | ORDER_MANAGER | 訂單管理員 |
                                | CUSTOMER_SERVICE | 客服人員 |
                                | MARKETING | 行銷人員 |
                                | FINANCE | 財務人員 |
                                | SUPER_ADMIN | 最高權限，可管理所有使用者與資料 |
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("Shop System")
                                .email("admin@shop-sys.com")))
                .addServersItem(new Server()
                        .url("http://localhost:8088/api")
                        .description("本地開發環境"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("請輸入登入後取得的 Access Token（不需要加 Bearer 前綴）")));
    }
}
