package cn.agrinav.utils.netty;

import android.content.Context;

import cn.agrinav.R;
import cn.agrinav.utils.netty.coder.MessageDecoder;
import cn.agrinav.utils.netty.coder.MessageEncoder;
import cn.agrinav.utils.netty.exception.NettyConnectionException;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
    private EventLoopGroup group;
    private Channel channel;
    private ChannelFuture future;
    private MessageListener listener;
    private static Context context;

    public static void setContext(Context context) {
        NettyClient.context = context;
    }

    // 连接并手动关闭
    public void connect() throws NettyConnectionException {
        // 创建EventLoopGroup实例，处理I/O操作
        group = new NioEventLoopGroup();
        // 创建Bootstrap实例
        Bootstrap bootstrap = new Bootstrap();
        // 设置EventLoopGroup
        bootstrap.group(group)
                .option(ChannelOption.SO_KEEPALIVE, true)
                // 设置用于NIO传输的Channel类型
                .channel(NioSocketChannel.class)
                // 设置Channel的初始化器
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 设置ChannelPipeline中的处理器
                        ch.pipeline().addLast(new MessageDecoder()); // 字符串解码器
                        ch.pipeline().addLast(new MessageEncoder()); // 字符串编码器
                        // 添加自定义的ChannelInboundHandler，用于处理接收到的消息
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                // 调用onMessageReceived()方法处理接收到的消息
                                onMessageReceived(msg);
                            }
                        });
                    }
                });
        // 连接到服务器，并等待连接完成
        try {
            String ip = context.getResources().getString(R.string.jtt808_ip);
            String[] split = ip.split(":");
            String[] split1 = split[1].split("//");
            String host = split1[1];
            int port = Integer.parseInt(split[2]);
            future = bootstrap.connect(host, port).sync();
            if(!future.isSuccess()) {
                throw new NettyConnectionException("Netty连接失败", "1001");
            }
        } catch (InterruptedException e) {
            throw new NettyConnectionException("Netty连接失败", "1001");
        }
        channel = future.channel();
    }

    // 处理接收到的消息
    private void onMessageReceived(String message) {
        // 调用MessageListener中的onMessage()方法处理消息
        if (listener != null) {
            listener.onMessage(message);
        }
    }

    // 向服务器发送消息
    public void sendMessage(String message) {
        if (channel != null && channel.isActive()) {
            // 如果Channel已经连接且处于活跃状态，发送message消息
            channel.writeAndFlush(message);
        }
    }

    // 设置MessageListener
    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    // 关闭客户端连接，并释放资源
    public void close() {
        if (channel != null && channel.isOpen()) {
            // 关闭Channel
            channel.close();
        }
        if (group != null) {
            // 关闭EventLoopGroup，释放所有资源
            group.shutdownGracefully();
        }
    }
}
