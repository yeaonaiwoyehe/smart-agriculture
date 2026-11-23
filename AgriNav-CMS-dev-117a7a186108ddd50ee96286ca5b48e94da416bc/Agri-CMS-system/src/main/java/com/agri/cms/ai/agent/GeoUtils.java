package com.agri.cms.ai.agent;

/**
 * 地理计算工具（A-11）。
 *
 * 基于 WGS84 经纬度，用 Haversine 球面距离公式计算两点间的地表距离。
 * 用于任务卡片展示农机手到地块中心的大致距离（米），精度满足"就近找活"场景；
 * 真正的逐向导航由 APP 调起高德地图完成。
 *
 * @author agrinav
 */
public final class GeoUtils {

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GeoUtils() {
    }

    /**
     * 计算两个经纬度之间的球面距离。
     *
     * @param lng1 起点经度
     * @param lat1 起点纬度
     * @param lng2 终点经度
     * @param lat2 终点纬度
     * @return 距离（米）
     */
    public static double haversineMeters(double lng1, double lat1, double lng2, double lat2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
