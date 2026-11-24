package com.agri.cms.web.core.config;

import com.agri.cms.common.config.GlobalConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / springdoc 接口文档配置（A-21）。
 *
 * 按业务域分组，覆盖全部控制器；统一使用 Bearer Token 鉴权。
 * 访问地址：/swagger-ui.html（在线文档）与 /v3/api-docs（OpenAPI JSON）。
 *
 * @author agrinav
 */
@Configuration
public class SwaggerConfig {

    @Autowired
    private GlobalConfig globalConfig;

    /**
     * 自定义 OpenAPI 元数据与鉴权方案。
     */
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes("apikey", securityScheme()))
                .addSecurityItem(new SecurityRequirement().addList("apikey"))
                .info(getApiInfo());
    }

    // ------------------------------------------------------------------ 分组

    @Bean
    public GroupedOpenApi authApi() {
        return buildGroup("01-认证与登录",
                "/login", "/loginQR", "/captcha", "/getInfo", "/getRouters",
                "/loginWithoutSecret", "/loginOAuth", "/loginWechatMobile",
                "/signOut/**", "/render");
    }

    @Bean
    public GroupedOpenApi machineryApi() {
        return buildGroup("02-农机管理",
                "/agriculturalMachinery/**", "/agriculturalMachineryType/**");
    }

    @Bean
    public GroupedOpenApi farmWorkApi() {
        return buildGroup("03-农活招工", "/farmWork/**", "/workingLandPlot/**");
    }

    @Bean
    public GroupedOpenApi landPlotApi() {
        return buildGroup("04-地块管理",
                "/landPlot/**", "/landPlotExclude/**", "/landAddress/**");
    }

    @Bean
    public GroupedOpenApi fieldWorkApi() {
        return buildGroup("05-作业类型与作物", "/fieldWork/**", "/crop/**");
    }

    @Bean
    public GroupedOpenApi trajectoryApi() {
        return buildGroup("06-轨迹管理",
                "/trajectoryInfo/**", "/effectiveTrajectory/**", "/originalTrajectory/**", "/trajectory/playback/**");
    }

    @Bean
    public GroupedOpenApi dispatchApi() {
        return buildGroup("07-作业调度", "/dispatch/**");
    }

    @Bean
    public GroupedOpenApi closureApi() {
        return buildGroup("08-内部结算",
                "/settlement/**", "/reviewRequest/**", "/pricingRule/**");
    }

    @Bean
    public GroupedOpenApi agentApi() {
        return buildGroup("09-智能体", "/llm/**", "/agent/**");
    }

    @Bean
    public GroupedOpenApi systemApi() {
        return buildGroup("10-系统管理", "/system/**", "/permission/**");
    }

    @Bean
    public GroupedOpenApi monitorApi() {
        return buildGroup("11-监控运维", "/monitor/**");
    }

    @Bean
    public GroupedOpenApi analysisApi() {
        return buildGroup("12-统计分析", "/analysis/**", "/static/**", "/screen/**");
    }

    @Bean
    public GroupedOpenApi commonApi() {
        return buildGroup("13-通用与测试", "/common/**", "/version/**", "/test/**");
    }

    // ------------------------------------------------------------------ 基础组件

    private GroupedOpenApi buildGroup(String group, String... paths) {
        return GroupedOpenApi.builder()
                .group(group)
                .pathsToMatch(paths)
                .addOpenApiCustomizer(openApi -> openApi.info(getApiInfo()))
                .build();
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .name("Authorization")
                .in(SecurityScheme.In.HEADER)
                .scheme("Bearer")
                .bearerFormat("JWT");
    }

    private Info getApiInfo() {
        return new Info()
                .title("田界智航 AgriNav 农机作业管控平台 · 接口文档")
                .description("农机作业调度、轨迹管控、面积核算与内部结算的统一后端接口清单，"
                        + "作为 Web 前端（C）与 Android（D）的对接契约。在线文档支持按业务分组浏览，"
                        + "需先登录获取 Token，点击右上角 Authorize 填入 Bearer Token 后调试。")
                .contact(new Contact().name("AgriNav 项目组"))
                .license(new License().name("AgriNav"))
                .version("v" + globalConfig.getVersion());
    }
}
