package com.agri.cms.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 发送给大模型的工具定义，对应请求体 tools 数组的一项。
 *
 * @param type      工具类型，目前固定为 function
 * @param function  函数元数据：名称、说明与 JSON Schema 参数描述
 * @author agrinav
 */
public record ToolDefinition(
        String type,
        FunctionDef function
) {

    /**
     * 函数元数据。
     *
     * @param name        函数名，需符合 a-z A-Z 0-9 下划线/短横线，且全局唯一
     * @param description 函数用途说明，模型据此判断何时调用
     * @param parameters  参数的 JSON Schema；无参数时给空对象 schema
     */
    public record FunctionDef(
            String name,
            String description,
            JsonNode parameters
    ) {
        public static FunctionDef of(String name, String description, JsonNode parameters) {
            return new FunctionDef(name, description, parameters);
        }
    }

    /**
     * 构造一个 function 类型的工具定义。
     */
    public static ToolDefinition function(FunctionDef functionDef) {
        return new ToolDefinition("function", functionDef);
    }
}
