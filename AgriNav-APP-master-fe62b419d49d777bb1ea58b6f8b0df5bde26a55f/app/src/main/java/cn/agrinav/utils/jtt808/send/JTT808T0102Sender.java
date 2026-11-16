package cn.agrinav.utils.jtt808.send;

import cn.agrinav.utils.netty.NettyClientManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class JTT808T0102Sender {
    private NettyClientManager manager;

    public JTT808T0102Sender(NettyClientManager manager) {
        this.manager = manager;
    }

    public void sendAuthenticationMessage(String terminalId, String authCode) {
        ByteBuf message = Unpooled.buffer();

        // 消息头
        message.writeShort(0x0102); // 消息ID
        byte[] authCodeBytes = authCode.getBytes(StandardCharsets.UTF_8);
        message.writeShort(authCodeBytes.length); // 消息体长度
        writeTerminalId(message, terminalId);
        message.writeShort(0); // 消息流水号，这里简化处理，实际应用中需要维护

        // 消息体
        message.writeBytes(authCodeBytes);

        // 计算校验码
        byte checkSum = calculateCheckSum(message);
        message.writeByte(checkSum);

        // 转义处理
        ByteBuf escapedMessage = escapeMessage(message);

        // 发送消息
        manager.sendMessage(ByteBufUtil.hexDump(escapedMessage), 50000, TimeUnit.MILLISECONDS);
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
}