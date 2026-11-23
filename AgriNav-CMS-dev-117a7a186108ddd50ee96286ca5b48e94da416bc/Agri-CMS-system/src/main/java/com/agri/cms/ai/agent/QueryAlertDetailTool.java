package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 异常工单详情查询工具（A-14，读工具）。
 *
 * 按工单编号查询单个异常工单的处理人、处理时限、当前状态与处理结果；
 * 普通机手仅能查看本人工单。
 *
 * @author agrinav
 */
@Component
public class QueryAlertDetailTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(QueryAlertDetailTool.class);

    private static final String ALERT_NO = "alert_no";

    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    public QueryAlertDetailTool(ObjectMapper objectMapper, AlertService alertService) {
        this.objectMapper = objectMapper;
        this.alertService = alertService;
    }

    @Override
    public String name() {
        return "query_alert_detail";
    }

    @Override
    public String description() {
        return "按工单编号查询单个异常工单的详情，包括处理人、处理时限、当前状态与处理结果。当用户询问某个工单具体情况时调用。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of(ALERT_NO);
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode alertNo = objectMapper.createObjectNode();
        alertNo.put("type", "string");
        alertNo.put("description", "要查询的异常工单编号，如 AT-20261123-XXXX");
        properties.set(ALERT_NO, alertNo);
        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add(ALERT_NO);
        schema.set("required", required);
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        Long userId = SecurityUtils.getUserId();
        boolean isAdmin = SecurityUtils.isAdmin(userId);
        String alertNo = ToolArgs.toText(ToolArgs.get(arguments, ALERT_NO));

        if (alertNo == null || alertNo.isBlank()) {
            return ToolResult.fail("请提供要查询的异常工单编号。");
        }

        AgentAlert alert = alertService.getByNoForUser(alertNo, userId, isAdmin);
        if (alert == null) {
            return ToolResult.fail("未找到编号为 " + alertNo + " 的工单，或该工单不属于您，请核对后再试。");
        }
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(alert));
        } catch (Exception e) {
            log.error("[A-14] 序列化异常工单详情失败", e);
            return ToolResult.fail("工单详情整理失败，请稍后再试。");
        }
    }
}
