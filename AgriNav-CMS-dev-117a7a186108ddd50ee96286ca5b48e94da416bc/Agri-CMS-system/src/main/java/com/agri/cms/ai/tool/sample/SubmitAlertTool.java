package com.agri.cms.ai.tool.sample;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ReceiptGenerator;
import com.agri.cms.ai.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 示例写工具：提交异常上报工单。
 *
 * 用于验证写操作的二次确认链路：模型请求调用 → 框架暂存并要求确认 →
 * 用户确认后才真正执行，并生成回执编号。
 *
 * @author agrinav
 */
@Component
public class SubmitAlertTool implements AgentTool {

    private static final String DESCRIPTION = "description";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReceiptGenerator receiptGenerator;

    public SubmitAlertTool(ReceiptGenerator receiptGenerator) {
        this.receiptGenerator = receiptGenerator;
    }

    @Override
    public String name() {
        return "submit_alert";
    }

    @Override
    public String description() {
        return "提交一条异常上报工单。需要用户提供异常情况描述。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of(DESCRIPTION);
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode desc = objectMapper.createObjectNode();
        desc.put("type", "string");
        desc.put("description", "异常情况的具体描述");
        properties.set(DESCRIPTION, desc);
        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add(DESCRIPTION);
        schema.set("required", required);
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return true;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String receiptNo = receiptGenerator.next();
        // 这里演示工单生成，后续接入真实异常工单服务
        String content = "异常工单已提交，描述：" + arguments.get(DESCRIPTION)
                + "，回执编号：" + receiptNo;
        return ToolResult.ok(content, receiptNo);
    }
}
