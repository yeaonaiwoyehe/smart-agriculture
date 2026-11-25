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
 * 开始作业工具（A-12，写工具）。
 *
 * 农机手在指定地块、按指定作业类型开始作业，可携带幅宽与开始定位；
 * 写操作需二次确认，开始点超界时给出提醒。
 *
 * @author agrinav
 */
@Component
public class StartWorkTool implements AgentTool {

    private static final String PLOT_ID = "plot_id";
    private static final String WORK_TYPE = "work_type";
    private static final String WIDTH = "width";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private final ObjectMapper objectMapper;
    private final WorkOrderService workOrderService;

    public StartWorkTool(ObjectMapper objectMapper, WorkOrderService workOrderService) {
        this.objectMapper = objectMapper;
        this.workOrderService = workOrderService;
    }

    @Override
    public String name() {
        return "start_work";
    }

    @Override
    public String description() {
        return "开始一项作业并生成作业单号。需要地块编号 plot_id 和作业类型 work_type（4001-4006），可选幅宽 width 与当前定位经纬度。当用户说开始作业、开始干活、下地干活时调用。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of(PLOT_ID, WORK_TYPE);
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode plotId = objectMapper.createObjectNode();
        plotId.put("type", "integer");
        plotId.put("description", "地块编号");
        properties.set(PLOT_ID, plotId);

        ObjectNode workType = objectMapper.createObjectNode();
        workType.put("type", "integer");
        workType.put("description", "作业类型编号（4001 耕地、4002 旋耕、4003 播种、4004 植保、4005 收割、4006 秸秆还田）");
        properties.set(WORK_TYPE, workType);

        ObjectNode width = objectMapper.createObjectNode();
        width.put("type", "integer");
        width.put("description", "作业幅宽（厘米，可选）");
        properties.set(WIDTH, width);

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
        required.add(PLOT_ID);
        required.add(WORK_TYPE);
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
        String userName = SecurityUtils.getUsername();

        Long plotId = ToolArgs.toLong(ToolArgs.get(arguments, PLOT_ID));
        Long workType = ToolArgs.toLong(ToolArgs.get(arguments, WORK_TYPE));
        Integer width = ToolArgs.toInt(ToolArgs.get(arguments, WIDTH));
        Double longitude = ToolArgs.toDouble(ToolArgs.get(arguments, LONGITUDE));
        Double latitude = ToolArgs.toDouble(ToolArgs.get(arguments, LATITUDE));

        WorkOrderResult result = workOrderService.startWork(
                plotId, workType, width, longitude, latitude,
                String.valueOf(userId), userName);

        if (!result.success()) {
            return ToolResult.fail(result.message());
        }

        String content = result.message()
                + "，作业单号：" + result.workOrderNo()
                + "，回执编号：" + result.receiptNo()
                + (result.boundaryWarning() == null ? "" : "。" + result.boundaryWarning());
        return ToolResult.ok(content, result.receiptNo());
    }
}
