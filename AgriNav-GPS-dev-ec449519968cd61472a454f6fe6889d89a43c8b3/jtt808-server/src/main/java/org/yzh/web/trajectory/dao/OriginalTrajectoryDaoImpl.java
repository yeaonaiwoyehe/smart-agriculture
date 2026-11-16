package org.yzh.web.trajectory.dao;

import jakarta.annotation.Resource;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author mfr
 * @since 2024/9/24 */
@Repository
public class OriginalTrajectoryDaoImpl implements IOriginalTrajectoryDao{
    // Elasticsearch的客户端，用于与Elasticsearch服务器进行交互
    @Resource
    private RestHighLevelClient client;

    /**
     * 保存原始轨迹点信息
     * @return boolean
     * @author mfr
     * @since 2024/9/26     */
    @Override
    public boolean saveOriginalPoint(Map<String, Object> jsonMap) {
        // 创建存储点信息的Map
//        Map<String, Object> jsonMap = new HashMap<>();
//        jsonMap.put("longitude", longitude);
//        jsonMap.put("latitude", latitude);
//        jsonMap.put("deviceTime", deviceTime);
//        jsonMap.put("workType", workType);
//        jsonMap.put("width", width);
//        jsonMap.put("phoneNumber", phoneNumber);

        // 创建索引请求并指定要插入的索引和源数据,若没有则创建一个
        IndexRequest indexRequest = new IndexRequest("original_point")
                .source(jsonMap);

        // 执行请求插入文档
        boolean result = false;
        try {
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.add(indexRequest);
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                // 打印插入失败信息
                System.out.println("Bulk insert had failures: " + bulkResponse.buildFailureMessage());
            } else {
                result = true;
                // 打印成功信息
                System.out.println("Bulk insert successful, inserted " + bulkResponse.getItems().length + " documents");
            }
        } catch (IOException e) {
            System.err.println("Failed to insert document: " + e.getMessage());
        }

        return result;
    }
}
