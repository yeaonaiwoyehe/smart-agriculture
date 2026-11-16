package cn.agrinav.utils.jtt808.receive;

import java.nio.ByteBuffer;

public class JTT808T8001Receiver {
    private static final String TAG = "JTT808T8001Receiver";

    public int processResponse(String hexResponse) {
        ByteBuffer buffer = ByteBuffer.wrap(hexStringToByteArray(hexResponse));

        // 跳过起始符 0x7e
        buffer.get();

        // 解析消息头
        short messageId = buffer.getShort();
        short bodyProperties = buffer.getShort();
        byte[] terminalIdBytes = new byte[6];
        buffer.get(terminalIdBytes);
        short messageSerialNumber = buffer.getShort();

        // 解析消息体
        short responseMessageId = buffer.getShort();
        short responseSerialNumber = buffer.getShort();
        byte result = buffer.get();

        // 跳过校验码和结束符
        
        // 处理解析结果
        return result;
    }

    private String interpretResult(byte result) {
        switch (result) {
            case 0:
                return "成功";
            case 1:
                return "失败";
            case 2:
                return "消息有误";
            case 3:
                return "不支持";
            default:
                return "未知结果";
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}