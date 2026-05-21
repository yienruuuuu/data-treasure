package io.github.yienruuuuu.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the generated OpenAPI document used by Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dataTreasureOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Data Treasure API")
                        .version("1.0.0")
                        .description("""
                                Data Treasure 是面向 TOC 客戶的資料站 API。
                                
                                目前專案核心包含爬蟲任務、資料處理與持久化排程框架。
                                排程 API 可用來建立 Cron 任務、啟用/停用任務、查詢任務狀態，以及追蹤任務執行錯誤。
                                """)
                        .contact(new Contact()
                                .name("Data Treasure Team"))
                        .license(new License()
                                .name("Internal Use")));
    }
}
