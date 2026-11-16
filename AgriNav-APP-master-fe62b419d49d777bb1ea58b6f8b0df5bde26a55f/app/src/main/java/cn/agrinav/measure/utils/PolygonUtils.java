package cn.agrinav.measure.utils;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;

import java.util.List;

/**
 * @author 18506
 */
public class PolygonUtils {
    // 计算多边形的中心点
    public static LatLng getPolygonCenterPoint(List<LatLng> polygonPoints) {
        double latitude = 0.0;
        double longitude = 0.0;
        for (LatLng point : polygonPoints) {
            latitude += point.latitude;
            longitude += point.longitude;
        }
        int totalPoints = polygonPoints.size();
        return new LatLng(latitude / totalPoints, longitude / totalPoints);
    }
    //计算多边形的面积
    public static float calculateArea(List<LatLng> points) {
        if (points != null && points.size() >= 3) {
            double doubleArea = 0.0;
            for(int i = 0; i < points.size(); ++i) {
                LatLng point = points.get(i);
                LatLng latLng = points.get((i + 1) % points.size());
                double length1 = point.longitude * 111319.49079327357 * Math.cos(point.latitude * 0.017453292519943295);
                double width2 = point.latitude * 111319.49079327357;
                double length2 = latLng.longitude * 111319.49079327357 * Math.cos(latLng.latitude * 0.017453292519943295);
                double width1 = latLng.latitude * 111319.49079327357;
                doubleArea += length1 * width1 - length2 * width2;
            }
            return Math.abs((float)(doubleArea / 1333.34));
        } else {
            return 0.0F;
        }
    }
    //根据点的坐标计算amap的视野大小
    public static void adjustCameraToPolygon(AMap aMap, List<LatLng> polygonPoints) {
        if (aMap == null || polygonPoints == null || polygonPoints.isEmpty()) {
            return;
        }
        // 1. 计算边界框
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : polygonPoints) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        // 2. 计算合适的缩放级别
        int padding = 100; // 边缘填充，单位为像素
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        // 3. 移动相机
        aMap.moveCamera(cameraUpdate);
        // 可选：添加动画效果
        // aMap.animateCamera(cameraUpdate, 1000, null);
    }
}
