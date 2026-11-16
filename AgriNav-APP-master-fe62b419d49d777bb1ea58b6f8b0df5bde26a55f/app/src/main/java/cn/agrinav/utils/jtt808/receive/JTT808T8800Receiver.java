package cn.agrinav.utils.jtt808.receive;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class JTT808T8800Receiver {
    
    public JTT808Response parseResponse(String hexString) {
        // 移除起始和结束标识符
        if (hexString.startsWith("7E") && hexString.endsWith("7E")) {
            hexString = hexString.substring(2, hexString.length() - 2);
        }
        
        // 反转义
        hexString = hexString.replace("7D02", "7E").replace("7D01", "7D");
        
        // 转换为ByteBuf
        ByteBuf buf = Unpooled.wrappedBuffer(hexStringToBytes(hexString));
        
        try {
            // 消息ID (2字节)
            short messageId = buf.readShort();
            
            // 消息体属性 (2字节)
            int bodyProps = buf.readUnsignedShort();
            int bodyLength = bodyProps & 0x3FF; // 消息体长度，10位
            
            // 终端手机号 (6字节)
            byte[] phoneBytes = new byte[6];
            buf.readBytes(phoneBytes);
            String terminalId = bcdToString(phoneBytes);
            
            // 消息流水号 (2字节)
            short serialNumber = buf.readShort();
            
            // 多媒体ID (4字节)
            int mediaId = buf.readInt();
            
            // 重传包总数 (1字节)
            byte retransmitCount = buf.readByte();
            
            // 重传包ID列表 (2字节 * 重传包数)
            short[] retransmitIds = null;
            if (retransmitCount > 0) {
                retransmitIds = new short[retransmitCount];
                for (int i = 0; i < retransmitCount; i++) {
                    retransmitIds[i] = buf.readShort();
                }
            }
            
            // 创建响应对象
            return new JTT808Response(
                messageId,
                terminalId,
                serialNumber,
                mediaId,
                retransmitCount,
                retransmitIds
            );
            
        } finally {
            buf.release();
        }
    }
    
    private byte[] hexStringToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
    
    private String bcdToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}
