package com.agri.cms.ai.audit;

import com.agri.cms.ai.audit.mapper.AgentAuditLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 审计日志异步写入器（A-06）。
 *
 * 独立成组件，保证 @Async 经过 Spring 代理生效；
 * 落库失败仅记录日志，不影响智能体主链路。
 *
 * @author agrinav
 */
@Component
public class AuditLogAsyncWriter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAsyncWriter.class);

    private final AgentAuditLogMapper auditLogMapper;

    public AuditLogAsyncWriter(AgentAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 异步插入审计记录，使用公共 taskExecutor 线程池。
     */
    @Async("taskExecutor")
    public void save(AgentAuditLog auditLog) {
        try {
            auditLogMapper.insert(auditLog);
            log.debug("审计记录已落表：id={} status={}", auditLog.getId(), auditLog.getStatus());
        } catch (Exception e) {
            log.error("审计记录落表失败：{}，指令原文：{}", e.getMessage(), auditLog.getMessage(), e);
        }
    }
}
