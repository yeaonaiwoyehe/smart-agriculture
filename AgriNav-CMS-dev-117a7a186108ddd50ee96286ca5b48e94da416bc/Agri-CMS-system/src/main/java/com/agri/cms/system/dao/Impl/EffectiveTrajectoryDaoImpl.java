package com.agri.cms.system.dao.Impl;

import com.agri.cms.system.dao.EffectiveTrajectoryDao;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.influxdb.client.DeleteApi;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.DeletePredicateRequest;
import com.influxdb.query.FluxTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: 姚锦伦
 * @CreateTime: 2024-09-25
 * @Description:
 * @Version: 1.0
 */
@Repository
public class EffectiveTrajectoryDaoImpl implements EffectiveTrajectoryDao {
    @Autowired
    private QueryApi queryApi;
    @Autowired
    private DeleteApi deleteApi;
    @Autowired
    private LandPlotMapper landPlotMapper;
    @Value("${spring.influxdb.measurement}")
    private String measurement;
    @Value("${spring.influxdb.bucket}")
    private String bucket;
    @Value("${spring.influxdb.startTime}")
    private String startTime;
    @Value("${spring.influxdb.tag.token}")
    private String trajectoryId;
    @Value("${spring.influxdb.tag.phoneNumber}")
    private String tagPhoneNumber;
    @Value("${spring.influxdb.org}")
    private String org;
    @Value("${spring.influxdb.tag.plotId}")
    private String plotId;

    @Override
    public List<FluxTable> queryByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        //查询flux语句
        // 将 LocalDateTime 转换为 OffsetDateTime，并格式化为 RFC3339 格式
        OffsetDateTime startOffset = startTime.atOffset(ZoneOffset.ofHours(8));
        OffsetDateTime endOffset = endTime.atOffset(ZoneOffset.ofHours(8));
        String formattedStart = startOffset.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String formattedEnd = endOffset.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 构建 Flux 查询
        String flux = String.format("""
                        from(bucket: "%s")
                          |> range(start: %s, stop: %s)
                          |> filter(fn: (r) => r["_measurement"] == "%s")
                          |> filter(fn: (r) => r["%s"] == "%s")
                          |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
                        """,
                bucket,
                formattedStart,
                formattedEnd,
                measurement,
                tagPhoneNumber,
                phoneNumber);
        try {
            return queryApi.query(flux);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteByPhoneNumber(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        //开始时间和结束时间转换为OffsetDateTime类型
        OffsetDateTime start = startTime.atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime stop = endTime.atOffset(OffsetDateTime.now().getOffset());
        //删除条件
        String predicate = String.format("%s=\"%s\"", tagPhoneNumber, phoneNumber);
        DeletePredicateRequest request = new DeletePredicateRequest()
                .predicate(predicate)
                .start(start)
                .stop(stop);
        deleteApi.delete(request, bucket, org);
    }

    @Override
    public List<Long> queryAllWorkingLandPlot() {
        String flux = String.format("""
                from(bucket: "%s")
                  |> range(start: %s)
                  |> filter(fn: (r) => r["_measurement"] == "%s")
                  |> group(columns: [])
                  |> distinct(column: "%s")
                """,bucket,startTime,measurement,plotId);
        List<FluxTable> fluxTables = queryApi.query(flux);
        
        if (io.jsonwebtoken.lang.Collections.isEmpty(fluxTables)) {
            return new ArrayList<>();
        }
        
        return fluxTables.get(0).getRecords().stream()
                .map(record -> Long.parseLong(record.getValueByKey("_value").toString()))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List<Point>> queryWorkingTrajectoryByPhoneNumber(String landPlotId) {
        // 构建 Flux 查询
        String flux = String.format("""
            from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r["_measurement"] == "%s")
              |> filter(fn: (r) => r["%s"] == "%s")
              |> pivot(rowKey: ["_time"], columnKey: ["_field"], valueColumn: "_value")
              |> group(columns: ["phoneNumber"])
            """,
                bucket,
                startTime,
                measurement,
                plotId,
                landPlotId);

        try {
            List<FluxTable> fluxTables = queryApi.query(flux);
            Map<String, List<Point>> trajectoryMap = new HashMap<>();

            fluxTables.forEach(table -> {
                table.getRecords().forEach(record -> {
                    String plotId = Objects.requireNonNull(record.getValueByKey("plotId")).toString();

                    // 创建新的轨迹点
                    Point point = new Point();
                    point.setLatitude(Double.parseDouble(Objects.requireNonNull(record.getValueByKey("latitude")).toString()));
                    point.setLongitude(Double.parseDouble(Objects.requireNonNull(record.getValueByKey("longitude")).toString()));

                    // 解析设备时间
                    String deviceTimeStr = Objects.requireNonNull(record.getValueByKey("deviceTime")).toString();
                    LocalDateTime deviceTime = LocalDateTime.parse(deviceTimeStr);
                    point.setTime(deviceTime);

                    // 将点添加到对应地块ID的列表中
                    trajectoryMap.computeIfAbsent(plotId, k -> new ArrayList<>()).add(point);
                });
            });

            // 对每个地块的点集合按时间排序
            trajectoryMap.values().forEach(points ->
                    points.sort(Comparator.comparing(Point::getTime))
            );

            return trajectoryMap;
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
