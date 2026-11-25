package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 结束作业工具（A-12，写工具）。
 *
 * 农机手结束指定作业单，记录结束时间并将状态置为已完成，面积留待轨迹核算；
 * 写操作需二次确认，结束点超界时给出提醒。
 *
 * @author agrinav
 */
@Component
public class EndWorkTool implements AgentTool {

    private static final String TRAJECTORY_INFO_ID = "trajectory_info_id";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private final ObjectMapper objectMapper;
    private final WorkOrderService workOrderService;

    public EndWorkTool(ObjectMapper objectMapper, WorkOrderService workOrderService) {
        this.objectMapper = objectMapper;
        this.workOrderService = workOrderService;
    }

    @Override
    public String name() {
        return "end_work";
    }

    @Override
    public String description() {
        return "结束一项进行中的作业并记录结束时间。需要作业单号 trajectory_info_id，可选当前定位经纬度用于超界判断。当用户说结束作业、收工、干完了时调用。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of(TRAJECTORY_INFO_ID);
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode workOrderId = objectMapper.createObjectNode();
        workOrderId.put("type", "string");
        workOrderId.put("description", "要结束的作业单号（开始作业时返回的 WO- 单号）");
        properties.set(TRAJECTORY_INFO_ID, workOrderId);

        ObjectNode longitude = objectMapper.createObjectNode();
        longitude.put("type", "number");
        longitude.put("description", "当前定位经度（可选，用于超界判断）");
        properties.set(LONGITUDE, longitude);

        ObjectNode latitude = objectMapper.createObjectNode();
        latitude.put("type", "number");
        latitude.put("description", "当前定位纬度（可选，用于超界判断）");
        properties.set(LATITUDE, latitude);

        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add(TRAJECTORY_INFO_ID);
        schema.set("required", required);
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return true;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        Long userId = SecurityUtils.getUserId();

        String workOrderId = ToolArgs.toText(ToolArgs.get(arguments, TRAJECTORY_INFO_ID));
        Double longitude = ToolArgs.toDouble(ToolArgs.get(arguments, LONGITUDE));
        Double latitude = ToolArgs.toDouble(ToolArgs.get(arguments, LATITUDE));

        WorkOrderResult result = workOrderService.endWork(
                workOrderId, longitude, latitude, String.valueOf(userId));

        if (!result.success()) {
            return ToolResult.fail(result.message());
        }

        String content = result.message()
                + "，回执编号：" + result.receiptNo()
                + (result.boundaryWarning() == null ? "" : "。" + result.boundaryWarning());
        return ToolResult.ok(content, result.receiptNo());
    }
}
