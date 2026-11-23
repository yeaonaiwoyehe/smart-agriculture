package com.agri.cms.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * 可被大模型调用的业务工具抽象（A-05）。
 *
 * 每个工具声明自己的名称、用途、参数 JSON Schema、必填参数与是否为写操作；
 * 实现类交给 Spring 管理后，会被 {@link ToolRegistry} 自动收集注册。
 *
 * 工具本身不做权限放行，执行前由框架统一校验参数，写操作还需经过一次二次确认。
 *
 * @author agrinav
 */
public interface AgentTool {

    /**
     * 工具名，全局唯一，需符合 a-z A-Z 0-9 下划线/短横线。
     */
    String name();

    /**
     * 工具用途说明，供大模型判断何时调用。
     */
    String description();

    /**
     * 必填参数名，框架据此校验，缺失则不执行。
     */
    List<String> requiredParams();

    /**
     * 参数的 JSON Schema，用于生成请求中的 tools 定义。
     */
    JsonNode parametersSchema();

    /**
     * 是否为写操作；写操作必须经过用户二次确认后才执行。
     */
    boolean isWriteOperation();

    /**
     * 执行工具，参数已通过框架校验。
     *
     * @param arguments 调用参数
     * @return 执行结果，包含回传文本或回执编号
     */
    ToolResult execute(Map<String, Object> arguments);
}
