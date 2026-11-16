package org.yzh.web.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-16 21:16
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-16 21:16
 */
@Document(indexName = "media_files")
@Setting(
    shards = 3,  // 分片数
    replicas = 1, // 副本数
    refreshInterval = "1s" // 刷新间隔
)
public record MediaFile(
    @Id
    String id,
    
    @Field(type = FieldType.Keyword)
    String clientId,
    
    @Field(type = FieldType.Keyword)
    String fileUrl,
    
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
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
