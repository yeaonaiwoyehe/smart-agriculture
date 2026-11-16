package cn.agrinav.front.entity;

import java.io.Serializable;

import lombok.Data;

/**
 * @author 86158
 */
@Data
public class MessageContent implements Serializable {
    private String phoneNumber;
    private String commitTime;
    private String type;
    private String id;
    private Integer isChecked;
    private String content;
    private String userName;
}
