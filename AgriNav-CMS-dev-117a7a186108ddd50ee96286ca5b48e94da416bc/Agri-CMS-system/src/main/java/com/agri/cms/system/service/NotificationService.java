package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.model.Notification;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-21 09:42
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-21 09:42
 */
public interface NotificationService extends IService<Notification<?>> {

    /**
     * 读取通知
     * @param id 通知id
     * @return String
     * @author simon
     * @since 2024/11/30
     */
    Boolean read(String id);

    /**
     *  获取通知列表
     * @param userName 用户名
     * @return List<Notification>
     * @author simon
     * @since 2024/11/30
     */
    List<Notification> list(String userName);

    Notification transfer(Notification<?> notification) throws ClassNotFoundException;

    Boolean readAll(String username);


    Boolean deleteById(String username, String id);

    Boolean clear(String username);

    Set<Notification> listUnread(String username);
}
