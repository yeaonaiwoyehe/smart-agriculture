package cn.agrinav.utils.jtt808.exception;

public class HeartbeatSendFailedException extends Exception {
    private final String errorCode;

    public HeartbeatSendFailedException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}