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
public class FarmWorkDTO {
    /**
     * 农活id
     */
    private String id;
    /**
     * 农活名称
     */
    private String name;
    /**
     * 地块总面积
     */
    private Double area;
    /**
     * 截止时间
     */
    private LocalDateTime endTime;
    /**
     * 手机号
     */
    private String phoneNumber;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 是够按照创建时间降序排序
     */
    private Boolean orderByCreateTimeDesc;
    /**
     * 是否是我发布的
     */
    private Boolean myFarmWork;
    /**
     * 地块id
     */
    private List<Long> landPlotId;
    /**
     * 最小面积
     */
    private Double minArea;
    /**
     * 最大面积
     */
    private Double maxArea;
    /**
     * 作物id
     */
    private List<Long> cropId;
    /**
     * 作业类型id
     */

    /**
     * 作业类型id
     */
    private List<Long> workTypeId;
    /**
     * 地块位置
     */
    private List<String> location;
}
