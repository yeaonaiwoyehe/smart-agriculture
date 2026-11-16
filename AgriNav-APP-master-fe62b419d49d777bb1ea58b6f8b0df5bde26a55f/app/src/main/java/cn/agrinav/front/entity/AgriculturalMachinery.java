package cn.agrinav.front.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lya
 */
@Getter
@Setter
public class AgriculturalMachinery implements Serializable {
    /**
     * 农机id
     */
    private String id;
    /**
     * 农机类型
     */
    private String typeName;
    /**
     * 联系方式
     */
    private String phoneNumber;
    /**
     * 农机状态
     */
    private int status;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新人
     */
    private String updateBy;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 是否配备运粮车
     */
    private boolean havingGrainCarrier;
    /**
     * 截止日期
     */
    private LocalDateTime endTime;
    /**
     * 图片
     */
    private List<String> image;
    /**
     * 位置
     */
    private List<String> location;
    /**
     * 农作物
     */
    private List<String> crop;
    /**
     * 农机找活名称
     */
    private String name;

    @Override
    public String toString() {
        return "AgriculturalMachinery{" +
                "id='" + id + '\'' +
                ", typeName='" + typeName + '\'' +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", status=" + status +
                ", createBy='" + createBy + '\'' +
                ", createTime=" + createTime +
                ", updateBy='" + updateBy + '\'' +
                ", updateTime=" + updateTime +
                ", havingGrainCarrier=" + havingGrainCarrier +
                ", endTime=" + endTime +
                ", image=" + image +
                ", location=" + location +
                ", crop=" + crop +
                '}';
    }
}