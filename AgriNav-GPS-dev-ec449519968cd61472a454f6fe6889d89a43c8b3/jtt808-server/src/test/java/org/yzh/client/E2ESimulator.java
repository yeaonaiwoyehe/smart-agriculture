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
import java.util.Map;
import java.util.TreeMap;

/**
 * 端到端模拟终端：注册 -> 上报 4 个落在地块 3001 内部的位置点。
 * 手机号使用系统已存在的用户（admin 15888888888），坐标均经 PostGIS ST_Contains 验证在 3001 内。
 */
public class E2ESimulator {

    private static final String PHONE = "15888888888";

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

        TCPClient client = new TCPClient("e2e", config).start();

        // 1. 终端注册
        client.writeObject(buildRegister());
        Thread.sleep(1500);

        // 2. 位置点（经度, 纬度），均在 3001 内
        double[][] points = {
                {114.821, 38.021},
                {114.822, 38.022},
                {114.823, 38.022},
                {114.824, 38.023}
        };
        LocalDateTime base = LocalDateTime.now().minusMinutes(5);
        for (int i = 0; i < points.length; i++) {
            client.writeObject(buildLocation(points[i][0], points[i][1], base.plusSeconds(30L * i)));
            Thread.sleep(600);
        }

        // 等待异步处理完成
        Thread.sleep(3000);
        client.stop();
        System.out.println("E2ESimulator finished, phone=" + PHONE);
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
        message.setDeviceModel("agrinav-e2e");
        message.setDeviceId("E2E00001");
        message.setPlateColor(1);
        message.setPlateNo("测A00001");
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
        attributes.put(AttributeKey.WorkType, 1);
        attributes.put(AttributeKey.Width, 200);
        message.setAttributes(attributes);
        return message;
    }
}
