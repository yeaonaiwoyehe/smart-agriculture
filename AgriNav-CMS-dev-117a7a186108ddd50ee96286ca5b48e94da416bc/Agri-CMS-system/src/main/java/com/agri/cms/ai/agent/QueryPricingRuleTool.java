package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.agri.cms.system.domain.FieldWork;
import com.agri.cms.system.domain.PricingRule;
import com.agri.cms.system.mapper.FieldWorkMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 单价规则查询工具（A-13，读工具）。
 *
 * 查询启用的作业单价规则，可按作业类型过滤；展示规则名称、单价、适用面积区间。
 * 用于回答"单价是多少、规则依据是什么"。
 *
 * @author agrinav
 */
@Component
public class QueryPricingRuleTool implements AgentTool {

    private static final String WORK_TYPE = "work_type";

    private final ObjectMapper objectMapper;
    private final SettlementQueryService queryService;
    private final FieldWorkMapper fieldWorkMapper;

    public QueryPricingRuleTool(ObjectMapper objectMapper,
                                SettlementQueryService queryService,
                                FieldWorkMapper fieldWorkMapper) {
        this.objectMapper = objectMapper;
        this.queryService = queryService;
        this.fieldWorkMapper = fieldWorkMapper;
    }

    @Override
    public String name() {
        return "query_pricing_rule";
    }

    @Override
    public String description() {
        return "查询作业单价规则，可按作业类型过滤，展示单价（元/亩）和适用面积区间。当用户问耕地一亩多少钱、单价怎么定的、规则依据时调用。";
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
        ObjectNode workType = objectMapper.createObjectNode();
        workType.put("type", "integer");
        workType.put("description", "作业类型 ID（4001-4006，可选，不填则返回全部单价规则）");
        properties.set(WORK_TYPE, workType);
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        Long workType = ToolArgs.toLong(ToolArgs.get(arguments, WORK_TYPE));
        List<PricingRule> rules = queryService.listPricingRules(workType);

        if (rules.isEmpty()) {
            return ToolResult.ok(workType == null
                    ? "当前暂无启用的单价规则。"
                    : "作业类型 " + workType + " 暂无启用的单价规则。");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("当前启用的单价规则（共 ").append(rules.size()).append(" 条）：");
        int i = 1;
        for (PricingRule rule : rules) {
            String workName = resolveWorkName(rule.getWorkType());
            sb.append("\n").append(i++).append(". ").append(rule.getRuleName())
                    .append("，作业类型：").append(workName)
                    .append("，单价：").append(rule.getUnitPrice()).append(" 元/亩")
                    .append("，适用面积：").append(rule.getMinArea() == null ? "0" : rule.getMinArea())
                    .append(" 亩至 ")
                    .append(rule.getMaxArea() == null ? "不限" : rule.getMaxArea() + " 亩");
        }
        return ToolResult.ok(sb.toString());
    }

    private String resolveWorkName(Long workType) {
        if (workType == null) {
            return "未知";
        }
        FieldWork work = fieldWorkMapper.selectById(workType);
        return work == null ? String.valueOf(workType) : work.getWorkName() + "（" + workType + "）";
    }
}
