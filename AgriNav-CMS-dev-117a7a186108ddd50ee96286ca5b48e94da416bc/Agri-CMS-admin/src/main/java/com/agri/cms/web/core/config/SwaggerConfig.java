package com.agri.cms.web.core.config;

import com.agri.cms.common.config.GlobalConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger2的接口配置
 *
 * @author ruoyi
 */
@Configuration
public class SwaggerConfig {
    /**
     * 系统基础配置
     */
    @Autowired
    private GlobalConfig globalConfig;

    /**
     * 自定义的 OpenAPI 对象
     */
    @Bean
    public OpenAPI customOpenApi() {
        return new OpenAPI().components(new Components()
                        // 设置认证的请求头
                        .addSecuritySchemes("apikey", securityScheme()))
                .addSecurityItem(new SecurityRequirement().addList("apikey"))
                .info(getApiInfo());
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("登录注册验证")
                .pathsToMatch("/login", "/captcha", "/getInfo", "/loginWithoutSecret", "/signOut/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }
    @Bean
    public GroupedOpenApi feedbackApi() {
        return GroupedOpenApi.builder()
                .group("用户反馈API")
                .pathsToMatch("/system/feedback/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi agriculturalMachineryTypeApi() {
        return GroupedOpenApi.builder()
                .group("农机类型")
                .pathsToMatch("/agriculturalMachineryType/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi agriculturalMachineryApi() {
        return GroupedOpenApi.builder()
                .group("农机找活")
                .pathsToMatch("/agriculturalMachinery/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi farmWorkApi() {
        return GroupedOpenApi.builder()
                .group("寻找农机")
                .pathsToMatch("/farmWork/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }


    @Bean
    public GroupedOpenApi fieldWorkApi() {
        return GroupedOpenApi.builder()
                .group("田地工作类型相关API")
                .pathsToMatch("/fieldWork/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi cropApi() {
        return GroupedOpenApi.builder()
                .group("作物相关API")
                .pathsToMatch("/crop/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi ProfileApi() {
        return GroupedOpenApi.builder()
                .group("我的管理")
                .pathsToMatch("/system/user/profile/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi fieldApi() {
        return GroupedOpenApi.builder()
                .group("地块管理")
                .pathsToMatch("/landPlot/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi landAddressApi() {
        return GroupedOpenApi.builder()
                .group("田块详细地址相关API")
                .pathsToMatch("/landAddress/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi trajectoryInfoApi() {
        return GroupedOpenApi.builder()
                .group("轨迹信息API")
                .pathsToMatch("/trajectoryInfo/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi effectiveTrajectoryApi() {
        return GroupedOpenApi.builder()
                .group("有效轨迹API")
                .pathsToMatch("/effectiveTrajectory/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }

    @Bean
    public GroupedOpenApi originalTrajectoryApi() {
        return GroupedOpenApi.builder()
                .group("原始轨迹API")
                .pathsToMatch("/originalTrajectory/**")
                .addOpenApiCustomizer(openApi -> openApi
                        .info(getApiInfo()))
                .build();
    }


    @Bean
    public SecurityScheme securityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .name("Authorization")
                .in(SecurityScheme.In.HEADER)
                .scheme("Bearer");
    }

    /**
     * 添加摘要信息
     */
    public Info getApiInfo() {
        return new Info()
                // 设置标题
                .title("标题：若依管理系统_接口文档")
                // 描述
                .description("描述：用于管理集团旗下公司的人员信息,具体包括XXX,XXX模块...")
                // 作者信息
                .contact(new Contact().name(globalConfig.getName()))
                // 版本
                .version("版本号:" + globalConfig.getVersion());
    }
}
