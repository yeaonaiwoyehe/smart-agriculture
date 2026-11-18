package org.yzh.client;

import org.yzh.client.netty.ClientConfig;
import org.yzh.client.netty.HandlerMapping;
import org.yzh.client.netty.TCPClient;
import org.yzh.protocol.codec.JTMessageAdapter;
import org.yzh.protocol.commons.JT808;
import org.yzh.protocol.commons.transform.AttributeKey;
import org.yzh.protocol.t808.T0100;
import org.yzh.protocol.t808.T0200;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 密集作业模拟终端：注册后，在两块真实耕地（3011/3012）内分别上报蛇形（boustrophedon）
 * 作业轨迹点，模拟农机来回耕作。所有点经内缩处理，确保落在地块多边形内部，
 * 经完整 TCP 链路（checkPoint 空间判定）写入 effective_point_buffer，供作业地块与热力图展示。
 *
 * 作业类型 work_type=4001（耕地），作业幅宽 width=200（厘米）。
 */
public class DenseWorkSimulator {

    private static final String PHONE = "15888888888";
    private static final int WORK_TYPE = 4001;
    private static final int WIDTH = 200;
    private static final double MARGIN = 0.0002;

    public static void main(String[] args) throws Exception {
        JTMessageAdapter messageAdapter = new JTMessageAdapter("org.yzh.protocol");

        ClientConfig config = new ClientConfig.Builder()
                .setIp("127.0.0.1")
                .setPort(7100)
                .setMaxFrameLength(2 + 21 + 1023 * 2 + 1 + 2)
                .setDelimiters(new byte[]{0x7e})
                .setDecoder(messageAdapter)
                .setEncoder(messageAdapter)
                .setHandlerMapping(new HandlerMapping("org.yzh.client"))
                .build();

        TCPClient client = new TCPClient("dense", config).start();

        // 1. 终端注册
        client.writeObject(buildRegister());
        Thread.sleep(1500);

        // 2. 生成两块地的蛇形作业点（坐标均内缩，保证在多边形内）
        List<double[]> allPoints = new ArrayList<>();
        // 3011 真实耕地一号
        allPoints.addAll(serpentine(
                114.811664 + MARGIN, 114.815312 - MARGIN,
                38.005872 + MARGIN, 38.007817 - MARGIN,
                8, 12));
        // 3012 真实耕地二号
        allPoints.addAll(serpentine(
                114.806542 + MARGIN, 114.808366 - MARGIN,
                38.006397 + MARGIN, 38.008156 - MARGIN,
                8, 12));

        // 3. 逐点上报
        LocalDateTime base = LocalDateTime.now().minusMinutes(allPoints.size() * 2L / 60L);
        for (int i = 0; i < allPoints.size(); i++) {
            double[] p = allPoints.get(i);
            client.writeObject(buildLocation(p[0], p[1], base.plusSeconds(2L * i)));
            Thread.sleep(80);
        }

        // 4. 等待异步处理与落库完成
        Thread.sleep(6000);
        client.stop();
        System.out.println("DenseWorkSimulator finished, points=" + allPoints.size() + ", phone=" + PHONE);
    }

    /**
     * 在给定经纬度矩形内生成蛇形点：rows 条平行作业行，每行 perRow 个点，相邻行方向相反。
     * 数组元素为 [经度, 纬度]。
     */
    private static List<double[]> serpentine(double lngMin, double lngMax,
                                             double latMin, double latMax,
                                             int rows, int perRow) {
        List<double[]> points = new ArrayList<>(rows * perRow);
        double latStep = (latMax - latMin) / (rows - 1);
        for (int r = 0; r < rows; r++) {
            double lat = latMin + latStep * r;
            for (int c = 0; c < perRow; c++) {
                double lng;
                if (r % 2 == 0) {
                    lng = lngMin + (lngMax - lngMin) * c / (perRow - 1);
                } else {
                    lng = lngMax - (lngMax - lngMin) * c / (perRow - 1);
                }
                points.add(new double[]{lng, lat});
            }
        }
        return points;
    }

    private static T0100 buildRegister() {
        T0100 message = new T0100();
        message.setMessageId(JT808.终端注册);
        message.setProtocolVersion(1);
        message.setVersion(true);
        message.setClientId(PHONE);

        message.setProvinceId(31);
        message.setCityId(115);
        message.setMakerId("yzh");
        message.setDeviceModel("agrinav-dense");
        message.setDeviceId("DENSE0001");
        message.setPlateColor(1);
        message.setPlateNo("测A00002");
        return message;
    }

    private static T0200 buildLocation(double lng, double lat, LocalDateTime deviceTime) {
        T0200 message = new T0200();
        message.setMessageId(JT808.位置信息汇报);
        message.setProtocolVersion(1);
        message.setVersion(true);
        message.setClientId(PHONE);

        message.setWarnBit(0);
        message.setStatusBit(2048);
        message.setLatitude((int) Math.round(lat * 1000000d));
        message.setLongitude((int) Math.round(lng * 1000000d));
        message.setAltitude(60);
        message.setSpeed(120);
        message.setDirection(90);
        message.setDeviceTime(deviceTime);

        Map<Integer, Object> attributes = new TreeMap<>();
        attributes.put(AttributeKey.WorkType, WORK_TYPE);
        attributes.put(AttributeKey.Width, WIDTH);
        message.setAttributes(attributes);
        return message;
    }
}
