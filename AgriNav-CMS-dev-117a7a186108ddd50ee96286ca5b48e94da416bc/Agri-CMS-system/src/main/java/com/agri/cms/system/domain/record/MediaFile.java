package com.agri.cms.system.domain.record;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-17 21:23
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-17 21:23
 */

@Document(indexName = "media_files")
@Setting(
    shards = 3,  // 分片数
    replicas = 1, // 副本数
    refreshInterval = "1s" // 刷新间隔
)
@Mapping(mappingPath = "/mapping/mediaFile.json")
public record MediaFile(
    @Id
    String id,

    @Field(type = FieldType.Keyword)
    String clientId,

    @Field(type = FieldType.Keyword)
    String fileUrl,

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "Asia/Shanghai")
    LocalDateTime createTime,

    @Field(type = FieldType.Double)
    Double longitude,

    @Field(type = FieldType.Double)
    Double latitude,

    @Field(type = FieldType.Keyword)
    String location
) {
    public MediaFile {
        if (createTime != null) {
            createTime = createTime.truncatedTo(ChronoUnit.MILLIS);
        }
        if (longitude != null && latitude != null) {
            location = String.format("%f,%f", longitude, latitude);
        }
    }

    public static MediaFile create(
            String clientId,
            String fileUrl,
            LocalDateTime createTime,
            Double longitude,
            Double latitude) {
        String id = String.format("%s_%d", clientId, createTime.toEpochSecond(java.time.ZoneOffset.UTC));
        return new MediaFile(id, clientId, fileUrl, createTime, longitude, latitude, null);
    }
}