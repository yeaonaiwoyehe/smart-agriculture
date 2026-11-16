package cn.agrinav.front.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * @author 姚锦伦
 */
@Setter
@Getter
public class FarmWork implements Serializable {
    /**
     * 农事作业id
     */
    private String id;
    /**
     * 作业名称
     */
    private String name;
    /**
     * 作业亩数
     */
    private Double area;
    /**
     * 期限时间
     */
    private LocalDateTime endTime;
    /**
     * 联系方式
     */
    private String phoneNumber;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 作物id
     */
    private List<Long> cropId;
    /**
     * 作业类型id
     */
    private List<Long> workTypeId;
    /**
     * 地块id
     */
    private List<Long> landPlotId;
    /**
     * 作物名
     */
    private List<String> cropName;
    /**
     * 作业类型名
     */
    private List<String> workTypeName;
    /**
     * 地块名
     */
    private List<String> landPlotName;
    /**
     * 地块图片
     */
    private List<String> landPlotImage;

    private List<String> landAddress;
}
