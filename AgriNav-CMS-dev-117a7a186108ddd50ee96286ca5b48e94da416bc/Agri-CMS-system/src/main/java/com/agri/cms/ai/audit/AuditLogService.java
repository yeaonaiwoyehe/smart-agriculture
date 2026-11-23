package com.agri.cms.ai.audit;

import com.agri.cms.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 智能体指令审计服务（A-06）。
 *
 * 对外提供统一的记录入口：在主线程先捕获当前登录用户与时间
 * （异步线程无法访问 SecurityContext），再交由异步写入器落表，
 * 因此审计不增加用户可感知的等待时间。
 *
 * @author agrinav
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogAsyncWriter asyncWriter;

    public AuditLogService(AuditLogAsyncWriter asyncWriter) {
        this.asyncWriter = asyncWriter;
    }

    /**
     * 记录一次智能体调用。埋点方只需填好业务字段（指令、意图、工具、状态、耗时等），
     * 用户与时间由本方法统一补齐。
     */
    public void record(AgentAuditLog auditLog) {
        fillCurrentUser(auditLog);
        if (auditLog.getCreateTime() == null) {
            auditLog.setCreateTime(LocalDateTime.now());
        }
        asyncWriter.save(auditLog);
    }

    /**
     * 捕获当前登录用户信息；未登录场景用匿名兜底，不阻断审计。
     */
    private void fillCurrentUser(AgentAuditLog auditLog) {
        try {
            var loginUser = SecurityUtils.getLoginUser();
            auditLog.setUserId(String.valueOf(loginUser.getUserId()));
            auditLog.setUserName(loginUser.getUsername());
            auditLog.setNickName(loginUser.getUser().getNickName());
        } catch (Exception e) {
            log.debug("审计时未获取到登录用户，按匿名处理：{}", e.getMessage());
            auditLog.setUserName("anonymous");
        }
    }
}
