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
 * 调度建议查询工具（A-16 MA-01，读工具，仅管理员）。
 *
 * 返回待派农活、机手候选评估、可用农机与推荐结论，
 * 仅提供建议，不直接派单。
 *
 * @author agrinav
 */
@Component
public class GetDispatchSuggestionTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(GetDispatchSuggestionTool.class);

    private final ObjectMapper objectMapper;
    private final DispatchSuggestionService suggestionService;

    public GetDispatchSuggestionTool(ObjectMapper objectMapper,
                                     DispatchSuggestionService suggestionService) {
        this.objectMapper = objectMapper;
        this.suggestionService = suggestionService;
    }

    @Override
    public String name() {
        return "get_dispatch_suggestion";
    }

    @Override
    public String description() {
        return "获取作业调度派单建议，包括待派农活、空闲机手、历史完成率与可用农机运力，并给出推荐排序和依据。"
                + "当管理人员询问该把活派给谁、给个调度建议时调用。仅提供建议，不直接派单。";
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
            DispatchSuggestionResult result = suggestionService.suggest();
            return ToolResult.ok(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            log.error("[A-16] 调度建议生成失败", e);
            return ToolResult.fail("调度建议暂时无法生成，请稍后再试。");
        }
    }
}
