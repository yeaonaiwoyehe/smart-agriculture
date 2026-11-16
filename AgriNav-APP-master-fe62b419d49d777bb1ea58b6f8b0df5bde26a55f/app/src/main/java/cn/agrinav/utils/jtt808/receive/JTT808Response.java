package cn.agrinav.utils.jtt808.receive;

public class JTT808Response {
    protected short messageId;
    protected String terminalId;
    protected short serialNumber;

    // 多媒体数据上传应答新增字段
    protected int mediaId;            // 多媒体ID
    protected byte retransmitCount;   // 重传包总数
    protected short[] retransmitIds;  // 重传包ID列表

    public JTT808Response(short messageId, String terminalId, short serialNumber) {
        this.messageId = messageId;
        this.terminalId = terminalId;
        this.serialNumber = serialNumber;
    }

    // 新增构造函数，用于多媒体数据上传应答
    public JTT808Response(short messageId, String terminalId, short serialNumber,
                         int mediaId, byte retransmitCount, short[] retransmitIds) {
        this(messageId, terminalId, serialNumber);
        this.mediaId = mediaId;
        this.retransmitCount = retransmitCount;
        this.retransmitIds = retransmitIds;
    }

    public short getMessageId() {
        return messageId;
    }

    public void setMessageId(short messageId) {
        this.messageId = messageId;
    }

    public String getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(String terminalId) {
        this.terminalId = terminalId;
    }

    public short getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(short serialNumber) {
        this.serialNumber = serialNumber;
    }

    // 新增字段的 getter 和 setter
    public int getMediaId() {
        return mediaId;
    }

    public void setMediaId(int mediaId) {
        this.mediaId = mediaId;
    }

    public byte getRetransmitCount() {
        return retransmitCount;
    }

    public void setRetransmitCount(byte retransmitCount) {
        this.retransmitCount = retransmitCount;
    }

    public short[] getRetransmitIds() {
        return retransmitIds;
    }

    public void setRetransmitIds(short[] retransmitIds) {
        this.retransmitIds = retransmitIds;
    }

    // Getters and setters
}