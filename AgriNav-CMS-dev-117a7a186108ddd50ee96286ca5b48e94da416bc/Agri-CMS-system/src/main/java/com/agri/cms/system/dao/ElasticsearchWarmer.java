package com.agri.cms.system.dao;

import com.agri.cms.system.dao.Impl.OriginalTrajectoryDaoImpl;
import com.agri.cms.system.domain.vo.Point;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.util.List;

@Component
public class ElasticsearchWarmer {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private OriginalTrajectoryDaoImpl trajectoryDao;

    @Value("${spring.elasticsearch.index}")
    private String indexName;

    @Value("${spring.elasticsearch.pool.max-conn-per-route:10}")
    private int maxConnPerRoute;

    @PostConstruct
    public void warmUpElasticsearch() {
        try {
            System.out.println("Starting Elasticsearch warm-up process...");

            // 预热连接
            for (int i = 0; i < maxConnPerRoute; i++) {
                boolean pingSuccessful = client.ping(RequestOptions.DEFAULT);
            }

            // 检查索引是否存在
            boolean indexExists = client.indices().exists(new GetIndexRequest(indexName), RequestOptions.DEFAULT);

            if (indexExists) {
                // 刷新索引
                client.indices().refresh(new RefreshRequest(indexName), RequestOptions.DEFAULT);

                // 执行全条件查询以预热索引
                long startTime = System.currentTimeMillis();
                List<Point> points = trajectoryDao.queryAllPoints();
                long endTime = System.currentTimeMillis();

            }

        } catch (IOException e) {
            System.err.println("Error during Elasticsearch warm-up:");
            e.printStackTrace();
        } finally {
            System.out.println("Elasticsearch warm-up process completed.");
        }
    }
}