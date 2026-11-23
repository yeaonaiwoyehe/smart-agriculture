package com.agri.cms.ai.knowledge;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * 常见问题索引与检索服务（A-15 AG-05）。
 *
 * 启动时把固定话术加载到独立 ES 索引，query_faq 工具按问题
 * 检索最匹配的标准话术，保证回答口径统一。
 *
 * @author agrinav
 */
@Service
public class FaqIndexService {

    private static final Logger log = LoggerFactory.getLogger(FaqIndexService.class);

    /** 常见问题索引名 */
    public static final String INDEX = "agri_faq";

    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper;

    public FaqIndexService(ElasticsearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initOnStartup() {
        try {
            ensureIndex();
            long count = count();
            if (count == 0) {
                long loaded = rebuild();
                log.info("[A-15] 常见问题初始化完成，写入 {} 条话术", loaded);
            } else {
                log.info("[A-15] 常见问题已存在 {} 条，跳过初始化", count);
            }
        } catch (Exception e) {
            log.error("[A-15] 常见问题初始化失败，工具将走兜底", e);
        }
    }

    public void ensureIndex() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
        if (exists) {
            return;
        }
        client.indices().create(c -> c.index(INDEX)
                .mappings(m -> m
                        .properties("faqId", p -> p.keyword(k -> k))
                        .properties("category", p -> p.keyword(k -> k))
                        .properties("question", p -> p.text(t -> t))
                        .properties("keywords", p -> p.keyword(k -> k))
                        .properties("answer", p -> p.text(t -> t))));
        log.info("[A-15] 创建常见问题索引 {}", INDEX);
    }

    public long rebuild() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
        if (exists) {
            client.indices().delete(d -> d.index(INDEX));
        }
        ensureIndex();

        ClassPathResource resource = new ClassPathResource("faq/faq-base.json");
        SourceFaq[] source = objectMapper.readValue(resource.getInputStream(), SourceFaq[].class);

        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (SourceFaq src : source) {
            FaqDocument doc = new FaqDocument(src.faqId, src.faqId, src.category,
                    src.question, src.keywords, src.answer);
            builder.operations(op -> op.index(idx -> idx.index(INDEX).id(doc.id()).document(doc)));
        }
        BulkResponse response = client.bulk(builder.build());
        if (response.errors()) {
            response.items().stream()
                    .filter(item -> item.error() != null)
                    .forEach(item -> log.error("[A-15] 话术写入失败 id={} 原因={}",
                            item.id(), item.error().reason()));
        }
        client.indices().refresh(r -> r.index(INDEX));
        return source.length;
    }

    public long count() throws IOException {
        CountResponse response = client.count(c -> c.index(INDEX));
        return response.count();
    }

    public List<FaqDocument> listAll() throws IOException {
        SearchResponse<FaqDocument> response = client.search(s -> s
                .index(INDEX)
                .size(500)
                .sort(so -> so.field(f -> f.field("faqId"))),
                FaqDocument.class);
        return response.hits().hits().stream().map(Hit::source).toList();
    }

    /**
     * 检索与问题最匹配的固定话术。
     */
    public List<FaqDocument> search(String question, int topK) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        try {
            SearchResponse<FaqDocument> response = client.search(s -> s
                    .index(INDEX)
                    .size(topK)
                    .query(q -> q.multiMatch(m -> m
                            .query(question)
                            .fields("question^2", "keywords", "answer"))),
                    FaqDocument.class);
            List<FaqDocument> docs = response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(d -> d != null)
                    .toList();
            log.info("[A-15] FAQ 检索“{}”命中 {} 条", question, docs.size());
            return docs;
        } catch (IOException e) {
            log.error("[A-15] FAQ 检索失败", e);
            return List.of();
        }
    }

    /** classpath 反序列化结构 */
    private static class SourceFaq {
        public String faqId;
        public String category;
        public String question;
        public List<String> keywords;
        public String answer;
    }
}
