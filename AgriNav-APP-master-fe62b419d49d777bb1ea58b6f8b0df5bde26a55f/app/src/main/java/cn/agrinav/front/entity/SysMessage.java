package cn.agrinav.front.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @author 86158
 */
@Data
public class SysMessage implements Serializable {
    private String id;
    // 消息标题
    private String title;
    // 消息内容
    private MessageContent content;
    // 接收id
    private String receiveId;
    // 接受类型
    private String type;
    // 创建时间
    private String createTime;
    // 读取时间
    private String readTime;
    // 是否已读
    private Integer isRead;

}