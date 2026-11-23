package com.agri.cms.ai.intent;

import com.agri.cms.ai.client.LlmClient;
import com.agri.cms.ai.prompt.PromptTemplateManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 意图识别服务（A-04）。
 *
 * 把用户的自然语言输入转换为结构化的 {@link IntentResult}：
 * 1. 用大模型做意图分类与参数抽取，要求其输出严格 JSON；
 * 2. 解析 JSON（兼容 Markdown 代码块包裹），解析失败兜底为 UNKNOWN；
 * 3. 校验该意图的必填参数，缺失时生成一次追问话术，不默认取最近一条记录。
 *
 * @author agrinav
 */
@Service
public class IntentRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(IntentRecognitionService.class);

    private static final String TEMPLATE_PATH = "prompts/common/intent-recognition.txt";
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    /** 参数名 → 中文称呼，用于拼装追问话术 */
    private static final Map<String, String> PARAM_DISPLAY_NAMES = Map.of(
            IntentParams.TASK_ID, "任务",
            IntentParams.WORK_ORDER_NO, "作业单号",
            IntentParams.DESCRIPTION, "具体情况");

    private final LlmClient llmClient;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IntentRecognitionService(LlmClient llmClient,
                                    PromptTemplateManager promptTemplateManager) {
        this.llmClient = llmClient;
        this.promptTemplateManager = promptTemplateManager;
    }

    /**
     * 识别用户输入，不限制所属端。
     */
    public IntentResult recognize(String userText) {
        return recognize(userText, null);
    }

    /**
     * 识别用户输入。
     *
     * @param userText 用户原话
     * @param side     所属端 ag / ma，为 null 时加载全部意图
     */
    public IntentResult recognize(String userText, String side) {
        if (userText == null || userText.isBlank()) {
            return IntentResult.unknown(userText);
        }
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("current_date", LocalDate.now().format(DATE_FORMATTER));
            variables.put("intent_catalog", buildCatalog(side));
            String systemPrompt = promptTemplateManager.renderTemplate(TEMPLATE_PATH, variables);

            // LlmClient 已内置重试与兜底，这里拿到的是模型输出或兜底文案
            String llmOutput = llmClient.chat(systemPrompt, userText);

            JsonNode root = objectMapper.readTree(extractJson(llmOutput));
            IntentType intent = IntentType.fromCode(root.path("intent").asText());
            double confidence = root.path("confidence").asDouble(0.0);
            Map<String, Object> parameters = parseParameters(root.get("parameters"));
            List<String> missingParams = checkMissingParams(intent, parameters);

            IntentResult result = IntentResult.of(intent, confidence, parameters, missingParams, userText);
            log.info("意图识别完成：{} 置信度={} 参数={} 缺失必填={}",
                    intent.getCode(), confidence, parameters, missingParams);
            return result;
        } catch (Exception e) {
            log.warn("意图识别解析失败，兜底为 UNKNOWN，原文：{}，原因：{}",
                    userText, e.getMessage());
            return IntentResult.unknown(userText);
        }
    }

    /**
     * 根据缺失的必填参数生成一次追问话术；参数齐全时返回空串。
     */
    public String buildFollowUpQuestion(IntentResult result) {
        if (result == null || !result.needFollowUp()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (String param : result.missingParams()) {
            names.add(PARAM_DISPLAY_NAMES.getOrDefault(param, param));
        }
        return "为了帮你准确处理，请告诉我" + String.join("、", names) + "是哪一个？";
    }

    /**
     * 构建意图清单文本，按 side 过滤，供识别提示词使用。
     */
    private String buildCatalog(String side) {
        StringBuilder catalog = new StringBuilder();
        for (IntentType type : IntentType.values()) {
            if (side != null && !side.isBlank()
                    && !"common".equals(type.getSide())
                    && !type.getSide().equalsIgnoreCase(side)) {
                continue;
            }
            catalog.append("- ").append(type.getCode())
                    .append("（").append(type.getDisplayName()).append("）：")
                    .append(type.getDescription());
            if (!type.getRequiredParams().isEmpty()) {
                catalog.append("，标准参数键：").append(String.join("、", type.getRequiredParams()));
            }
            catalog.append('\n');
        }
        return catalog.toString().stripTrailing();
    }

    /**
     * 从大模型输出中提取 JSON 文本：剥离 Markdown 代码块，截取最外层花括号内容。
     */
    private String extractJson(String output) {
        if (output == null) {
            throw new IllegalStateException("大模型输出为空");
        }
        String text = output.trim();
        int firstBrace = text.indexOf('{');
        int lastBrace = text.lastIndexOf('}');
        if (firstBrace < 0 || lastBrace < firstBrace) {
            throw new IllegalStateException("大模型输出中未找到 JSON 对象");
        }
        return text.substring(firstBrace, lastBrace + 1);
    }

    /**
     * 解析 parameters 节点为有序 Map，值统一转为字符串。
     */
    private Map<String, Object> parseParameters(JsonNode node) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                parameters.put(entry.getKey(), value.isNull() ? "" : value.asText());
            });
        }
        return parameters;
    }

    /**
     * 检查意图必填参数是否缺失（值为 null 或空白即视为缺失）。
     */
    private List<String> checkMissingParams(IntentType intent, Map<String, Object> parameters) {
        List<String> missing = new ArrayList<>();
        for (String required : intent.getRequiredParams()) {
            Object value = parameters.get(required);
            if (value == null || value.toString().isBlank()) {
                missing.add(required);
            }
        }
        return missing;
    }
}
