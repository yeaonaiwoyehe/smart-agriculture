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
 * 今日任务查询工具（A-11，读工具）。
 *
 * 农机手查询当前可认领任务与我的任务；可携带定位经纬度，用于计算到地块的距离。
 * 无必填参数，定位为可选；返回结构化任务卡片，由大模型组织成自然语言。
 *
 * @author agrinav
 */
@Component
public class QueryTodayTasksTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(QueryTodayTasksTool.class);

    private final ObjectMapper objectMapper;
    private final TaskQueryService taskQueryService;

    public QueryTodayTasksTool(ObjectMapper objectMapper, TaskQueryService taskQueryService) {
        this.objectMapper = objectMapper;
        this.taskQueryService = taskQueryService;
    }

    @Override
    public String name() {
        return "query_today_tasks";
    }

    @Override
    public String description() {
        return "查询当前可认领的农活任务以及我已认领的任务。可传入当前定位经纬度，用于计算到各地块的距离并按距离排序。当用户说看看任务、有什么活、附近的活、我的任务时调用。";
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

        ObjectNode longitude = objectMapper.createObjectNode();
        longitude.put("type", "number");
        longitude.put("description", "农机手当前定位经度（可选，用于计算距离）");
        properties.set("longitude", longitude);

        ObjectNode latitude = objectMapper.createObjectNode();
        latitude.put("type", "number");
        latitude.put("description", "农机手当前定位纬度（可选，用于计算距离）");
        properties.set("latitude", latitude);

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
        Double longitude = toDouble(arguments.get("longitude"));
        Double latitude = toDouble(arguments.get("latitude"));

        TaskQueryResult result = taskQueryService.queryTasks(
                String.valueOf(userId), longitude, latitude);

        try {
            String json = objectMapper.writeValueAsString(result);
            return ToolResult.ok(json);
        } catch (Exception e) {
            log.error("[A-11] 序列化任务查询结果失败", e);
            return ToolResult.fail("任务结果整理失败，请稍后再试。");
        }
    }

    /**
     * 把工具参数中的定位值健壮地转为 Double（LLM 可能给数字或字符串）。
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            String text = String.valueOf(value).trim();
            return text.isEmpty() ? null : Double.parseDouble(text);
        } catch (NumberFormatException e) {
            log.warn("[A-11] 无法解析的定位参数：{}", value);
            return null;
        }
    }
}
