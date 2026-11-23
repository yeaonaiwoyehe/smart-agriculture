package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 我的异常工单查询工具（A-14，读工具）。
 *
 * 农机手查询本人上报的异常工单及处理进度，可按状态过滤；管理员可查看全部工单。
 *
 * @author agrinav
 */
@Component
public class QueryMyAlertsTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(QueryMyAlertsTool.class);

    private static final String STATUS = "status";

    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    public QueryMyAlertsTool(ObjectMapper objectMapper, AlertService alertService) {
        this.objectMapper = objectMapper;
        this.alertService = alertService;
    }

    @Override
    public String name() {
        return "query_my_alerts";
    }

    @Override
    public String description() {
        return "查询我上报的异常工单及处理进度，可按状态过滤（PENDING待处理/PROCESSING处理中/RESOLVED已处理/CLOSED已关闭）。"
                + "当用户说我的异常工单、工单进度、上报的问题处理了吗时调用。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of();
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode status = objectMapper.createObjectNode();
        status.put("type", "string");
        status.put("description", "工单状态过滤（可选）：PENDING/PROCESSING/RESOLVED/CLOSED");
        properties.set(STATUS, status);
        schema.set("properties", properties);
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
        String status = ToolArgs.toText(ToolArgs.get(arguments, STATUS));

        List<AgentAlert> list = isAdmin
                ? alertService.listAll(status)
                : alertService.listMyAlerts(String.valueOf(userId), status);

        if (list.isEmpty()) {
            return ToolResult.ok("暂无符合条件的异常工单。");
        }
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(list));
        } catch (Exception e) {
            log.error("[A-14] 序列化异常工单列表失败", e);
            return ToolResult.fail("工单结果整理失败，请稍后再试。");
        }
    }
}
