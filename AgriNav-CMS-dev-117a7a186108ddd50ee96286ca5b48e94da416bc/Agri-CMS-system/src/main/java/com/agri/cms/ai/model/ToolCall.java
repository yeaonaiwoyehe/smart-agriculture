package com.agri.cms.ai.model;

/**
 * 大模型返回的工具调用，对应响应 message 中 tool_calls 数组的一项。
 *
 * @param id        工具调用编号，执行工具后需用它作为 tool_call_id 回传结果
 * @param type      调用类型，目前固定为 function
 * @param function  模型请求调用的函数名与参数（JSON 字符串）
 * @author agrinav
 */
public record ToolCall(
        String id,
        String type,
        FunctionCall function
) {

    /**
     * 模型请求调用的函数。
     *
     * @param name      函数名，对应已注册的工具名
     * @param arguments 调用参数，JSON 字符串，可能不合法，执行前需校验
     */
    public record FunctionCall(
            String name,
            String arguments
    ) {
    }
}
