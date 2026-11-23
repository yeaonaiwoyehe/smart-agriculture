package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 异常上报工具（A-14，AG-04，写工具）。
 *
 * 农机手提交异常工单，描述为必填；因属于写操作，执行前由框架要求用户二次确认，
 * 确认后真正落库，并返回工单编号、处理人与处理时限。
 *
 * @author agrinav
 */
@Component
public class SubmitAlertTool implements AgentTool {

    private static final String ALERT_TYPE = "alert_type";
    private static final String DESCRIPTION = "description";
    private static final String WORK_ORDER_ID = "work_order_id";
    private static final String PLOT_ID = "plot_id";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";
    private static final String OCCUR_TIME = "occur_time";

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ObjectMapper objectMapper;
    private final AlertService alertService;

    public SubmitAlertTool(ObjectMapper objectMapper, AlertService alertService) {
        this.objectMapper = objectMapper;
        this.alertService = alertService;
    }

    @Override
    public String name() {
        return "submit_alert";
    }

    @Override
    public String description() {
        return "提交一条异常上报工单。必须提供异常描述 description，可选异常类型、作业单号、地块、定位与发生时间。"
                + "当用户说机器陷车了、设备坏了、地里有石头、定位不准、要上报异常时调用。";
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

        ObjectNode type = objectMapper.createObjectNode();
        type.put("type", "string");
        type.put("description", "异常类型：设备故障/定位异常/作业争议/陷车/障碍物/其他，无法判断时填其他");
        properties.set(ALERT_TYPE, type);

        ObjectNode desc = objectMapper.createObjectNode();
        desc.put("type", "string");
        desc.put("description", "异常现象的具体描述（现象、发生时间、地点、作业单号）");
        properties.set(DESCRIPTION, desc);

        ObjectNode workOrder = objectMapper.createObjectNode();
        workOrder.put("type", "string");
        workOrder.put("description", "关联作业单号（可选）");
        properties.set(WORK_ORDER_ID, workOrder);

        ObjectNode plot = objectMapper.createObjectNode();
        plot.put("type", "integer");
        plot.put("description", "关联地块 ID（可选）");
        properties.set(PLOT_ID, plot);

        ObjectNode lng = objectMapper.createObjectNode();
        lng.put("type", "number");
        lng.put("description", "异常发生位置经度（可选）");
        properties.set(LONGITUDE, lng);

        ObjectNode lat = objectMapper.createObjectNode();
        lat.put("type", "number");
        lat.put("description", "异常发生位置纬度（可选）");
        properties.set(LATITUDE, lat);

        ObjectNode occur = objectMapper.createObjectNode();
        occur.put("type", "string");
        occur.put("description", "异常发生时间，格式 yyyy-MM-dd HH:mm:ss（可选）");
        properties.set(OCCUR_TIME, occur);

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
        Long userId = SecurityUtils.getUserId();
        String userName = SecurityUtils.getUsername();
        String nickName = SecurityUtils.getLoginUser().getUser().getNickName();

        String alertType = ToolArgs.toText(ToolArgs.get(arguments, ALERT_TYPE));
        String description = ToolArgs.toText(ToolArgs.get(arguments, DESCRIPTION));
        String workOrderId = ToolArgs.toText(ToolArgs.get(arguments, WORK_ORDER_ID));
        Long plotId = ToolArgs.toLong(ToolArgs.get(arguments, PLOT_ID));
        Double longitude = ToolArgs.toDouble(ToolArgs.get(arguments, LONGITUDE));
        Double latitude = ToolArgs.toDouble(ToolArgs.get(arguments, LATITUDE));
        LocalDateTime occurTime = parseTime(ToolArgs.toText(ToolArgs.get(arguments, OCCUR_TIME)));

        AlertSubmitResult result = alertService.submit(
                alertType, description, workOrderId, plotId, longitude, latitude,
                occurTime, String.valueOf(userId), userName, nickName);

        if (!result.success()) {
            return ToolResult.fail(result.message());
        }

        String content = result.message() + " 工单编号：" + result.alertNo()
                + "，处理人：" + result.handlerName()
                + "，预计处理时限：" + result.deadline().format(DT_FMT);
        return ToolResult.ok(content, result.alertNo());
    }

    private LocalDateTime parseTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.trim(), DT_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
