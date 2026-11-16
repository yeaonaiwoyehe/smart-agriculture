package cn.agrinav.trajectory.entity.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author 11230
 * 作物类型的实体类
 */
@Setter
@Getter
public class CornEntity {
    private Long id;
    private String cropName;
    private Boolean isDeleted;
    private String createTime;
    private String  updateTime;
}
