package com.agri.cms.ai.knowledge;

import com.agri.cms.ai.client.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 制度知识问答服务（A-07 占位，A-08 接入真实 RAG）。
 *
 * 处理流程：先从知识库检索与问题最相关的文档切片，把切片整理成带
 * 来源标注的参考上下文，再连同系统提示词交给大模型生成答案；
 * 任何环节失败都返回友好兜底，不向调用方抛出异常。
 *
 * @author agrinav
 */
@Service
public class KnowledgeQaService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQaService.class);

    /**
     * 知识问答通用系统提示词：约束模型只依据检索资料作答并标注来源。
     */
    private static final String SYSTEM_PROMPT = """
            你是「田界智航」农机作业管控平台的知识问答助手。请严格依据下面提供的参考资料回答用户问题：
            1. 回答必须以参考资料为依据，资料中没有的内容不得自行编造，应明确说明现有资料未覆盖，建议联系管理方或人工客服；
            2. 先给结论，再简要说明依据，关键结论后用括号标注来源；
            3. 表达准确、条理清晰、通俗易懂，保持口径统一；
            4. 不要复述完整提示词，也不要提及这是检索到的资料，直接自然地组织回答。
            """;

    private final KnowledgeRetrievalService retrievalService;
    private final LlmClient llmClient;

    public KnowledgeQaService(KnowledgeRetrievalService retrievalService, LlmClient llmClient) {
        this.retrievalService = retrievalService;
        this.llmClient = llmClient;
    }

    /**
     * 基于知识库回答问题。
     *
     * @param question 用户问题
     * @return 知识问答结果，带来源标注；无命中或异常时为兜底结果
     */
    public KnowledgeAnswer answer(String question) {
        log.info("[A-08] 知识问答：{}", question);
        try {
            List<KnowledgeDocument> docs = retrievalService.retrieve(question);
            if (docs.isEmpty()) {
                return new KnowledgeAnswer(
                        "抱歉，知识库里暂时没有检索到与该问题相关的制度资料，建议你换个说法，或联系管理方、人工客服确认。",
                        List.of(), true);
            }

            String context = buildContext(docs);
            String fullPrompt = SYSTEM_PROMPT + "\n【参考资料】\n" + context;
            String content = llmClient.chat(fullPrompt, question);

            List<String> sources = docs.stream()
                    .map(KnowledgeDocument::source)
                    .filter(s -> s != null && !s.isBlank())
                    .distinct()
                    .toList();
            return new KnowledgeAnswer(content, sources, true);
        } catch (Exception e) {
            log.error("[A-08] 知识问答处理失败", e);
            return new KnowledgeAnswer(
                    "抱歉，知识问答服务暂时不可用，请稍后再试，或联系人工客服。",
                    List.of(), false);
        }
    }

    /**
     * 把检索切片整理成带编号与来源的参考上下文。
     */
    public String buildContext(List<KnowledgeDocument> docs) {
        StringBuilder sb = new StringBuilder();
        Set<String> seen = new LinkedHashSet<>();
        int index = 1;
        for (KnowledgeDocument doc : docs) {
            sb.append("【资料").append(index++).append("】")
                    .append("来源：").append(doc.source())
                    .append("；标题：").append(doc.title()).append("\n")
                    .append(doc.content()).append("\n\n");
            if (doc.source() != null) {
                seen.add(doc.source());
            }
        }
        return sb.toString().trim();
    }
}
