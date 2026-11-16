package com.agri.cms.framework.aspectj;


import com.agri.cms.common.annotation.DAU;
import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.common.utils.DateUtils;
import jakarta.annotation.Resource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author 李鸿麟
 * DAU注解切面
 */
@Aspect
@Component
public class DAUAspect {
    @Resource
    private RedisCache redisCache;

    @AfterReturning(pointcut = "@annotation(dau)", returning = "map")
    public void doAfter(JoinPoint point, DAU dau, Map<String,String> map) {
        if (map.isEmpty()) {
            return;
        }
        Object userId = getUserId(point, dau.value());
        String dauKey = CacheConstants.DAU_KEY_PREFIX + DateUtils.getDate();
        redisCache.pfadd(dauKey, userId.toString());
        redisCache.expire(dauKey, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);
    }


    /**
     * 解析SpEL表达式获取用户ID
     */
    private Object getUserId(JoinPoint point, String spEl) {
        if (spEl.isEmpty()) {
            return null;
        }

        try {
            ExpressionParser parser = new SpelExpressionParser();
            StandardEvaluationContext context = new StandardEvaluationContext();

            // 获取方法参数
            Object[] args = point.getArgs();
            // 获取方法参数名
            String[] paramNames = Arrays.stream(point.getArgs())
                    .map(arg -> "p" + Arrays.asList(args).indexOf(arg))
                    .toArray(String[]::new);

            // 将参数放入上下文
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            Expression expression = parser.parseExpression(spEl);
            return expression.getValue(context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
