package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 我的作业单查询工具（A-12，读工具）。
 *
 * 查询当前用户的全部作业单，按开始时间倒序，展示单号、状态、地块、类型、
 * 起止时间与面积（待核算时明确标注）。读操作直接执行，无需确认。
 *
 * @author agrinav
 */
@Component
public class QueryMyWorkOrdersTool implements AgentTool {

    private final ObjectMapper objectMapper;
    private final WorkOrderService workOrderService;

    public QueryMyWorkOrdersTool(ObjectMapper objectMapper, WorkOrderService workOrderService) {
        this.objectMapper = objectMapper;
        this.workOrderService = workOrderService;
    }

    @Override
    public String name() {
        return "query_my_work_orders";
    }

    @Override
    public String description() {
        return "查询我的作业单列表，包含单号、状态、地块、作业类型、起止时间和面积。当用户问我的作业、我有哪些作业单、作业记录时调用。";
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
        String userId = String.valueOf(SecurityUtils.getUserId());
        List<WorkOrderCard> cards = workOrderService.listMyWorkOrders(userId);
        if (cards.isEmpty()) {
            return ToolResult.ok("您目前还没有作业单。可以对我说“开始作业”来创建一条作业单。");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("您共有 ").append(cards.size()).append(" 条作业单：");
        for (int i = 0; i < cards.size(); i++) {
            WorkOrderCard c = cards.get(i);
            sb.append("\n").append(i + 1).append(". 单号 ").append(c.workOrderNo())
                    .append("（").append(c.statusText()).append("）");
            if (c.plotName() != null) {
                sb.append("，地块：").append(c.plotName());
            }
            if (c.workTypeName() != null) {
                sb.append("，类型：").append(c.workTypeName());
            }
            if (c.startTime() != null) {
                sb.append("，开始：").append(c.startTime());
            }
            if (c.endTime() != null) {
                sb.append("，结束：").append(c.endTime());
            }
            if (c.areaPending()) {
                sb.append("，面积：待核算");
            } else {
                sb.append("，面积：").append(c.area()).append(" 亩");
            }
        }
        return ToolResult.ok(sb.toString());
    }
}
