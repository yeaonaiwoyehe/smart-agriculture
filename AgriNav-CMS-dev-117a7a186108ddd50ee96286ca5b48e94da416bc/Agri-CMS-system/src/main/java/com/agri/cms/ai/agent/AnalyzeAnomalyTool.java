package com.agri.cms.ai.agent;

import com.agri.cms.ai.tool.AgentTool;
import com.agri.cms.ai.tool.ToolResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 异常研判查询工具（A-17 MA-02，读工具，仅管理员）。
 *
 * 汇总异常工单、超时派单、面积争议与作业停滞，给出区分数据事实与
 * 可能原因的可追溯研判，只分析不修改数据。
 *
 * @author agrinav
 */
@Component
public class AnalyzeAnomalyTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(AnalyzeAnomalyTool.class);

    private final ObjectMapper objectMapper;
    private final AnomalyInsightService insightService;

    public AnalyzeAnomalyTool(ObjectMapper objectMapper,
                              AnomalyInsightService insightService) {
        this.objectMapper = objectMapper;
        this.insightService = insightService;
    }

    @Override
    public String name() {
        return "analyze_anomaly";
    }

    @Override
    public String description() {
        return "对系统中的作业异常进行综合研判，包括异常工单、超时派单、面积争议和作业停滞，"
                + "给出数据事实、可能原因与可追溯线索。当管理人员要求分析异常、研判问题原因时调用。"
                + "只分析不修改数据。";
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
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        try {
            AnomalyInsightResult result = insightService.analyze();
            return ToolResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("[A-17] 异常研判生成失败", e);
            return ToolResult.fail("异常研判暂时无法生成，请稍后再试。");
        }
    }
}
