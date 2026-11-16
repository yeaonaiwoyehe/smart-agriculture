package cn.agrinav.front.entity.dto;




import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * @author lya
 */
@Getter
@Setter
public class AgriculturalMachineryDTO {
    private String id;
    /**
     * 农机任务名称
     */
    private String name;
    /**
     * 农机型号
     */
    private String typeId;
    /**
     * 可作物类型
     */
    private List<Long> cropId;
    /**
     * 是够配备运粮车
     */
    private Boolean havingGrainCarrier;
    /**
     * 作业区域
     */
    private List<String> location;
    /**
     * 联系方式
     */
    private String phoneNumber;
    /**
     * 农机图片
     */
    private List<String> image;
    /**
     * 有效时间
     */
    private LocalDateTime endTime;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 农机找活状态
     */
    private Integer status;
    private Boolean orderByCreateTimeDesc;
    private Boolean myAgriculturalMachinery;


    @Override
    public String toString() {
        return "AgriculturalMachineryDTO{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", typeId='" + typeId + '\'' +
                ", cropId=" + cropId +
                ", havingGrainCarrier=" + havingGrainCarrier +
                ", location=" + location +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", image=" + image +
                ", endTime=" + endTime +
                ", createTime=" + createTime +
                ", status=" + status +
                ", orderByCreateTimeDesc=" + orderByCreateTimeDesc +
                ", myAgriculturalMachinery=" + myAgriculturalMachinery +
                '}';
    }
}
