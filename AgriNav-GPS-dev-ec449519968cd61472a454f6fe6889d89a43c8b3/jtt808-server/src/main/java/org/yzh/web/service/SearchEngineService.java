package org.yzh.web.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yzh.web.model.entity.MediaFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-16 21:52
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-16 21:52
 */
@Service
public class SearchEngineService {
    private static final Logger log = LoggerFactory.getLogger(SearchEngineService.class);
    private static final String INDEX_NAME = "media_files";

    @Resource
    private ElasticsearchClient searchClient;

    /**
     * 保存或更新文档
     *
     * @param mediaFile 媒体文件信息
     * @return 是否成功
     */
    public void save(MediaFile mediaFile) throws IOException {
            searchClient.index(i -> i
                    .index(INDEX_NAME)
                    .id(mediaFile.id())
                    .document(mediaFile));
    }

    /**
     * 批量保存文档
     *
     * @param mediaFiles 媒体文件列表
     * @return 成功保存的数量
     */
    public int saveBatch(List<MediaFile> mediaFiles) {
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            
            for (MediaFile mediaFile : mediaFiles) {
                br.operations(op -> op
                        .index(idx -> idx
                                .index(INDEX_NAME)
                                .id(mediaFile.id())
                                .document(mediaFile)
                        )
                );
            }
            
            BulkResponse result = searchClient.bulk(br.build());
            
            if (result.errors()) {
                log.error("Bulk had errors");
                return 0;
            }
            
            return mediaFiles.size();
        } catch (IOException e) {
            log.error("Failed to save batch documents", e);
            return 0;
        }
    }

    /**
     * 根据ID查询文档
     *
     * @param id 文档ID
     * @return 媒体文件信息
     */
    public Optional<MediaFile> findById(String id) {
        try {
            GetResponse<MediaFile> response = searchClient.get(g -> g
                    .index(INDEX_NAME)
                    .id(id), 
                    MediaFile.class);
            
            return Optional.ofNullable(response.source());
        } catch (IOException e) {
            log.error("Failed to get document by id: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 根据设备ID查询文档
     *
     * @param clientId 设备ID
     * @return 媒体文件列表
     */
    public List<MediaFile> findByClientId(String clientId) {
        try {
            SearchResponse<MediaFile> response = searchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .term(t -> t
                                    .field("clientId")
                                    .value(clientId))),
                    MediaFile.class);
            
            return extractHits(response);
        } catch (IOException e) {
            log.error("Failed to search documents by clientId: {}", clientId, e);
            return List.of();
        }
    }

    /**
     * 根据时间范围查询文档
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 媒体文件列表
     */
    public List<MediaFile> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            SearchResponse<MediaFile> response = searchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .range(r -> r
                                    .field("createTime")
                                    .gte(JsonData.fromJson(startTime.toString()))
                                    .lte(JsonData.fromJson(endTime.toString())))),
                    MediaFile.class);
            
            return extractHits(response);
        } catch (IOException e) {
            log.error("Failed to search documents by time range", e);
            return List.of();
        }
    }

    /**
     * 根据设备ID和时间范围组合查询
     *
     * @param clientId  设备ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 媒体文件列表
     */
    public List<MediaFile> findByClientIdAndTimeRange(String clientId, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            SearchResponse<MediaFile> response = searchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .term(t -> t
                                                    .field("clientId")
                                                    .value(clientId)))
                                    .must(m -> m
                                            .range(r -> r
                                                    .field("createTime")
                                                    .gte(JsonData.fromJson(startTime.toString()))
                                                    .lte(JsonData.fromJson(endTime.toString())))))),
                    MediaFile.class);
            
            return extractHits(response);
        } catch (IOException e) {
            log.error("Failed to search documents by clientId and time range", e);
            return List.of();
        }
    }

    /**
     * 删除文档
     *
     * @param id 文档ID
     * @return 是否成功
     */
    public boolean deleteById(String id) {
        try {
            DeleteResponse response = searchClient.delete(d -> d
                    .index(INDEX_NAME)
                    .id(id));
            
            log.info("Document deleted: {}", response.id());
            return true;
        } catch (IOException e) {
            log.error("Failed to delete document: {}", id, e);
            return false;
        }
    }

    /**
     * 提取搜索结果
     */
    private List<MediaFile> extractHits(SearchResponse<MediaFile> response) {
        List<MediaFile> results = new ArrayList<>();
        for (Hit<MediaFile> hit : response.hits().hits()) {
            if (hit.source() != null) {
                results.add(hit.source());
            }
        }
        return results;
    }
}
