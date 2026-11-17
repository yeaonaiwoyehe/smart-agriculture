package org.yzh.client;

import io.netty.buffer.Unpooled;
import org.yzh.client.netty.ClientConfig;
import org.yzh.client.netty.HandlerMapping;
import org.yzh.client.netty.TCPClient;
import org.yzh.protocol.codec.JTMessageAdapter;
import org.yzh.protocol.commons.JT808;
import org.yzh.protocol.t808.T0100;
import org.yzh.protocol.t808.T0200;
import org.yzh.protocol.t808.T0801;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

/**
 * 端到端多媒体模拟终端：注册 -> 上报一张 JPEG 图片（T0801）。
 * 验证去 MinIO 后：本地文件落盘、ES media_files 索引、/media/** 可访问。
 */
public class MediaSimulator {

    private static final String PHONE = "15888888888";

    public static void main(String[] args) throws Exception {
        JTMessageAdapter messageAdapter = new JTMessageAdapter("org.yzh.protocol");

        ClientConfig config = new ClientConfig.Builder()
                .setIp("127.0.0.1")
                .setPort(7100)
                .setMaxFrameLength(1024 * 1024)
                .setDelimiters(new byte[]{0x7e})
                .setDecoder(messageAdapter)
                .setEncoder(messageAdapter)
                .setHandlerMapping(new HandlerMapping("org.yzh.client"))
                .build();

        TCPClient client = new TCPClient("media", config).start();

        // 1. 终端注册
        client.writeObject(buildRegister());
        Thread.sleep(1500);

        // 2. 生成一张 160x120 的测试 JPEG
        BufferedImage img = new BufferedImage(160, 120, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 160; x++) {
            for (int y = 0; y < 120; y++) {
                img.setRGB(x, y, (x * 2) << 16 | (y * 2) << 8 | 128);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] jpeg = baos.toByteArray();

        // 3. 构造并发送 T0801
        T0801 media = new T0801();
        media.setMessageId(JT808.多媒体数据上传);
        media.setProtocolVersion(1);
        media.setVersion(true);
        media.setClientId(PHONE);
        media.setId(1);
        media.setType(0);
        media.setFormat(0);
        media.setEvent(0);
        media.setChannelId(1);
        media.setLocation(buildLocation(114.8225, 38.0225, LocalDateTime.now()));
        media.setPacket(Unpooled.wrappedBuffer(jpeg));

        client.writeObject(media);

        // 等待异步处理完成
        Thread.sleep(4000);
        client.stop();
        System.out.println("MediaSimulator finished, jpeg bytes=" + jpeg.length);
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
        message.setDeviceModel("agrinav-media");
        message.setDeviceId("MED00001");
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
        message.setSpeed(0);
        message.setDirection(90);
        message.setDeviceTime(deviceTime);
        return message;
    }
}
