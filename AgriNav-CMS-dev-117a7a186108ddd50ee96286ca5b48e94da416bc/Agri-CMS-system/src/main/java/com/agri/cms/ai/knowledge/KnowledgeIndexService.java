package com.agri.cms.ai.knowledge;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.CountResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库索引服务（A-08 RAG）。
 *
 * 负责知识库索引的创建、制度文档的加载与切片、批量写入与重建。
 * 索引采用 Elasticsearch 默认 standard 分词，配合 BM25 相关性排序；
 * 应用启动时若索引为空会自动从 classpath 初始化，保证开箱即用。
 *
 * @author agrinav
 */
@Service
public class KnowledgeIndexService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexService.class);

    /** 知识库索引名 */
    public static final String INDEX = "agri_knowledge";

    /** 单个切片最大字符数 */
    private static final int MAX_CHUNK_CHARS = 400;

    private final ElasticsearchClient client;
    private final ObjectMapper objectMapper;

    public KnowledgeIndexService(ElasticsearchClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    /**
     * 启动后确保索引存在，若为空则自动初始化文档。
     */
    @PostConstruct
    public void initOnStartup() {
        try {
            ensureIndex();
            long count = count();
            if (count == 0) {
                long loaded = rebuild();
                log.info("[A-08] 知识库启动初始化完成，写入 {} 个切片", loaded);
            } else {
                log.info("[A-08] 知识库已存在 {} 个切片，跳过初始化", count);
            }
        } catch (Exception e) {
            // 知识库初始化失败不阻断主流程，检索时会走兜底
            log.error("[A-08] 知识库启动初始化失败，将使用兜底回答", e);
        }
    }

    /**
     * 确保索引与映射存在，不存在则创建。
     */
    public void ensureIndex() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
        if (exists) {
            return;
        }
        client.indices().create(c -> c.index(INDEX)
                .mappings(m -> m
                        .properties("docId", p -> p.keyword(k -> k))
                        .properties("title", p -> p.text(t -> t))
                        .properties("category", p -> p.keyword(k -> k))
                        .properties("source", p -> p.keyword(k -> k))
                        .properties("chunkNo", p -> p.integer(i -> i))
                        .properties("content", p -> p.text(t -> t))));
        log.info("[A-08] 创建知识库索引 {}", INDEX);
    }

    /**
     * 重建知识库：删除旧索引后重新加载并写入，返回写入的切片数。
     */
    public long rebuild() throws IOException {
        boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
        if (exists) {
            client.indices().delete(d -> d.index(INDEX));
        }
        ensureIndex();

        List<KnowledgeDocument> chunks = loadAndChunk();
        return bulkIndex(chunks);
    }

    /**
     * 批量写入切片，返回写入数量。
     */
    private long bulkIndex(List<KnowledgeDocument> chunks) throws IOException {
        BulkRequest.Builder builder = new BulkRequest.Builder();
        for (KnowledgeDocument doc : chunks) {
            builder.operations(op -> op.index(idx -> idx.index(INDEX).id(doc.id()).document(doc)));
        }
        BulkResponse response = client.bulk(builder.build());
        if (response.errors()) {
            response.items().stream()
                    .filter(item -> item.error() != null)
                    .forEach(item -> log.error("[A-08] 切片写入失败 id={} 原因={}",
                            item.id(), item.error().reason()));
        }
        client.indices().refresh(r -> r.index(INDEX));
        return chunks.size();
    }

    /**
     * 统计索引中的切片数量。
     */
    public long count() throws IOException {
        CountResponse response = client.count(c -> c.index(INDEX));
        return response.count();
    }

    /**
     * 列出全部切片（按 docId、chunkNo 排序），用于管理端查看。
     */
    public List<KnowledgeDocument> listAll() throws IOException {
        SearchResponse<KnowledgeDocument> response = client.search(s -> s
                .index(INDEX)
                .size(500)
                .sort(so -> so.field(f -> f.field("docId").unmappedType(FieldType.Keyword)))
                .sort(so -> so.field(f -> f.field("chunkNo"))),
                KnowledgeDocument.class);
        return response.hits().hits().stream().map(Hit::source).toList();
    }

    /**
     * 从 classpath 加载原始制度文档并切片。
     */
    private List<KnowledgeDocument> loadAndChunk() throws IOException {
        ClassPathResource resource = new ClassPathResource("knowledge/knowledge-base.json");
        SourceDoc[] sourceDocs = objectMapper.readValue(resource.getInputStream(), SourceDoc[].class);

        List<KnowledgeDocument> result = new ArrayList<>();
        for (SourceDoc src : sourceDocs) {
            List<String> pieces = splitContent(src.content == null ? "" : src.content.trim());
            for (int i = 0; i < pieces.size(); i++) {
                String id = src.docId + "-" + i;
                result.add(new KnowledgeDocument(id, src.docId, src.title, src.category,
                        src.source, i, pieces.get(i)));
            }
        }
        return result;
    }

    /**
     * 按中文句号、换行切分长文本，尽量使每片不超过 {@link #MAX_CHUNK_CHARS}。
     */
    private List<String> splitContent(String content) {
        List<String> pieces = new ArrayList<>();
        if (content.length() <= MAX_CHUNK_CHARS) {
            pieces.add(content);
            return pieces;
        }

        String[] sentences = content.split("(?<=[。！？；\\n])");
        StringBuilder buffer = new StringBuilder();
        for (String sentence : sentences) {
            if (buffer.length() + sentence.length() > MAX_CHUNK_CHARS && buffer.length() > 0) {
                pieces.add(buffer.toString().trim());
                buffer.setLength(0);
            }
            buffer.append(sentence);
        }
        if (buffer.length() > 0) {
            pieces.add(buffer.toString().trim());
        }
        return pieces;
    }

    /**
     * classpath 原始文档结构（仅用于反序列化）。
     */
    private static class SourceDoc {
        public String docId;
        public String title;
        public String category;
        public String source;
        public String content;
    }
}
