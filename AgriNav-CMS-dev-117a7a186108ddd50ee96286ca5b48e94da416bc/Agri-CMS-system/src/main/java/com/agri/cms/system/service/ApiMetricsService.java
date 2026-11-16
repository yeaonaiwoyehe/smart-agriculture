package com.agri.cms.system.service;

import com.agri.cms.system.domain.ApiStats;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.Duration;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.agri.cms.common.constant.InFluxConstants.BUCKET_API_METRICS;
import static com.agri.cms.common.constant.InFluxConstants.MEASUREMENT_API_STATS;

/**
 * @author : simon
 * @description :
 * @last : 2024-12-17 14:34
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-12-17 14:34
 */
@Service
public class ApiMetricsService {



    @Value("${spring.influxdb.org}")
    private String organization;
    @Resource
    private WriteApi writeApi;

    @Resource
    private QueryApi queryApi;


    /**
     * 记录接口访问记录
     * @param path
     * @param method
     * @param executionTime
     * @author simon
     * @since 2024/12/17
     */
    public void recordApiAccess(String path, String method, long executionTime) {
        Point point = Point.measurement(MEASUREMENT_API_STATS)
                .addTag("path", path)
                .addTag("method", method)
                .addField("count", 1)
                .addField("execution_time", executionTime)
                .time(Instant.now(), WritePrecision.MS);

        writeApi.writePoint(BUCKET_API_METRICS,organization,point);
    }

    /**
     * 获取按小时统计的接口调用情况
     * @param duration 查询时间范围
     * @return List<ApiStats>
     * @author simon
     * @since 2024/12/17
     */
    public List<ApiStats> getHourlyStats(Duration duration) {
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: -%s)
              |> filter(fn: (r) => r["_measurement"] == "%s")
              |> window(every: 1h)
              |> group(columns: ["path", "_time"])
              |> reduce(
                   fn: (r, accumulator) => ({
                     count: r.count + accumulator.count,
                     total_time: r.execution_time + accumulator.total_time
                   })
                 )
              |> map(fn: (r) => ({
                   path: r.path,
                   count: r.count,
                   avg_time: r.total_time / r.count
                 }))
              |> sort(columns: ["_time"])
            """,
                BUCKET_API_METRICS,
                duration.toString(),
                MEASUREMENT_API_STATS);

        List<ApiStats> results = new ArrayList<>();
        List<FluxTable> tables = queryApi.query(flux);

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                ApiStats stats = new ApiStats();
                stats.setPath(record.getValueByKey("path").toString());
                stats.setCount(((Number) record.getValueByKey("count")).longValue());
                stats.setAvgTime(((Number) record.getValueByKey("avg_time")).doubleValue());
                stats.setTimestamp(LocalDateTime.parse(record.getTime().toString()));
                results.add(stats);
            }
        }

        return results;
    }

    /**
     * 获取接口总体统计
     * @return List<ApiStats>
     * @author simon
     * @since 2024/12/17
     */
    public List<ApiStats> getOverallStats() {
        String flux = String.format("""
        from(bucket: "%s")
          |> range(start: -30d)
          |> filter(fn: (r) => r["_measurement"] == "%s")
          |> group(columns: ["path", "method"])
          |> reduce(
               identity: {count: 0, total_time: 0.0},
               fn: (r, accumulator) => ({
                 count: r.count + accumulator.count,
                 total_time: r.execution_time + accumulator.total_time
               })
             )
          |> map(fn: (r) => ({
               path: r.path,
               method: r.method,
               count: r.count,
               avg_time: r.total_time / r.count
             }))
          |> sort(columns: ["count"], desc: true)
        """,
                BUCKET_API_METRICS,
                MEASUREMENT_API_STATS);

        List<ApiStats> results = new ArrayList<>();
        List<FluxTable> tables = queryApi.query(flux);

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                ApiStats stats = new ApiStats();
                stats.setPath(Objects.requireNonNull(record.getValueByKey("path")).toString());
                stats.setMethod(Objects.requireNonNull(record.getValueByKey("method")).toString());
                stats.setCount(((Number) Objects.requireNonNull(record.getValueByKey("count"))).longValue());
                stats.setAvgTime(((Number) Objects.requireNonNull(record.getValueByKey("avg_time"))).doubleValue());
                stats.setTimestamp(LocalDateTime.now());
                results.add(stats);
            }
        }

        return results;
    }

}
