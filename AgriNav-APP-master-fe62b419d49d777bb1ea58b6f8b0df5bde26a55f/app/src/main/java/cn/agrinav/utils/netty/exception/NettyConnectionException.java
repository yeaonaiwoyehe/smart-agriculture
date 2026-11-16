package cn.agrinav.utils.netty.exception;

public class NettyConnectionException extends Exception {
    private final String errorCode;

    public NettyConnectionException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}