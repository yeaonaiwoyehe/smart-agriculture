package org.yzh.web.trajectory.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.yzh.protocol.t808.T0200;
import org.yzh.web.config.Constants;
import org.yzh.web.trajectory.dao.IOriginalTrajectoryDao;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 原始轨迹服务实现类
 *
 * @author mfr
 * @since 2024/9/24
 */
@Service
public class OriginalTrajectoryServiceImpl implements IOriginalTrajectoryService {

    @Resource
    private IOriginalTrajectoryDao originalTrajectoryDao;

    /**
     * 保存原始轨迹点列表
     * @param t0200List T0200对象列表
     * @return boolean 是否全部保存成功
     * @author mfr
     * @since 2024/9/24
     */
    @Override
    public boolean saveOriginalPoints(List<T0200> t0200List) {
//        for (T0200 t0200 : t0200List) {
//            boolean isTransformed = t0200.transform();
//
//            // 如果转换失败，继续下一个点
//            if (!isTransformed) {
//                continue;
//            }
//
//            // 获取经度和纬度
//            double longitude = t0200.getLng();
//            double latitude = t0200.getLat();
//            String workType = t0200.getAttributes()==null ? "" : Objects
//                    .toString(t0200.getAttributes().get(Constants.WORK_TYPE_KEY), "");
//            String width = t0200.getAttributes()==null ? "" : Objects
//                    .toString(t0200.getAttributes().get(Constants.WIDTH_KEY), "");
//
//
//            // 从属性中提取地块工作类型，幅宽
//            Map<Integer, Object> attributes = t0200.getAttributes();
//
//
//
//            String phoneNumber = t0200.getClientId();
//            // 保存点
//            boolean saved = originalTrajectoryDao.saveOriginalPoint(longitude, latitude, t0200.getDeviceTime(), workType, width, phoneNumber);
//
//            // 如果保存失败，返回false
//            if (!saved) {
//                return false;
//            }
//        }
//
//        // 所有点都保存成功
//        return true;
        t0200List.forEach(T0200::transform);
        t0200List.stream()
                .map(t0200 -> {
                    Map<String, Object> map = new HashMap<>(8);
                    map.put("longitude", t0200.getLongitude());
                    map.put("latitude", t0200.getLatitude());
                    // 读取设备时间
                    LocalDateTime deviceTime = t0200.getDeviceTime();

                    // 创建东八区时区对象
                    ZoneId zoneId = ZoneId.of("Asia/Shanghai");

                    // 转换为 ZonedDateTime 对象
                    ZonedDateTime zonedDateTime = deviceTime.atZone(zoneId);

                    // 格式化为 ISO 8601 格式
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
                    String formattedTime = zonedDateTime.format(formatter);

                    // 将转换后的时间放入 map 中
                    map.put("deviceTime", formattedTime);
                    if(!t0200.getAttributes().isEmpty()){
                        t0200.getAttributes().forEach((key, value) ->
                                map.put(Constants.AddtionalInfo.get(key), value));
                    }
                    map.put("phoneNumber", t0200.getClientId());
                    return map;
                }).toList()
                .forEach(originalTrajectoryDao::saveOriginalPoint);

        return true;
    }
}