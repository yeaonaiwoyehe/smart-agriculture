package com.agri.cms.ai.agent;

import com.agri.cms.ai.knowledge.FaqDocument;
import com.agri.cms.ai.knowledge.FaqIndexService;
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
 * 常见问题话术查询工具（A-15 AG-05，读工具）。
 *
 * 按机手问题检索固定话术，返回统一口径的标准回答，
 * 由大模型用亲切口吻转述；无命中时引导联系人工客服。
 *
 * @author agrinav
 */
@Component
public class QueryFaqTool implements AgentTool {

    private static final Logger log = LoggerFactory.getLogger(QueryFaqTool.class);

    private static final String QUESTION = "question";

    private final ObjectMapper objectMapper;
    private final FaqIndexService faqIndexService;

    public QueryFaqTool(ObjectMapper objectMapper, FaqIndexService faqIndexService) {
        this.objectMapper = objectMapper;
        this.faqIndexService = faqIndexService;
    }

    @Override
    public String name() {
        return "query_faq";
    }

    @Override
    public String description() {
        return "查询平台常见问题的统一话术答案，例如怎么结算、多久到账、怎么认领任务、忘记密码、怎么联系人工客服等。"
                + "当用户咨询通用操作或流程类问题时优先调用，以保证口径一致。";
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
        ObjectNode question = objectMapper.createObjectNode();
        question.put("type", "string");
        question.put("description", "用户要咨询的常见问题（口语化即可）");
        properties.set(QUESTION, question);
        schema.set("properties", properties);
        return schema;
    }

    @Override
    public boolean isWriteOperation() {
        return false;
    }

    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String question = ToolArgs.toText(ToolArgs.get(arguments, QUESTION));
        if (question == null || question.isBlank()) {
            return ToolResult.fail("请告诉我您想咨询的具体问题，我好帮您查对应的说明。");
        }

        List<FaqDocument> docs = faqIndexService.search(question, 3);
        if (docs.isEmpty()) {
            return ToolResult.ok("暂时没有匹配到对应的标准话术，建议您拨打人工客服电话400-888-0000（每天8:00-20:00），由客服人工解答。");
        }
        try {
            return ToolResult.ok(objectMapper.writeValueAsString(docs));
        } catch (Exception e) {
            log.error("[A-15] 序列化 FAQ 话术失败", e);
            return ToolResult.fail("话术结果整理失败，请稍后再试。");
        }
    }
}
