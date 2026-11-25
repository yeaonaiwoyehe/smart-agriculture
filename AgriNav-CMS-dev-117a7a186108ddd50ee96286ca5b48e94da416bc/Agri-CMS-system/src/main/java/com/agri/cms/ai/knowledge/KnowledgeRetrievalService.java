package com.agri.cms.ai.knowledge;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 知识库检索服务（A-08 RAG）。
 *
 * 用 multi_match 在标题（加权）与正文中按 BM25 相关性检索，
 * 返回最相关的若干切片，供大模型生成答案。
 *
 * @author agrinav
 */
@Service
public class KnowledgeRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalService.class);

    /** 默认返回的切片数量 */
    private static final int DEFAULT_TOP_K = 4;

    private final ElasticsearchClient client;

    public KnowledgeRetrievalService(ElasticsearchClient client) {
        this.client = client;
    }

    /**
     * 按默认数量检索与问题最相关的切片。
     */
    public List<KnowledgeDocument> retrieve(String question) {
        return retrieve(question, DEFAULT_TOP_K);
    }

    /**
     * 检索与问题最相关的切片。
     *
     * @param question 用户问题
     * @param topK     返回切片数量
     * @return 相关切片；检索异常时返回空列表由上层兜底
     */
    public List<KnowledgeDocument> retrieve(String question, int topK) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        try {
            SearchResponse<KnowledgeDocument> response = client.search(s -> s
                    .index(KnowledgeIndexService.INDEX)
                    .size(topK)
                    .query(q -> q.multiMatch(m -> m
                            .query(question)
                            .fields("title^2", "content"))),
                    KnowledgeDocument.class);

            List<KnowledgeDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(d -> d != null)
                    .toList();
            log.info("[A-08] 检索“{}”命中 {} 个切片", question, docs.size());
            return docs;
        } catch (IOException e) {
            log.error("[A-08] 知识库检索失败，将走兜底", e);
            return List.of();
        }
    }
}
