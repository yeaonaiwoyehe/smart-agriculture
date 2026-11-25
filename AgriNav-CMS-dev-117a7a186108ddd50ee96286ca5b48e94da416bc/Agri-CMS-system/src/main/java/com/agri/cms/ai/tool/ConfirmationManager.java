package com.agri.cms.ai.tool;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 写操作二次确认管理器（A-05）。
 *
 * 大模型请求写操作时，先把待执行内容登记并返回一个确认令牌，
 * 前端（D-03 确认弹窗）拿到用户明确确认后，再凭令牌执行。
 *
 * 令牌一次性使用，确认后立即移除，避免重复执行；
 * 待确认信息保存在内存中，服务重启后需重新发起。
 *
 * @author agrinav
 */
@Component
public class ConfirmationManager {

    private final Map<String, PendingConfirmation> pending = new ConcurrentHashMap<>();

    /**
     * 登记一个待确认操作，返回确认令牌。
     */
    public String register(PendingConfirmation confirmation) {
        String token = UUID.randomUUID().toString().replace("-", "");
        pending.put(token, confirmation);
        return token;
    }

    /**
     * 凭令牌取出并移除待确认操作（一次性）；令牌无效或已使用返回 null。
     */
    public PendingConfirmation consume(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return pending.remove(token);
    }
}
