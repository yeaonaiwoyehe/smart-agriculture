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
 * 任务认领工具（A-11，写工具）。
 *
 * 农机手认领指定农活，必须提供农活编号；因属于写操作，执行前由框架要求用户
 * 二次确认，确认后才真正落库，并返回认领回执。
 *
 * @author agrinav
 */
@Component
public class ClaimTaskTool implements AgentTool {

    private static final String FARM_WORK_ID = "farm_work_id";

    private final ObjectMapper objectMapper;
    private final TaskClaimService taskClaimService;

    public ClaimTaskTool(ObjectMapper objectMapper, TaskClaimService taskClaimService) {
        this.objectMapper = objectMapper;
        this.taskClaimService = taskClaimService;
    }

    @Override
    public String name() {
        return "claim_task";
    }

    @Override
    public String description() {
        return "认领指定的农活任务。需要农活编号 farm_work_id，认领成功后生成回执。当用户明确表达要认领/接下某个任务时调用。";
    }

    @Override
    public List<String> requiredParams() {
        return List.of(FARM_WORK_ID);
    }

    @Override
    public JsonNode parametersSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");

        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode farmWorkId = objectMapper.createObjectNode();
        farmWorkId.put("type", "string");
        farmWorkId.put("description", "要认领的农活编号（任务卡片上的 farmWorkId）");
        properties.set(FARM_WORK_ID, farmWorkId);
        schema.set("properties", properties);

        ArrayNode required = objectMapper.createArrayNode();
        required.add(FARM_WORK_ID);
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

        Object rawId = arguments.get(FARM_WORK_ID);
        String farmWorkId = rawId == null ? null : String.valueOf(rawId).trim();

        ClaimResult result = taskClaimService.claim(
                farmWorkId, String.valueOf(userId), userName, nickName);

        if (!result.success()) {
            return ToolResult.fail(result.message());
        }

        StringBuilder content = new StringBuilder();
        content.append(result.message());
        if (result.workName() != null) {
            content.append(" 任务名称：").append(result.workName());
        }
        content.append("，农活编号：").append(result.farmWorkId());
        if (result.receiptNo() != null) {
            content.append("，回执编号：").append(result.receiptNo());
        }
        return ToolResult.ok(content.toString(), result.receiptNo());
    }
}
