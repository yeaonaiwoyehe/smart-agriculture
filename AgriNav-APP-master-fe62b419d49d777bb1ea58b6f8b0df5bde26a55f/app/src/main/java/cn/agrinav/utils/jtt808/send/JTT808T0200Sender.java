package cn.agrinav.utils.jtt808.send;

import cn.agrinav.utils.netty.NettyClientManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class JTT808T0200Sender {
    private final NettyClientManager manager;

    public JTT808T0200Sender(NettyClientManager manager) {
        this.manager = manager;
    }

    public void sendLocationMessage(String terminalId, double latitude, double longitude, int speed, int direction, long timestamp, Map<Integer, Object> additionalInfo) {
        ByteBuf message = Unpooled.buffer();

        // 计算附加信息的长度
        int additionalInfoLength = calculateAdditionalInfoLength(additionalInfo);

        // 消息头
        message.writeShort(0x0200); // 消息ID
        message.writeShort(28 + additionalInfoLength); // 消息体长度
        writeTerminalId(message, terminalId);
        message.writeShort(0); // 消息流水号，这里简化处理，实际应用中需要维护

        // 消息体
        message.writeInt(0); // 报警标志
        message.writeInt(0x00000001); // 状态（根据实际情况设置，这里设置为ACC开启）
        message.writeInt((int) (latitude * 1000000)); // 纬度
        message.writeInt((int) (longitude * 1000000)); // 经度
        message.writeShort(0); // 高程（根据实际情况设置）
        message.writeShort((short) speed); // 速度
        message.writeShort((short) direction); // 方向
        message.writeBytes(timestampToBCD(timestamp)); // 时间（BCD码）

        // 附加信息
        writeAdditionalInfo(message, additionalInfo);

        // 计算校验码
        byte checkSum = calculateCheckSum(message);
        message.writeByte(checkSum);

        // 转义处理
        ByteBuf escapedMessage = escapeMessage(message);

        // 发送消息
        String hexMessage = ByteBufUtil.hexDump(escapedMessage);
        manager.sendMessage(hexMessage, 50000, TimeUnit.MILLISECONDS);
    }

    private int calculateAdditionalInfoLength(Map<Integer, Object> additionalInfo) {
        int length = 0;
        for (Map.Entry<Integer, Object> entry : additionalInfo.entrySet()) {
            length += 2; // ID (1 byte) + Length (1 byte)
            if (entry.getValue() instanceof Byte) {
                length += 1;
            } else if (entry.getValue() instanceof Short) {
                length += 2;
            } else if (entry.getValue() instanceof Integer) {
                length += 4;
            } else if (entry.getValue() instanceof String) {
                length += ((String) entry.getValue()).getBytes(StandardCharsets.UTF_8).length;
            }
        }
        return length;
    }

    private void writeAdditionalInfo(ByteBuf message, Map<Integer, Object> additionalInfo) {
        for (Map.Entry<Integer, Object> entry : additionalInfo.entrySet()) {
            int id = entry.getKey();
            Object value = entry.getValue();

            message.writeByte(id);
            if (value instanceof Byte) {
                message.writeByte(1);
                message.writeByte((Byte) value);
            } else if (value instanceof Short) {
                message.writeByte(2);
                message.writeShort((Short) value);
            } else if (value instanceof Integer) {
                message.writeByte(1);
                message.writeInt((Integer) value & 0xFF); // 只取低8位
            } else if (value instanceof String) {
                byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                message.writeByte(bytes.length);
                message.writeBytes(bytes);
            }
        }
    }
    
 private void writeTerminalId(ByteBuf buf, String terminalId) {
        byte[] idBytes = terminalId.getBytes(StandardCharsets.US_ASCII);
        if (idBytes.length != 6) {
            // 如果不是 6 字节，需要进行 BCD 编码
            byte[] bcdBytes = new byte[6];
            for (int i = 0; i < 12; i += 2) {
                byte high = (byte) (terminalId.charAt(i) - '0');
                byte low = (byte) (terminalId.charAt(i + 1) - '0');
                bcdBytes[i / 2] = (byte) ((high << 4) | low);
            }
            buf.writeBytes(bcdBytes);
        } else {
            // 如果刚好是 6 字节，直接写入
            buf.writeBytes(idBytes);
        }
    }

    private byte[] longToBytes(long value) {
        byte[] result = new byte[6];
        for (int i = 5; i >= 0; i--) {
            result[i] = (byte) (value % 100);
            value /= 100;
        }
        return result;
    }

    private byte calculateCheckSum(ByteBuf buf) {
        byte checkSum = 0;
        for (int i = 0; i < buf.readableBytes(); i++) {
            checkSum ^= buf.getByte(i);
        }
        return checkSum;
    }

    private ByteBuf escapeMessage(ByteBuf original) {
        ByteBuf escaped = Unpooled.buffer();
        escaped.writeByte(0x7e); // 起始符

        for (int i = 0; i < original.readableBytes(); i++) {
            byte b = original.getByte(i);
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

        escaped.writeByte(0x7e); // 结束符
        return escaped;
    }

    private byte[] timestampToBCD(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        String dateStr = sdf.format(new Date(timestamp));
        byte[] bcdTime = new byte[6];
        for (int i = 0; i < 6; i++) {
            bcdTime[i] = (byte) (((dateStr.charAt(i*2) - '0') << 4) | (dateStr.charAt(i*2+1) - '0'));
        }
        return bcdTime;
    }
}