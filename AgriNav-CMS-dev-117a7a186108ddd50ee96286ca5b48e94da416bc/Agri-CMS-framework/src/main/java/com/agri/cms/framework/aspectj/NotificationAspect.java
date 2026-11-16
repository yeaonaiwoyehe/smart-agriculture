package com.agri.cms.framework.aspectj;

import com.agri.cms.common.annotation.AsyncNotification;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.model.Notification;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.framework.manager.AsyncManager;
import com.agri.cms.framework.manager.factory.AsyncFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * @author : simon
 * @description :
 * @last : 2024-11-30 20:47
 * Copyright (c) 2024. 保留所有权利。
 * @since : 2024-11-30 20:47
 */
@Component
@Aspect
public class NotificationAspect {
    private static final Logger log = LoggerFactory.getLogger(NotificationAspect.class);
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<Long>("Cost Time");

    @After(value = "@annotation(asyncNotificationAnno)")
    public void doBefore(JoinPoint joinPoint, AsyncNotification asyncNotificationAnno) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            EvaluationContext context = new StandardEvaluationContext();
            
            // 将方法参数加入到表达式上下文
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
            var parser = new SpelExpressionParser();

            // 解析接收人ID
            String[] receiverIds = parseReceiverIds(asyncNotificationAnno.receiver(), context, parser);
            
            // 解析relationIdentity
            String relationIdentity = parseRelationIdentity(asyncNotificationAnno.relationIdentity(), context, parser);

            // 创建通知
            for (String receiverId : receiverIds) {
                createNotification(receiverId, asyncNotificationAnno, relationIdentity);
            }
        } catch (Exception e) {
            log.error("创建通知异常:", e);
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

    private String[] parseReceiverIds(String receiverIdsExpr, EvaluationContext context, SpelExpressionParser parser) {
        if (StringUtils.isEmpty(receiverIdsExpr)) {
            return new String[0];
        }

        Expression expression = parser.parseExpression(receiverIdsExpr);
        Object result = expression.getValue(context);
        
        if (result instanceof String) {
            return new String[]{(String) result};
        } else if (result instanceof String[]) {
            return (String[]) result;
        } else if (result instanceof Collection) {
            return ((Collection<?>) result).stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
        }
        return new String[0];
    }

    private String parseRelationIdentity(String relationIdentityExpr, EvaluationContext context, SpelExpressionParser parser) {
        if (StringUtils.isEmpty(relationIdentityExpr)) {
            return null;
        }
        Expression expression = parser.parseExpression(relationIdentityExpr);
        Object result = expression.getValue(context);
        return result != null ? result.toString() : null;
    }

    private void createNotification(String receiverId, AsyncNotification asyncNotificationAnno, String relationIdentity) {
        var notification = new Notification<>();
        notification.setType(asyncNotificationAnno.type());
        notification.setCreateTime(LocalDateTime.now());
        notification.setIsRead(Constants.NOT_READ);
        notification.setReceiverId(receiverId);
        notification.setRelationIdentity(relationIdentity);
        // 异步保存通知
        AsyncManager.me().execute(AsyncFactory.recordNotification(notification));
    }
}
