package com.agri.cms.system.mapper;

import com.agri.cms.system.domain.ReviewRequest;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

/**
 * 复核申请 Mapper
 */
@DS("pg")
public interface ReviewRequestMapper extends BaseMapper<ReviewRequest> {

    /**
     * 写入一条系统通知（复核结果推机手）。只插入 sys_notification 真实存在的列。
     */
    @Insert("INSERT INTO sys_notification(id, type_name, receiver_id, relation_id, create_time, is_read) " +
            "VALUES (#{id}, 'SYSTEM', #{receiverId}, #{relationId}, now(), 0)")
    int insertNotification(@Param("id") String id,
                           @Param("receiverId") String receiverId,
                           @Param("relationId") String relationId);
}
