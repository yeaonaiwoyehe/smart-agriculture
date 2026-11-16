package cn.agrinav.utils.netty;


public interface MessageListener {
    // 定义一个接口用于回调消息
    void onMessage(String message);
}
