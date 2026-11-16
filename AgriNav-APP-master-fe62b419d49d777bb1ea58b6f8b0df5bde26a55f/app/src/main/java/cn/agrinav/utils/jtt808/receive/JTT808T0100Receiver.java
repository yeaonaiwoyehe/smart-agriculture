package cn.agrinav.utils.jtt808.receive;

import android.util.Log;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class JTT808T0100Receiver {

    private static final String TAG = "JTT808T0100Receiver";

    public JTT808RegisterResponse receiveRegistrationResponse(String hexResponse) {
        // Remove start and end markers (7E)
        String cleanHexResponse = hexResponse.substring(2, hexResponse.length() - 2);
        ByteBuf byteBuf = Unpooled.wrappedBuffer(hexStringToByteArray(cleanHexResponse));

        // Parse message header
        short messageId = byteBuf.readShort();
        short bodyProperties = byteBuf.readShort();
        String terminalId = readTerminalId(byteBuf);
        short serialNumber = byteBuf.readShort();

        // Parse message body
        short responseSerialNumber = byteBuf.readShort();
        byte result = byteBuf.readByte();
        String authCode = null;
        if (result == 0) {
            authCode = readString(byteBuf);
        }

        // Create and return the response object
        JTT808RegisterResponse response = new JTT808RegisterResponse(terminalId, serialNumber, responseSerialNumber, result, authCode);
        Log.d(TAG, "Received registration response: " + response.toString());
        return response;
    }

    private String readTerminalId(ByteBuf buf) {
        byte[] idBytes = new byte[6];
        buf.readBytes(idBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : idBytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    private String readString(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
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