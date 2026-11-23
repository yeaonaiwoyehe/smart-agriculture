package com.agri.cms.ai.agent;

import com.agri.cms.ai.agent.mapper.AgentAlertMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 异常工单服务（A-14，AG-04 异常上报）。
 *
 * 负责异常工单的提交、归属校验、查询与管理端处理：
 * 校验描述与异常类型 → 生成工单编号/处理人/处理时限 → 落库；
 * 普通机手只能查看本人工单，管理员可查看与处理全部工单。
 *
 * @author agrinav
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    /** 默认处理时限（小时）与默认处理人 */
    private static final long HANDLE_DEADLINE_HOURS = 24L;
    private static final String DEFAULT_HANDLER = "片区生产队长";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final List<String> VALID_TYPES = List.of(
            AgentAlert.TYPE_EQUIPMENT, AgentAlert.TYPE_LOCATION, AgentAlert.TYPE_DISPUTE,
            AgentAlert.TYPE_TRAPPED, AgentAlert.TYPE_OBSTACLE, AgentAlert.TYPE_OTHER);

    private final AgentAlertMapper alertMapper;

    public AlertService(AgentAlertMapper alertMapper) {
        this.alertMapper = alertMapper;
    }

    /**
     * 提交异常工单。
     */
    public AlertSubmitResult submit(String alertType, String description, String workOrderId,
                                    Long plotId, Double longitude, Double latitude,
                                    LocalDateTime occurTime, String userId,
                                    String userName, String nickName) {
        if (description == null || description.isBlank()) {
            return AlertSubmitResult.fail("请先描述一下具体的异常情况（现象、发生时间、地点），我再帮您生成工单。");
        }
        if (userId == null || userId.isBlank()) {
            return AlertSubmitResult.fail("登录状态已失效，请重新登录后再上报。");
        }

        // 规范化异常类型：不在内置列表则归为“其他”
        String type = VALID_TYPES.contains(alertType) ? alertType : AgentAlert.TYPE_OTHER;

        LocalDateTime now = LocalDateTime.now();
        AgentAlert alert = new AgentAlert();
        alert.setId(IdWorker.getId());
        alert.setAlertNo(nextAlertNo());
        alert.setAlertType(type);
        alert.setDescription(description.trim());
        alert.setWorkOrderId(blankToNull(workOrderId));
        alert.setPlotId(plotId);
        alert.setLongitude(longitude);
        alert.setLatitude(latitude);
        alert.setOccurTime(occurTime);
        alert.setReporterId(userId);
        alert.setReporterName(userName);
        alert.setReporterNick(nickName);
        alert.setHandlerName(DEFAULT_HANDLER);
        alert.setDeadline(now.plusHours(HANDLE_DEADLINE_HOURS));
        alert.setStatus(AgentAlert.STATUS_PENDING);
        alert.setCreateTime(now);
        alert.setUpdateTime(now);
        alertMapper.insert(alert);

        log.info("[A-14] 用户 {} 提交异常工单 {}，类型 {}，处理时限 {}",
                userId, alert.getAlertNo(), type, alert.getDeadline());
        return new AlertSubmitResult(true, "异常工单已提交，" + DEFAULT_HANDLER
                + "将在 24 小时内联系处理，请保持电话畅通。",
                alert.getAlertNo(), DEFAULT_HANDLER, alert.getDeadline());
    }

    /**
     * 查询工单详情；普通机手仅能查看本人工单，管理员可查看全部。
     */
    public AgentAlert getForUser(Long id, Long userId, boolean isAdmin) {
        AgentAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            return null;
        }
        if (isAdmin) {
            return alert;
        }
        return alert.getReporterId() != null
                && alert.getReporterId().equals(String.valueOf(userId)) ? alert : null;
    }

    /**
     * 按工单编号查询（供工具层，含归属判断）。
     */
    public AgentAlert getByNoForUser(String alertNo, Long userId, boolean isAdmin) {
        List<AgentAlert> list = alertMapper.selectList(new LambdaQueryWrapper<AgentAlert>()
                .eq(AgentAlert::getAlertNo, alertNo));
        if (list.isEmpty()) {
            return null;
        }
        AgentAlert alert = list.get(0);
        if (isAdmin) {
            return alert;
        }
        return alert.getReporterId() != null
                && alert.getReporterId().equals(String.valueOf(userId)) ? alert : null;
    }

    /**
     * 查询我的异常工单，可按状态过滤。
     */
    public List<AgentAlert> listMyAlerts(String userId, String status) {
        LambdaQueryWrapper<AgentAlert> wrapper = new LambdaQueryWrapper<AgentAlert>()
                .eq(AgentAlert::getReporterId, userId)
                .eq(status != null && !status.isBlank(), AgentAlert::getStatus, status)
                .orderByDesc(AgentAlert::getCreateTime);
        return alertMapper.selectList(wrapper);
    }

    /**
     * 查询全部异常工单（管理员），可按状态过滤。
     */
    public List<AgentAlert> listAll(String status) {
        LambdaQueryWrapper<AgentAlert> wrapper = new LambdaQueryWrapper<AgentAlert>()
                .eq(status != null && !status.isBlank(), AgentAlert::getStatus, status)
                .orderByDesc(AgentAlert::getCreateTime);
        return alertMapper.selectList(wrapper);
    }

    /**
     * 处理异常工单（管理员）：更新状态、处理结果与处理时间。
     */
    public AgentAlert handle(Long id, String newStatus, String handleResult,
                             String handlerId, String handlerName) {
        AgentAlert alert = alertMapper.selectById(id);
        if (alert == null) {
            return null;
        }
        String status = normalizeStatus(newStatus);
        alert.setStatus(status);
        if (handleResult != null && !handleResult.isBlank()) {
            alert.setHandleResult(handleResult.trim());
        }
        alert.setHandleBy(handlerId);
        if (handlerName != null) {
            alert.setHandlerName(handlerName);
        }
        alert.setHandleTime(LocalDateTime.now());
        alert.setUpdateTime(LocalDateTime.now());
        alertMapper.updateById(alert);
        log.info("[A-14] 工单 {} 被 {} 处理，状态 {}", alert.getAlertNo(), handlerId, status);
        return alert;
    }

    private String normalizeStatus(String status) {
        return switch (status == null ? "" : status) {
            case AgentAlert.STATUS_PENDING, AgentAlert.STATUS_PROCESSING,
                 AgentAlert.STATUS_RESOLVED, AgentAlert.STATUS_CLOSED -> status;
            default -> AgentAlert.STATUS_RESOLVED;
        };
    }

    private String nextAlertNo() {
        String date = LocalDate.now().format(DATE_FMT);
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 4).toUpperCase();
        return "AT-" + date + "-" + random;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
