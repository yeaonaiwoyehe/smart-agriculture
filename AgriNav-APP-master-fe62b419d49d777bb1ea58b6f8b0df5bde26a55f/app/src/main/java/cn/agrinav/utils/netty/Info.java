package cn.agrinav.utils.netty;

/**
 * 登录时由后端传输
 */

public class Info {
    // 192.168.224.33  xiaomi13
    // 10.7.89.233     404
    // 192.168.1.147   501
    // 10.7.86.166     306
    // 172.29.90.115   宿舍
    // 192.168.1.144 7100
    // 模拟器访问宿主机使用 10.0.2.2；本类当前未被引用，实际连接地址以 R.string.jtt808_ip 为准
    static final String HOST = "10.0.2.2";
    static final int PORT = 7100;
    static final String defMsg = "未收到服务器端消息";
}
