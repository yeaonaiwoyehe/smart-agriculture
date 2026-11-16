package com.agri.cms.system.mapper;

import com.agri.cms.common.core.domain.model.Notification;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-30 17:30
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-30 17:30
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification<?>> {
    /**
     * 插入通知
     * @param notification 通知
     * @return
     */
    @Override
    @Insert("INSERT INTO sys_notification (" +
            "id, type_name, relation_db, relation_name, receiver_id, create_time, is_read,relation_id,content" +
            ") VALUES (" +
            "#{id}, #{type.typeName}, #{type.relationDB}, #{type.relationName}, #{receiverId}, " +
            "#{createTime}, #{isRead},#{relationIdentity},#{type.defaultContent}"+
            ")")
    int insert(Notification notification);

    @Select("select * from ${type.relationName} where id = #{relationIdentity}")
    Map<String,Object> transfer(Notification<?> notification);
}
