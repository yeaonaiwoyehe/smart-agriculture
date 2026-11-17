package org.yzh.web.service;

import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yzh.protocol.t808.T0801;
import org.yzh.web.model.entity.MediaFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;

/**
 * 多媒体文件存储服务（方案 B：去 MinIO，改为本地磁盘存储）
 * 文件写入本地媒体目录，通过 /media/** 静态资源访问，元数据写 ES media_files。
 */
@Service
public class RemoteFileService {

    private static final Logger log = LoggerFactory.getLogger(RemoteFileService.class);

    @Value("${jt-server.jt808.media-file.path}")
    private String mediaFileRoot;

    @Value("${jt-server.jt808.media-file.url-prefix:http://127.0.0.1:8100/media}")
    private String mediaUrlPrefix;

    /**
     * 保存多媒体文件到本地磁盘并返回媒体元数据
     */
    public MediaFile uploadMediaFile(T0801 message) throws Exception {
        String clientId = message.getClientId();

        // 构建日期路径（年/月/日）
        LocalDateTime now = LocalDateTime.now();
        String datePath = String.format("%d/%02d/%02d",
                now.getYear(), now.getMonthValue(), now.getDayOfMonth());

        // 构建文件名（时_分_秒.jpg，本地文件名不能含冒号）
        String filename = String.format("%02d_%02d_%02d.jpg",
                now.getHour(), now.getMinute(), now.getSecond());

        String relativePath = clientId + "/" + datePath + "/" + filename;

        ByteBuf packet = message.getPacket();
        try {
            byte[] bytes = new byte[packet.readableBytes()];
            packet.readBytes(bytes);

            File dest = new File(mediaFileRoot + File.separator + relativePath);
            if (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) {
                throw new IllegalStateException("创建目录失败: " + dest.getParent());
            }
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(bytes);
            }

            message.getLocation().transform();
            String fileUrl = mediaUrlPrefix + "/" + relativePath;
            return MediaFile.create(
                    clientId,
                    fileUrl,
                    now,
                    message.getLocation().getLng(),
                    message.getLocation().getLat()
            );
        } finally {
            if (packet != null) {
                packet.release();
            }
        }
    }
}
