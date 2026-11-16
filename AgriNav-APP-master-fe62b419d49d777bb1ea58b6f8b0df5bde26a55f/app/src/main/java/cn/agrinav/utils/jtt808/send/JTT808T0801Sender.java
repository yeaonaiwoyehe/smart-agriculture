package cn.agrinav.utils.jtt808.send;

import android.annotation.SuppressLint;
import android.util.Log;

import cn.agrinav.utils.netty.NettyClientManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JTT808T0801Sender {
    private static final int MSG_ID = 0x0801;
    private static final int MAX_PACKAGE_SIZE = 1023;
    private final NettyClientManager manager;
    private int msgSerialNo = 0;

    public JTT808T0801Sender(NettyClientManager manager) {
        this.manager = manager;
    }

    public void send(String terminalId, byte[] mediaData, int mediaId,
                     double latitude, double longitude, int speed, int direction,
                     long timestamp, Map<Integer, Object> additionalInfo) {
        msgSerialNo = 0;
        
        // 第一个包的消息体预留空间（多媒体信息8字节 + 位置信息28字节 = 36字节）
        final int FIRST_PACKAGE_HEADER = 36;
        // 第一个包实际可用的图片数据空间
        final int FIRST_PACKAGE_DATA_SIZE = MAX_PACKAGE_SIZE - FIRST_PACKAGE_HEADER;
        
        // 计算总包数（考虑第一包可用空间较小）
        int remainingLength = mediaData.length;
        int firstPackageDataLength = Math.min(FIRST_PACKAGE_DATA_SIZE, remainingLength);
        remainingLength -= firstPackageDataLength;
        int additionalPackages = (int) Math.ceil(remainingLength / (double) MAX_PACKAGE_SIZE);
        int totalPackages = 1 + additionalPackages;

        // 发送第一个包
        byte[] firstPackageData = new byte[FIRST_PACKAGE_DATA_SIZE];  // 固定大小
        System.arraycopy(mediaData, 0, firstPackageData, 0, firstPackageDataLength);
        
        ByteBuf firstMessage = buildPackageMessage(terminalId, firstPackageData, mediaId,
                1, totalPackages, latitude, longitude, speed, direction,
                timestamp, additionalInfo, false);
        sendPackage(firstMessage);

        // 发送剩余的包
        int offset = firstPackageDataLength;
        for (int i = 1; i < totalPackages; i++) {
            boolean isLastPackage = (i == totalPackages - 1);
            int currentDataSize;
            byte[] packageData;
            
            if (isLastPackage) {
                // 最后一个包：使用实际剩余数据长度
                currentDataSize = mediaData.length - offset;
                packageData = new byte[currentDataSize];
            } else {
                // 中间包：使用固定长度
                currentDataSize = MAX_PACKAGE_SIZE;
                packageData = new byte[MAX_PACKAGE_SIZE];
            }
            
            System.arraycopy(mediaData, offset, packageData, 0, currentDataSize);
            
            ByteBuf message = buildPackageMessage(terminalId, packageData, mediaId,
                    i + 1, totalPackages, latitude, longitude, speed, direction,
                    timestamp, additionalInfo, isLastPackage);
            sendPackage(message);
            
            offset += currentDataSize;
        }
    }

    private ByteBuf buildPackageMessage(String terminalId, byte[] packageData, int mediaId,
                                        int packageNo, int totalPackages,
                                        double latitude, double longitude, int speed, int direction,
                                        long timestamp, Map<Integer, Object> additionalInfo,
                                        boolean isLastPackage) {
        ByteBuf message = Unpooled.buffer();
        
        // 消息头
        message.writeShort(MSG_ID);

        // 消息体属性
        int msgBodyProps;
        if (isLastPackage) {
            // 最后一个包：使用实际数据长度
            msgBodyProps = packageData.length & 0x3FF;
        } else {
            // 其他包：使用固定长度
            msgBodyProps = 0x3FF;
        }
        msgBodyProps |= (1 << 13);  // 分包标志位
        message.writeShort(msgBodyProps);

        // 基本信息
        writeTerminalId(message, terminalId);
        message.writeShort(msgSerialNo++);
        message.writeShort(totalPackages);
        message.writeShort(packageNo);

        if (packageNo == 1) {
            // 多媒体信息（第一个包需要）
            message.writeInt(mediaId);      // 多媒体数据id
            message.writeByte(0);     // 多媒体类型
            message.writeByte(0);     // 多媒体格式编码
            message.writeByte(0);     // 事件项编码
            message.writeByte(1);     // 通道id

            // 位置信息（仅第一个包需要）
            message.writeInt(0);                // 报警标志
            message.writeInt(0x00000001);       // 状态
            message.writeInt((int) (latitude * 1000000));
            message.writeInt((int) (longitude * 1000000));
            message.writeShort(0);              // 高程
            message.writeShort((short) speed);
            message.writeShort((short) direction);
            message.writeBytes(timestampToBCD(timestamp));
        }

        // 多媒体数据包
        message.writeBytes(packageData);

        return message;
    }

    // 处理单个包的发送
    private void sendPackage(ByteBuf message) {
        ByteBuf escapedMessage = escapeMessage(message);
        String hexMessage = ByteBufUtil.hexDump(escapedMessage);
        manager.sendMessage(hexMessage, 5000, TimeUnit.MILLISECONDS);

        message.release();
        escapedMessage.release();

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Log.e("JTT808T0801Sender", "线程睡眠异常", e);
        }
    }

    private void writeTerminalId(ByteBuf buf, String terminalId) {
        byte[] idBytes = new byte[6];
        for (int i = 0; i < 12; i += 2) {
            byte high = (byte) (terminalId.charAt(i) - '0');
            byte low = (byte) (terminalId.charAt(i + 1) - '0');
            idBytes[i / 2] = (byte) ((high << 4) | low);
        }
        buf.writeBytes(idBytes);
    }

    private ByteBuf escapeMessage(ByteBuf original) {
        ByteBuf escaped = Unpooled.buffer();
        escaped.writeByte(0x7e); // 起始符

        byte checkSum = 0;
        for (int i = 0; i < original.readableBytes(); i++) {
            byte b = original.getByte(i);
            checkSum ^= b;

            if (b == 0x7e) {
                escaped.writeByte(0x7d);
                escaped.writeByte(0x02);
            } else if (b == 0x7d) {
                escaped.writeByte(0x7d);
                escaped.writeByte(0x01);
            } else {
                escaped.writeByte(b);
            }
        }

        if (checkSum == 0x7e) {
            escaped.writeByte(0x7d);
            escaped.writeByte(0x02);
        } else if (checkSum == 0x7d) {
            escaped.writeByte(0x7d);
            escaped.writeByte(0x01);
        } else {
            escaped.writeByte(checkSum);
        }

        escaped.writeByte(0x7e); // 结束符
        return escaped;
    }

    private byte[] timestampToBCD(long timestamp) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        String dateStr = sdf.format(new Date(timestamp));
        byte[] bcdTime = new byte[6];
        for (int i = 0; i < 6; i++) {
            bcdTime[i] = (byte) (((dateStr.charAt(i*2) - '0') << 4) | (dateStr.charAt(i*2+1) - '0'));
        }
        return bcdTime;
    }
}