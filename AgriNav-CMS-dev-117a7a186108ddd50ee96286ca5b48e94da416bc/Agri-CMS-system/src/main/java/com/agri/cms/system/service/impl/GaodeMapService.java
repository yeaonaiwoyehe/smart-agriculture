package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.DistanceConstants;
import com.agri.cms.common.utils.file.MinioUtil;
import com.agri.cms.common.utils.trajectory.TrajectoryUtils;
import com.agri.cms.system.domain.vo.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

/**
 * @author 姚锦伦
 */
@Service
public class GaodeMapService {

    @Value("${minio.bucketName}")
    private String bucketName;
    @Value("${gaode.key}")
    private String apiKey;
    @Value("${gaode.url}")
    private String staticMapUrl;
    private static final String PICTURE_PREFIX = "trajectory_";
    private static final String PICTURE_SUFFIX = ".png";
    private static final String TRAJECTORY = "0x95dda2";
    private static final String BOUNDARY = "0xa9daf3";
    private static final String FILL = "0xe0f0dd";
    public String generateThumbnail(List<Point> points, List<Point> boundaryPoints,
            List<List<Point>> boundaryPointsExclude, List<List<Point>> boundaryExclude) throws Exception {

        return null;
    }

    public String generateThumbnail(List<Point> points, List<Point> boundaryPoints) throws Exception {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("点集为空");
        }
        //计算中心点
        double centerLon = 0, centerLat = 0;
        for (Point point : boundaryPoints) {
            centerLon += point.getLongitude();
            centerLat += point.getLatitude();
        }
        centerLon /= boundaryPoints.size();
        centerLat /= boundaryPoints.size();
        //计算最大距离
        double maxDistance = 0;
        for (Point point : boundaryPoints) {
            double distance = TrajectoryUtils.calculateDistance(centerLat, centerLon,
                    point.getLatitude(), point.getLongitude());
            maxDistance = Math.max(maxDistance, distance);
        }
        //计算缩放级别
        int zoom = calculateZoom(maxDistance);

        //构造轨迹字符串
        StringBuilder pathBuilder = new StringBuilder();
        for (Point point : points) {
            pathBuilder.append(point.getLongitude()).append(",").append(point.getLatitude()).append(";");
        }
        //构造边界字符串
        StringBuilder boundaryBuilder = new StringBuilder();
        for (Point point : boundaryPoints) {
            boundaryBuilder.append(point.getLongitude()).append(",").append(point.getLatitude()).append(";");
        }
        pathBuilder.setLength(pathBuilder.length() - 1);
        String url = staticMapUrl + "?key=" + apiKey +
                "&zoom=" + zoom +
                "&location" + centerLon + "," + centerLat +
                "&size=10000*10000" +
                "&paths=10," + TRAJECTORY + ",1,,:" + pathBuilder +
                "|10," + BOUNDARY + ",1," + FILL + ",:" + boundaryBuilder;
        //去掉url的最后一个;
        url = url.substring(0, url.length() - 1);
        byte[] imageBytes = downloadImage(url);

        if (imageBytes == null || imageBytes.length == 0) {
            throw new IOException("下载图片失败");
        }
        String fileName = PICTURE_PREFIX + UUID.randomUUID().toString() + PICTURE_SUFFIX;

        MultipartFile multipartFile = new MultipartFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return imageBytes.length;
            }

            @Override
            public byte[] getBytes() throws IOException {
                return imageBytes;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(imageBytes);
            }

            @Override
            public void transferTo(File dest) throws IOException, IllegalStateException {
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    fos.write(imageBytes);
                }
            }
        };

        return MinioUtil.uploadFile(bucketName, fileName, multipartFile);
    }

    private int calculateZoom(double distance) {
        // 注意：这里假设输入的距离单位是米。如果输入是千米，需要乘以1000。
        if (distance >= DistanceConstants.ZERO && distance < DistanceConstants.TEN) {
            // 注意：由于您给出的数据中最小距离是10m，所以这里实际上不会匹配到任何值，但为了完整性还是保留。
            // 如果需要处理小于10m的情况，请添加相应的逻辑。
            return 20;
        } else if (distance < DistanceConstants.TWENTY_FIVE) {
            // 对于10m-25m（不包括25m），缩放级别为19-20，这里取上限20（根据实际需求调整）
            return 20;
        } else if (distance < DistanceConstants.FIFTY) {
            return 19;
        } else if (distance < DistanceConstants.HUNDRED) {
            return 18;
        } else if (distance < DistanceConstants.TWO_HUNDRED) {
            return 17;
        } else if (distance < DistanceConstants.FIVE_HUNDRED) {
            return 16;
        } else if (distance < DistanceConstants.THOUSAND) {
            return 15;
        } else if (distance < DistanceConstants.TWO_THOUSAND) {
            return 14;
        } else if (distance < DistanceConstants.FIVE_THOUSAND) {
            return 13;
        } else if (distance < DistanceConstants.TEN_THOUSAND) {
            return 12;
        } else if (distance < DistanceConstants.TWENTY_THOUSAND) {
            return 11;
        } else if (distance < DistanceConstants.THIRTY_THOUSAND) {
            return 10;
        } else if (distance < DistanceConstants.FIFTY_THOUSAND) {
            return 9;
        } else if (distance < DistanceConstants.HUNDRED_THOUSAND) {
            return 8;
        } else if (distance < DistanceConstants.TWO_HUNDRED_THOUSAND) {
            return 7;
        } else if (distance < DistanceConstants.FIVE_HUNDRED_THOUSAND) {
            return 6;
        } else if (distance < DistanceConstants.MILLION) {
            return 5;
        } else {
            // 如果距离超过1000km，根据需求可以返回特定值或继续扩展范围
            // 无效距离（或根据需求返回其他值）
            return 5;
        }
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        URL url = new URL(imageUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }
        } else {
            throw new IOException("请求失败，响应码为: " + responseCode);
        }
    }
}