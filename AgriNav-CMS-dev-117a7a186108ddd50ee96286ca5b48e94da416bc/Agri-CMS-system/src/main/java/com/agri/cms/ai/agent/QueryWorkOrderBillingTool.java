package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.system.domain.ReviewRequest;
import com.agri.cms.system.domain.SettlementItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 作业单结算查询工具（A-13，读工具）。
 *
 * 查询某条作业单的结算明细（面积、单价、金额）与所属结算单，并自动附带
 * 规则依据与复核（扣减）记录。所有数值取数回填，不估算。
 *
 * @author agrinav
 */
@Component
public class QueryWorkOrderBillingTool implements AgentTool {

    private static final String TRAJECTORY_INFO_ID = "trajectory_info_id";

    private final ObjectMapper objectMapper;
    private final SettlementQueryService queryService;
    private final DataOwnershipChecker ownershipChecker;

    public QueryWorkOrderBillingTool(ObjectMapper objectMapper,
                                     SettlementQueryService queryService,
                                     DataOwnershipChecker ownershipChecker) {
        this.objectMapper = objectMapper;
        this.queryService = queryService;
        this.ownershipChecker = ownershipChecker;
    }

    @Override
    public String name() {
        return "query_work_order_billing";
    }

    @Override
    public String description() {
        return "查询某条作业单的结算明细，包含面积、单价、金额、所属结算单，以及规则依据和复核扣减原因。当用户问这个单能拿多少钱、这单怎么算的、为什么扣钱时调用。";
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
        ObjectNode id = objectMapper.createObjectNode();
        id.put("type", "string");
        id.put("description", "作业单号（如 WO- 开头或历史作业单号）");
        properties.set(TRAJECTORY_INFO_ID, id);
        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add(TRAJECTORY_INFO_ID);
        schema.set("required", required);
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String workOrderId = ToolArgs.toText(ToolArgs.get(arguments, TRAJECTORY_INFO_ID));
        if (workOrderId == null || workOrderId.isBlank()) {
            return ToolResult.fail("请提供要查询的作业单号。");
        }

        // 数据范围校验：普通用户仅能查询本人作业单，越权拒绝
        if (!ownershipChecker.canAccessWorkOrder(workOrderId)) {
            return ToolResult.fail("无权查看该作业单：这不是您本人的作业单，仅可查询自己的作业数据。");
        }

        WorkOrderBillingView view = queryService.getWorkOrderBilling(workOrderId);
        StringBuilder sb = new StringBuilder();

        if (view.settled()) {
            SettlementItem item = view.item();
            sb.append("作业单 ").append(workOrderId).append(" 的结算明细：")
                    .append("\n结算面积：").append(item.getArea()).append(" 亩")
                    .append("\n适用单价：").append(item.getUnitPrice()).append(" 元/亩（规则依据：")
                    .append(view.settlement() == null ? "见单价规则" : "结算单 " + view.settlement().getSettlementNo())
                    .append("）")
                    .append("\n结算金额：").append(item.getAmount()).append(" 元");
            if (view.settlement() != null) {
                sb.append("\n所属结算单：").append(view.settlement().getSettlementNo());
            }
        } else {
            sb.append("作业单 ").append(workOrderId).append(" 尚未进入结算，暂无可结算金额。");
        }

        // 复核（扣减）记录
        ReviewRequest review = view.reviewRequest();
        if (review != null) {
            sb.append("\n复核记录（").append(reviewStatusText(review.getStatus())).append("）：")
                    .append("\n系统核算面积：").append(review.getSystemArea()).append(" 亩")
                    .append("，机手主张面积：").append(review.getClaimedArea()).append(" 亩")
                    .append("\n机手异议：").append(review.getReason());
            if (review.getFinalComment() != null) {
                sb.append("\n终审意见（扣减原因）：").append(review.getFinalComment());
            } else if (review.getInitialComment() != null) {
                sb.append("\n初核意见：").append(review.getInitialComment());
            }
        }
        return ToolResult.ok(sb.toString());
    }

    private String reviewStatusText(Short status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "待初核";
            case 1 -> "初核通过";
            case 2 -> "初核驳回";
            case 3 -> "终审通过";
            case 4 -> "终审驳回";
            default -> "未知";
        };
    }
}
