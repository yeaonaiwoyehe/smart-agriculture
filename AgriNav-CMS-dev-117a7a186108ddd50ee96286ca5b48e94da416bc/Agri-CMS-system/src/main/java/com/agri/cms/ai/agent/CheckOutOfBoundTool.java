package com.agri.cms.ai.agent;

import com.agri.cms.ai.agent.mapper.WorkOrderGeoMapper;
import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 超界判断工具（A-12，读工具，B-09 空间判断）。
 *
 * 判断定位点是否在地块边界内。可显式指定地块；不指定时，自动取当前用户
 * 进行中作业单对应的地块。读操作直接执行，无需确认。
 *
 * @author agrinav
 */
@Component
public class CheckOutOfBoundTool implements AgentTool {

    private static final String PLOT_ID = "plot_id";
    private static final String LONGITUDE = "longitude";
    private static final String LATITUDE = "latitude";

    private final ObjectMapper objectMapper;
    private final WorkOrderGeoMapper geoMapper;
    private final WorkOrderService workOrderService;
    private final LandPlotMapper landPlotMapper;

    public CheckOutOfBoundTool(ObjectMapper objectMapper,
                               WorkOrderGeoMapper geoMapper,
                               WorkOrderService workOrderService,
                               LandPlotMapper landPlotMapper) {
        this.objectMapper = objectMapper;
        this.geoMapper = geoMapper;
        this.workOrderService = workOrderService;
        this.landPlotMapper = landPlotMapper;
    }

    @Override
    public String name() {
        return "check_out_of_bound";
    }

    @Override
    public String description() {
        return "判断当前定位点是否在地块边界内。需要经纬度；plot_id 可选，不填则针对当前进行中的作业单。当用户问超界了没、我在不在地里、有没有越界时调用。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of(LONGITUDE, LATITUDE);
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();

        ObjectNode longitude = objectMapper.createObjectNode();
        longitude.put("type", "number");
        longitude.put("description", "当前定位经度");
        properties.set(LONGITUDE, longitude);

        ObjectNode latitude = objectMapper.createObjectNode();
        latitude.put("type", "number");
        latitude.put("description", "当前定位纬度");
        properties.set(LATITUDE, latitude);

        ObjectNode plotId = objectMapper.createObjectNode();
        plotId.put("type", "integer");
        plotId.put("description", "地块编号（可选，不填则用进行中作业单的地块）");
        properties.set(PLOT_ID, plotId);

        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add(LONGITUDE);
        required.add(LATITUDE);
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
        Double longitude = ToolArgs.toDouble(ToolArgs.get(arguments, LONGITUDE));
        Double latitude = ToolArgs.toDouble(ToolArgs.get(arguments, LATITUDE));
        Long plotId = ToolArgs.toLong(ToolArgs.get(arguments, PLOT_ID));

        if (longitude == null || latitude == null) {
            return ToolResult.fail("超界判断需要当前定位的经纬度，请先开启定位。");
        }

        try {com.agri.cms.system.service.boundary.BoundaryCoordinates.validate(longitude,latitude);}
        catch(IllegalArgumentException invalid){return ToolResult.fail(invalid.getMessage());}
        // 未指定地块时，取进行中作业单的地块
        if (plotId == null) {
            TrajectoryInfo active = workOrderService.findActiveWorkOrder(String.valueOf(userId));
            if (active == null || active.getPlotId() == null) {
                return ToolResult.fail("您当前没有进行中的作业单，请指定要核对的地块编号。");
            }
            plotId = active.getPlotId();
        }

        LandPlot plot = landPlotMapper.selectById(plotId);
        if (plot == null || plot.getIsDeleted()!=0) {
            return ToolResult.fail("未找到地块 " + plotId + "，请核对编号。");
        }

        Integer inside;
        try {inside=geoMapper.countPointInside(plotId,longitude,latitude);}
        catch(RuntimeException unavailable){return ToolResult.fail("地块边界暂时无法核对，请检查边界数据后重试。");}
        boolean inBound = inside != null && inside > 0;

        String content = inBound
                ? "当前定位点在地块「" + plot.getLandName() + "」边界内，未超界。"
                : "当前定位点不在地块「" + plot.getLandName() + "」边界内，可能已超界，请及时调整作业位置。";
        return ToolResult.ok(content);
    }
}
