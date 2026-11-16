package cn.agrinav.utils.netty;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.agrinav.utils.netty.exception.NettyConnectionException;

public class NettyClientManager implements MessageListener {
    private NettyClient nettyClient;
    private CompletableFuture<String> messageFuture;

    private static class SingleHolder {
        private static final NettyClientManager INSTANCE = new NettyClientManager();
    }

    public static NettyClientManager getInstance() {
        return SingleHolder.INSTANCE;
    }

    public static void setContext(Context context) {
        NettyClient.setContext(context);
    }

    private NettyClientManager() {}

    public CompletableFuture<String> createConnect(String message) {
        // 初始化NettyClient实例
        nettyClient = new NettyClient();
        // 初始化CompletableFuture对象，以便后续接收消息
        messageFuture = new CompletableFuture<>();
        // 给NettyClient的messageListener赋值
        nettyClient.setMessageListener(this);
        try {
            // 连接服务器端
            nettyClient.connect();
            // 发送消息
            nettyClient.sendMessage(message);
            // 返回messageFuture对象，以便外部可以异步获取消息
            return messageFuture;
        }catch (NettyConnectionException e) {
            // 通过messageFuture传递异常,避免长时间等待
            messageFuture.completeExceptionally(e);
            return messageFuture;
        }
    }

    // 仅连接服务器端
    public void connect() throws NettyConnectionException {
        // 初始化NettyClient实例
        nettyClient = new NettyClient();
        // 初始化CompletableFuture对象，以便后续接收消息
        messageFuture = new CompletableFuture<>();
        // 给NettyClient的messageListener赋值
        nettyClient.setMessageListener(this);
        // 连接服务器端
        nettyClient.connect();
    }

    // 发送消息
    public CompletableFuture<String> send(String msg) {
        // 初始化CompletableFuture对象，以便后续接收消息
        messageFuture = new CompletableFuture<>();
        try {
            // 发送消息
            nettyClient.sendMessage(msg);
            // 返回messageFuture对象，以便外部可以异步获取消息
            return messageFuture;
        }catch (Exception e) {
            // 通过messageFuture传递异常,避免长时间等待
            messageFuture.completeExceptionally(e);
            return messageFuture;
        }
    }

    // 发送消息到服务器端并接收返回
    public String sendMessage(String msg, long timeout, TimeUnit unit) {
        CompletableFuture<String> future = send(msg);
        try {
            Log.i("registerDevice()", "等待消息");
            // 阻塞等待，直到接收到消息或超时
            return future.get(timeout, unit);
        } catch (ExecutionException | InterruptedException e) {
            Log.i("ShowNettyConnectException", "连接失败");
        } catch (TimeoutException e) {
            Log.i("ShowNettyConnectException", "连接超时");
        }
        // 发生异常或超时，返回默认消息
        return "未收到服务器端消息";
    }

    // 用于连接服务器、发送消息并阻塞等待直到接收到消息或超时
    public String connectAndSend(String msg, long timeout, TimeUnit unit) {
        CompletableFuture<String> future = createConnect(msg);
        try {
            Log.i("registerDevice()", "等待消息");
            // 阻塞等待，直到接收到消息或超时
            return future.get(timeout, unit);
        } catch (ExecutionException | InterruptedException e) {
            Log.i("ShowNettyConnectException", "连接失败");
        } catch (TimeoutException e) {
            Log.i("ShowNettyConnectException", "连接超时");
        }
        // 发生异常或超时，返回默认消息
        return "未收到服务器端消息";
    }

    // 关闭NettyClient连接
    public void close() {
        if(nettyClient != null) {
            nettyClient.close();
            nettyClient = null;
        }
    }

    // 实现MessageListener接口的onMessage方法，用于处理服务器发送的消息
    @Override
    public void onMessage(String msg) {
        // 通知messageFuture的complete方法完成异步操作，将接收到的消息作为结果
        messageFuture.complete(msg);
    }
}
