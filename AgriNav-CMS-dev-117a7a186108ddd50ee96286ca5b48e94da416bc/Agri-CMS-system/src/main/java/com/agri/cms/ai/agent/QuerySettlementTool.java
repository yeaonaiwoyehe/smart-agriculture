package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.domain.Settlement;
import com.agri.cms.system.domain.SettlementItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 月度结算查询工具（A-13，读工具）。
 *
 * 查询当前用户指定年月（默认本月）的结算单及其全部明细，含总亩数、总金额。
 * 全部取数回填，不估算。
 *
 * @author agrinav
 */
@Component
public class QuerySettlementTool implements AgentTool {

    private static final String YEAR = "year";
    private static final String MONTH = "month";

    private final ObjectMapper objectMapper;
    private final SettlementQueryService queryService;

    public QuerySettlementTool(ObjectMapper objectMapper, SettlementQueryService queryService) {
        this.objectMapper = objectMapper;
        this.queryService = queryService;
    }

    @Override
    public String name() {
        return "query_settlement";
    }

    @Override
    public String description() {
        return "查询我某月（默认本月）的结算单和全部明细，包含总亩数、总金额。当用户问这个月挣了多少、本月结算、我的结算单时调用。";
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

        ObjectNode year = objectMapper.createObjectNode();
        year.put("type", "integer");
        year.put("description", "年份（可选，默认当前年）");
        properties.set(YEAR, year);

        ObjectNode month = objectMapper.createObjectNode();
        month.put("type", "integer");
        month.put("description", "月份 1-12（可选，默认当前月）");
        properties.set(MONTH, month);

        schema.set("properties", properties);
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String phone = SecurityUtils.getUsername();
        LocalDate now = LocalDate.now();

        Integer year = ToolArgs.toInt(ToolArgs.get(arguments, YEAR));
        Integer month = ToolArgs.toInt(ToolArgs.get(arguments, MONTH));
        int queryYear = year == null ? now.getYear() : year;
        int queryMonth = month == null ? now.getMonthValue() : month;
        if (queryMonth < 1 || queryMonth > 12) {
            return ToolResult.fail("月份取值应为 1-12，请核对。");
        }

        MonthlySettlementView view = queryService.getMonthlySettlement(phone, queryYear, queryMonth);
        if (view.settlement() == null) {
            return ToolResult.ok(queryYear + "年" + queryMonth + "月暂无您的结算单。");
        }

        Settlement s = view.settlement();
        StringBuilder sb = new StringBuilder();
        sb.append(queryYear).append("年").append(queryMonth).append("月结算单：")
                .append("\n结算单号：").append(s.getSettlementNo())
                .append("\n结算周期：").append(s.getPeriodStart()).append(" 至 ").append(s.getPeriodEnd())
                .append("\n结算状态：").append(settlementStatusText(s.getStatus()))
                .append("\n结算总亩数：").append(s.getTotalArea()).append(" 亩")
                .append("\n结算总金额：").append(s.getTotalAmount()).append(" 元")
                .append("\n明细（共 ").append(view.items().size()).append(" 条）：");

        int i = 1;
        for (SettlementItem item : view.items()) {
            sb.append("\n").append(i++).append(". 作业单 ").append(item.getTrajectoryInfoId())
                    .append("，面积 ").append(item.getArea()).append(" 亩")
                    .append("，单价 ").append(item.getUnitPrice()).append(" 元/亩")
                    .append("，金额 ").append(item.getAmount()).append(" 元");
        }
        return ToolResult.ok(sb.toString());
    }

    private String settlementStatusText(Short status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "已生成";
            case 1 -> "已确认";
            case 2 -> "已付款";
            default -> "未知";
        };
    }
}
