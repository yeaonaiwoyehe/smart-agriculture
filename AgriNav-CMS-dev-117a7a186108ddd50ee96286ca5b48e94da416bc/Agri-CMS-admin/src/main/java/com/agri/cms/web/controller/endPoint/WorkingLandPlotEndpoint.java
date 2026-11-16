package com.agri.cms.web.controller.endPoint;

import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.framework.websocket.SemaphoreUtils;
import com.agri.cms.framework.websocket.WebSocketUsers;
import com.agri.cms.system.domain.dto.WorkingLandPlotDTO;
import com.agri.cms.system.service.WorkingLandPlotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.concurrent.Semaphore;

/**
 * @author 姚锦伦
 */
@Component
@ServerEndpoint("/websocket/workingLandPlot")
public class WorkingLandPlotEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorkingLandPlotEndpoint.class);

    private static int socketMaxOnlineCount = 100;
    private static Semaphore socketSemaphore = new Semaphore(socketMaxOnlineCount);

    private static WorkingLandPlotService workingLandPlotService;

    private static ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

    @Autowired
    public void setWorkingLandPlotService(WorkingLandPlotService service) {
        WorkingLandPlotEndpoint.workingLandPlotService = service;
    }

    @OnOpen
    public void onOpen(Session session) throws Exception {
        boolean semaphoreFlag = false;
        semaphoreFlag = SemaphoreUtils.tryAcquire(socketSemaphore);
        if (!semaphoreFlag) {
            LOGGER.error("当前在线人数超过限制数- {}", socketMaxOnlineCount);
            WebSocketUsers.sendMessageToUserByText(session, "当前在线人数超过限制数：" + socketMaxOnlineCount);
            session.close();
        } else {
            WebSocketUsers.put(session.getId(), session);
            LOGGER.info("建立连接 - {}", session);
            WebSocketUsers.sendMessageToUserByText(session, "连接成功");
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            // 将接收到的JSON字符串转换为DTO对象
            WorkingLandPlotDTO workingLandPlotDTO = objectMapper.readValue(message, WorkingLandPlotDTO.class);

            // 调用服务处理请求
            AjaxResult result = workingLandPlotService.queryWorkingTrajectoryOfLandPlot(workingLandPlotDTO);

            // 将结果转换为JSON字符串并发送回客户端
            String response = objectMapper.writeValueAsString(result);
            WebSocketUsers.sendMessageToUserByText(session, response);

        } catch (Exception e) {
            LOGGER.error("处理消息时发生错误", e);
            try {
                WebSocketUsers.sendMessageToUserByText(session, "处理请求失败：" + e.getMessage());
            } catch (Exception ex) {
                LOGGER.error("发送错误消息失败", ex);
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        LOGGER.info("关闭连接 - {}", session);
        boolean removeFlag = WebSocketUsers.remove(session.getId());
        if (!removeFlag) {
            SemaphoreUtils.release(socketSemaphore);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        LOGGER.error("WebSocket发生错误", error);
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            LOGGER.error("关闭WebSocket连接失败", e);
        }
        WebSocketUsers.remove(session.getId());
        SemaphoreUtils.release(socketSemaphore);
    }
}