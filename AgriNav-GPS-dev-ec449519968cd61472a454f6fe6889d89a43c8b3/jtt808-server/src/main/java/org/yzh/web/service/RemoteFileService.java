package org.yzh.web.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.netty.buffer.ByteBuf;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yzh.protocol.t808.T0801;
import org.yzh.web.config.MinioConfig;
import org.yzh.web.model.entity.MediaFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


/**
 * @author : simon
 * @description :
 * @last : 2024-11-15 21:35
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-15 21:35
 */


@Service
public class RemoteFileService {

    private static final Logger log = LoggerFactory.getLogger(RemoteFileService.class);

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinioConfig minioConfig;

    /**
     * 上传多媒体文件到MinIO
     */
    public MediaFile uploadMediaFile(T0801 message) throws Exception {
        try {
            String clientId = message.getClientId();
            
            // 构建日期路径（年/月/日）
            LocalDateTime now = LocalDateTime.now();
            String datePath = String.format("%d/%02d/%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth());
            
            // 构建文件名（时:分:秒.jpg）
            String filename = String.format("%02d:%02d:%02d.jpg",
                now.getHour(), now.getMinute(), now.getSecond());

            // 构建对象名称(文件路径)
            String objectName = String.format("%s/%s/%s", clientId, datePath, filename);

            // 从ByteBuf获取字节数组
            ByteBuf packet = message.getPacket();
            byte[] bytes = new byte[packet.readableBytes()];
            packet.readBytes(bytes);

            // 创建输入流
            InputStream inputStream = new ByteArrayInputStream(bytes);

            // 上传到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType("image/jpeg")
                            .build()
            );
            message.getLocation().transform();
            return MediaFile.create(
                    clientId,
                    String.format("%s/%s/%s", minioConfig.getUrl(), minioConfig.getBucketName(), objectName),
                    now,
                    message.getLocation().getLat(),
                    message.getLocation().getLng()
            );
        } finally {
            if (message.getPacket() != null) {
                message.getPacket().release();
            }
        }
    }

    private String getMediaType(int type) {
        switch (type) {
            case 0: return "image";
            case 1: return "audio";
            case 2: return "video";
            default: return "unknown";
        }
    }

    private String getMediaFormat(int format) {
        switch (format) {
            case 0: return "jpg";
            case 1: return "tif";
            case 2: return "mp3";
            case 3: return "wav";
            case 4: return "wmv";
            default: return "bin";
        }
    }

    private String getContentType(int format) {
        switch (format) {
            case 0: return "image/jpeg";
            case 1: return "image/tiff";
            case 2: return "audio/mpeg";
            case 3: return "audio/wav";
            case 4: return "video/x-ms-wmv";
            default: return "application/octet-stream";
        }
    }

}