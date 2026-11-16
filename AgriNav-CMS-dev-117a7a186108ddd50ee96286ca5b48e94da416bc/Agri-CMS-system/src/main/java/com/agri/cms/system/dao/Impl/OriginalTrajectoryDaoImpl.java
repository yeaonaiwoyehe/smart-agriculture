package com.agri.cms.system.dao.Impl;

import com.agri.cms.common.config.ElasticsearchConfig;
import com.agri.cms.common.constant.OriginalTrajectoryConstants;
import com.agri.cms.common.exception.OriginalTrajectoryException;
import com.agri.cms.system.dao.OriginalTrajectoryDao;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.SortMessage;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

/**
 * 原始轨迹数据访问实现类
 * 负责与EsService交互以获取轨迹点数据
 *
 * @author mfr
 * @since 2024/9/26
 */
@Repository
public class OriginalTrajectoryDaoImpl implements OriginalTrajectoryDao {
    private static final Logger log = LoggerFactory.getLogger(OriginalTrajectoryDaoImpl.class);

    @Resource
    private RestHighLevelClient client;
    @Resource  // 添加这个
    private ElasticsearchConfig esConfig;

    @Value("${spring.elasticsearch.index}")
    private String ES_INDEX;

    @Value("${spring.elasticsearch.request-size}")
    private int REQUEST_SIZE;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 查询原始轨迹点数据
     *
     * @return List<Point>
     * @author mfr
     * @since 2024/9/26
     */
    @Override
    public List<Point> queryOriginalPoint(SortMessage sortMessage) {
        Objects.requireNonNull(sortMessage, "SortMessage must not be null");

        try {
            return executorService.submit(() -> {
                SearchRequest searchRequest = createSearchRequest(sortMessage);
                return getResultPoints(searchRequest);
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new OriginalTrajectoryException(OriginalTrajectoryConstants.ORIGINAL_TRAJECTORY_QUERY_EXCEPTION, e);
        }
    }

    /**
     * 查询轨迹点数据根据手机号和时间范围
     *
     * @return List<Point>
     * @author mfr
     * @since 2024/9/27
     */
    @Override
    public List<Point> queryOriginalPointByPhoneAndTime(String phone, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return executorService.submit(() -> {
                SearchRequest searchRequest = new SearchRequest(ES_INDEX);
                SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
                BoolQueryBuilder boolQuery = createBoolQuery(startTime, endTime, phone, null);
                searchSourceBuilder.query(boolQuery);
                searchSourceBuilder.size(REQUEST_SIZE);
                searchRequest.source(searchSourceBuilder);
                return getResultPoints(searchRequest);
            }).get();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new OriginalTrajectoryException(OriginalTrajectoryConstants.ORIGINAL_TRAJECTORY_QUERY_EXCEPTION, e);
        }
    }

    /**
     * 执行全条件查询（用于预热）
     *
     * @return List<Point>
     */
    @Override
    public List<Point> queryAllPoints() {
        SearchRequest searchRequest = new SearchRequest(ES_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 创建一个布尔查询
        BoolQueryBuilder boolQuery = new BoolQueryBuilder();

        // 添加时间范围查询
        ZoneId beijingZone = ZoneId.of("Asia/Shanghai");
        ZonedDateTime now = ZonedDateTime.now(beijingZone);
        ZonedDateTime oneWeekAgo = now.minusWeeks(1);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        String startTimeStr = oneWeekAgo.format(formatter);
        String endTimeStr = now.format(formatter);

        boolQuery.must(new RangeQueryBuilder("deviceTime")
                .gte(startTimeStr)
                .lte(endTimeStr)
                .format("yyyy-MM-dd'T'HH:mm:ssXXX"));

        // 添加电话号码查询（使用一个示例号码）
        boolQuery.should(new MatchQueryBuilder("phoneNumber", "1234567890"));

        // 添加工作类型查询（使用一些示例工作类型）
        boolQuery.should(new MatchQueryBuilder("WorkType", "1"));
        boolQuery.should(new MatchQueryBuilder("WorkType", "2"));
        boolQuery.should(new MatchQueryBuilder("WorkType", "3"));


        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.size(REQUEST_SIZE);
        searchRequest.source(searchSourceBuilder);

        return getResultPoints(searchRequest);
    }

    /**
     * 删除轨迹点数据
     *
     * @return boolean
     * @author mfr
     * @since 2024/10/30
     */
    @Override
    public boolean deleteByUserName(String phone) {
        try {
            return esConfig.executeWithRetry(client -> {
                DeleteByQueryRequest request = new DeleteByQueryRequest(ES_INDEX);
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("phoneNumber", phone));
                request.setQuery(boolQuery);
                request.setRefresh(true);

                BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
                return response.getDeleted() > 0;
            });
        } catch (IOException e) {
            throw new OriginalTrajectoryException(OriginalTrajectoryConstants.ORIGINAL_TRAJECTORY_DELETE_FAIL_EXCEPTION, e);
        }
    }

    /**
     * 异步查询原始轨迹点数据
     *
     * @return CompletableFuture<List < Point>>
     * @author mfr
     * @since 2024/10/18
     */
    public CompletableFuture<List<Point>> queryOriginalPointAsync(SortMessage sortMessage) {
        Objects.requireNonNull(sortMessage, "SortMessage must not be null");

        return CompletableFuture.supplyAsync(() -> {
            SearchRequest searchRequest = createSearchRequest(sortMessage);
            return getResultPoints(searchRequest);
        }, executorService);
    }

    /**
     * 获取搜索结果
     *
     * @return List<Point>
     * @author mfr
     * @since 2024/10/18
     */
    @NotNull
    private List<Point> getResultPoints(SearchRequest searchRequest) {
        try {
            return esConfig.executeWithRetry(client -> {
                SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
                List<Point> points = getResult(searchResponse);
                points.sort(Comparator.comparing(Point::getTime));
                return points;
            });
        } catch (IOException e) {
            throw new OriginalTrajectoryException(OriginalTrajectoryConstants.ORIGINAL_TRAJECTORY_QUERY_EXCEPTION, e);
        }
    }

    /**
     * 创建搜索请求
     *
     * @return SearchRequest
     * @author mfr
     * @since 2024/10/18
     */
    @NotNull
    private SearchRequest createSearchRequest(SortMessage sortMessage) {
        SearchRequest searchRequest = new SearchRequest(ES_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 拼接时间查询条件
        // 设置开始时间
        LocalDate startDate = LocalDate.parse(sortMessage.getLocalDate());
        LocalDateTime startTime = LocalDateTime.of(startDate, sortMessage.getBeginTime());

        // 设置结束时间
        LocalDateTime endTime = LocalDateTime.of(startDate, sortMessage.getEndTime());
        // 拿取条件
        String phone = sortMessage.getPhone();
        String workType = sortMessage.getWorkType();

        // 创建Bool查询
        BoolQueryBuilder boolQuery = createBoolQuery(startTime, endTime, phone, workType);

        // 将 Bool 查询应用到搜索源构建器
        searchSourceBuilder.query(boolQuery);
        // 设置返回文档的数量
        searchSourceBuilder.size(REQUEST_SIZE);

        // 将搜索源构建器应用到搜索请求
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

    /**
     * 创建Bool查询
     *
     * @return BoolQueryBuilder
     * @author mfr
     * @since 2024/10/18
     */
    private static BoolQueryBuilder createBoolQuery(LocalDateTime startTime, LocalDateTime endTime, String phone, String workType) {
        // 定义东八区时区
        ZoneId beijingZone = ZoneId.of("Asia/Shanghai");

        // 使用输入的时间，转换为ZonedDateTime并包含时区信息
        ZonedDateTime startZdt = ZonedDateTime.of(startTime, beijingZone);
        ZonedDateTime endZdt = ZonedDateTime.of(endTime, beijingZone);

        // 格式化时间字符串，包含时区信息
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
        String startTimeStr = startZdt.format(formatter);
        String endTimeStr = endZdt.format(formatter);

        RangeQueryBuilder rangeQuery = new RangeQueryBuilder("deviceTime")
                .gte(startTimeStr)
                .lte(endTimeStr)
                .format("yyyy-MM-dd'T'HH:mm:ssXXX");

        BoolQueryBuilder boolQuery = new BoolQueryBuilder()
                .must(rangeQuery);

        if (phone != null && !phone.isEmpty()) {
            boolQuery.filter(new MatchQueryBuilder("phoneNumber", phone));
        }

        if (workType != null && !workType.isEmpty()) {
            boolQuery.filter(new MatchQueryBuilder("WorkType", workType));
        }
        return boolQuery;
    }

    /**
     * 从搜索响应中获取轨迹点数据
     *
     * @return List<Point>
     * @author mfr
     * @since 2024/9/26
     */
    private List<Point> getResult(SearchResponse searchResponse) {
        List<Point> points = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits()) {
            Map<String, Object> source = hit.getSourceAsMap();

            // 转换经纬度为正确的格式
            double latitude = convertCoordinate(source.get("latitude"));
            double longitude = convertCoordinate(source.get("longitude"));

            // 获取时间
            Object timestamp = source.get("deviceTime");
            LocalDateTime localDateTime = null;
            if (timestamp instanceof String) {
                String timestampStr = (String) timestamp;
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestampStr);
                localDateTime = zonedDateTime.toLocalDateTime();
            }

            // 创建轨迹点对象
            Point point = new Point(longitude, latitude, localDateTime);
            points.add(point);
        }
        return points;
    }

    /**
     * 将坐标转换为正常的经纬度格式
     *
     * @return double
     * @author mfr
     * @since 2024/10/18
     */
    private double convertCoordinate(Object value) {
        if (value instanceof Number) {
            double numValue = ((Number) value).doubleValue();
            // 将存储的大数值转换为正常的经纬度格式
            return numValue / 1000000.0;
        } else if (value instanceof String) {
            // 如果是字符串,先解析为数字再转换
            return Double.parseDouble((String) value) / 1000000.0;
        }
        throw new IllegalArgumentException("Invalid coordinate value: " + value);
    }

    /**
     * 根据原手机号更新为新手机号
     *
     * @param oldPhone 原手机号
     * @param newPhone 新手机号
     * @return boolean 更新是否成功
     * @author mfr
     * @since 2024/10/31
     */
    @Override
    public boolean updatePhoneNumber(String oldPhone, String newPhone) {
        try {
            return esConfig.executeWithRetry(client -> {
                // 创建更新请求
                UpdateByQueryRequest request = new UpdateByQueryRequest(ES_INDEX);
                
                // 设置查询条件
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                        .must(QueryBuilders.matchQuery("phoneNumber", oldPhone));
                request.setQuery(boolQuery);
                
                // 设置更新脚本
                String scriptContent = "ctx._source.phoneNumber = params.newPhone";
                Script script = new Script(ScriptType.INLINE, "painless", scriptContent, 
                        Collections.singletonMap("newPhone", newPhone));
                request.setScript(script);
                
                // 执行更新
                BulkByScrollResponse response = client.updateByQuery(request, RequestOptions.DEFAULT);
                return response.getUpdated() > 0;
            });
        } catch (IOException e) {
            throw new OriginalTrajectoryException(OriginalTrajectoryConstants.ORIGINAL_TRAJECTORY_UPDATE_FAIL_EXCEPTION+"更新手机号失败");
        }
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}