package org.yzh.web.trajectory.dao;

import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.yzh.protocol.commons.transform.AttributeKey;
import org.yzh.protocol.t808.T0200;
import org.yzh.web.config.Constants;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 *  @since 2024/9/23
 */
@Repository
public class EffectiveTrajectoryDaoImpl implements IEffectiveTrajectoryDao{
    @Autowired
    private WriteApi writeApi;
    @Value("${spring.influxdb.measurement}")
    private String measurement;
    @Value("${spring.influxdb.field.longitude}")
    private String fieldLongitude;
    @Value("${spring.influxdb.field.latitude}")
    private String fieldLatitude;
    @Value("${spring.influxdb.field.deviceTime}")
    private String time;
    @Value("${spring.influxdb.tag.token}")
    private String tagPhoneNumber;
    @Value("${spring.influxdb.tag.plotId}")
    private String tagPlotId;
    @Value("${spring.influxdb.tag.workType}")
    private String tagWorkType;
    @Value("${spring.influxdb.tag.width}")
    private String tagWidth;

    /**
     * 插入轨迹点数据
     * @param t0200
     * @param plotIds
     * @return
     */
    @Override
    public boolean insert(T0200 t0200,List<Long> plotIds) {
        t0200.transform();
        String clientId = t0200.getClientId();
        Map<Integer, Object> attributes = t0200.getAttributes();
        final String workType;
        final String widthStr;
        if(attributes != null){
            workType = attributes.get(AttributeKey.WorkType) != null ? attributes.get(AttributeKey.WorkType).toString() : "-1";
            widthStr = attributes.get(AttributeKey.Width) != null ?  attributes.get(AttributeKey.Width).toString() : "-1";
        }else {
            workType = "-1";
            widthStr = "-1";
        }
        plotIds.forEach(plotId -> {
            Point point = Point.measurement(measurement)
                    .addTag(tagPlotId, String.valueOf(plotId))
                    .addTag(tagPhoneNumber, clientId)
                    .addTag(tagWorkType, workType)
                    .addTag(tagWidth, widthStr)
                    .addField(fieldLongitude, String.valueOf(t0200.getLng()))
                    .addField(fieldLatitude, String.valueOf(t0200.getLat()))
                    .addField(time, String.valueOf(t0200.getDeviceTime()))
                    .time(Instant.now(), WritePrecision.NS);
            writeApi.writePoint(point);
        });
        return true;
    }

    /**
     * 批量插入轨迹点
     * @param t0200ToPlotIdsMap
     * @return
     */
    @Override
    public boolean insertBatch(Map<T0200, List<String>> t0200ToPlotIdsMap) {
        try {
            t0200ToPlotIdsMap.entrySet().stream().forEach(entry -> {
                T0200 t0200 = entry.getKey();
                List<String> plotIds = entry.getValue();

                t0200.transform();
                String clientId = t0200.getClientId();
                final String workType;
                final String widthStr;
                Map<Integer, Object> attributes = t0200.getAttributes();
                if(attributes != null){
                    workType = attributes.get(AttributeKey.WorkType) != null ?
                            String.valueOf(((Number) attributes.get(AttributeKey.WorkType)).intValue()) : "-1";
                    widthStr = attributes.get(AttributeKey.Width) != null ?
                            String.valueOf(((Number) attributes.get(AttributeKey.Width)).intValue()) : "-1";
                }else {
                    workType = "-1";
                    widthStr = "-1";
                }
                List<Point> points = plotIds.stream()
                        .map(plotId -> Point.measurement(measurement)
                                .addTag(tagPlotId, plotId)
                                .addTag(tagPhoneNumber, clientId)
                                .addTag(tagWorkType, workType)
                                .addTag(tagWidth, String.valueOf(widthStr))
                                .addField(fieldLongitude, String.valueOf(t0200.getLng()))
                                .addField(fieldLatitude, String.valueOf(t0200.getLat()))
                                .addField(time, String.valueOf(t0200.getDeviceTime()))
                                .time(Instant.now(), WritePrecision.NS))
                        .collect(Collectors.toList());

                // 批量插入
                writeApi.writePoints(points);
            });

            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}