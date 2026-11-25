package com.agri.cms.ai.tool.sample;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 示例读工具：获取当前系统日期时间。
 *
 * 用于验证工具调用框架的读链路（模型发起调用 → 直接执行 → 结果回传），
 * 后续会替换为真实业务查询工具。
 *
 * @author agrinav
 */
@Component
public class GetCurrentTimeTool implements AgentTool {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String name() {
        return "get_current_time";
    }

    @Override
    public String description() {
        return "获取当前系统的日期和时间。当用户询问现在几点、当前时间时调用。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of();
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", objectMapper.createObjectNode());
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String now = LocalDateTime.now().format(FORMATTER);
        return ToolResult.ok("当前系统时间是 " + now);
    }
}
