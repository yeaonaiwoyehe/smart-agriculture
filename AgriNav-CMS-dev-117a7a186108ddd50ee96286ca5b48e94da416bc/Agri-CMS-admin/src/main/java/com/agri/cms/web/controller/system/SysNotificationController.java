package com.agri.cms.web.controller.system;

import com.agri.cms.common.annotation.Anonymous;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * @author : simon
 * @description : 通知管理
 * @last : 2024-11-22 15:47
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-22 15:47
 */
@RestController
@RequestMapping("/system/notification")
@PreAuthorize("ss.hasRole('admin')")
@Tag(name = "通知管理")
public class SysNotificationController extends BaseController {


    @Resource
    private NotificationService notificationService;


    @PutMapping("/{id}")
    public AjaxResult readNotification(@PathVariable String id){
        return success(notificationService.read(id));
    }

    @GetMapping
    public AjaxResult getNotificationList(){
        return success(notificationService.list(getUsername()));
    }


    @GetMapping("/unread")
    public AjaxResult getUnreadNotificationList(){
        return success(notificationService.listUnread(getUsername()));
    }
    @PutMapping
    public AjaxResult readAllNotification(){
        return success(notificationService.readAll(getUsername()));
    }

    @DeleteMapping("/{id}")
    public AjaxResult deleteNotification(@PathVariable String id){
        return success(notificationService.deleteById(getUsername(),id));
    }

    @DeleteMapping
    public AjaxResult clearNotification(){
        return success(notificationService.clear(getUsername()));
    }

}
