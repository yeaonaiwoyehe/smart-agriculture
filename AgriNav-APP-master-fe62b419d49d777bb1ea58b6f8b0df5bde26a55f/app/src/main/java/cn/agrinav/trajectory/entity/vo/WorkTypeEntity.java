package cn.agrinav.trajectory.entity.vo;

import lombok.Data;

/**
 * @author 11230
 * 工作类型的实体类
 */
@Data
public class WorkTypeEntity {
    private Long id;
    private String workName;
    private Boolean isDeleted;
    private String createTime;
    private String updateTime;
}
