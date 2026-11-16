package cn.agrinav.utils.jtt808.receive;

public class JTT808RegisterResponse extends JTT808Response {
    private short responseSerialNumber;
    private byte result;
    private String authCode;

    public JTT808RegisterResponse(String terminalId, short serialNumber, short responseSerialNumber, byte result, String authCode) {
        super((short) 0x8100, terminalId, serialNumber);
        this.responseSerialNumber = responseSerialNumber;
        this.result = result;
        this.authCode = authCode;
    }

    // Getters and setters

    public short getResponseSerialNumber() {
        return responseSerialNumber;
    }

    public void setResponseSerialNumber(short responseSerialNumber) {
        this.responseSerialNumber = responseSerialNumber;
    }

    public byte getResult() {
        return result;
    }

    public void setResult(byte result) {
        this.result = result;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }
}