package com.agri.cms.ai.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板管理器（A-03）。
 *
 * 负责把系统提示词从散落在 Java 代码中的字符串，统一改为从 classpath 的
 * prompts 目录加载，并提供：
 * 1. 模板加载与缓存：首次读取后缓存，避免重复 IO；
 * 2. 变量占位符替换：支持 {{变量名}} 语法，用于注入当前日期、用户、地块等动态信息；
 * 3. 加载失败兜底与日志：模板缺失时不影响主流程，返回空串并记录错误。
 *
 * @author agrinav
 */
@Component
public class PromptTemplateManager {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateManager.class);

    /** 占位符格式：{{ 变量名 }}，变量名以字母或下划线开头 */
    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}");

    private final ResourceLoader resourceLoader;

    /** 模板内容缓存，key 为 classpath 路径 */
    private final Map<String, String> templateCache = new ConcurrentHashMap<>();

    public PromptTemplateManager(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 获取某个智能体的原始系统提示词（不做变量替换）。
     */
    public String getSystemPrompt(AgentType type) {
        return loadTemplate(type.getTemplatePath());
    }

    /**
     * 获取某个智能体的系统提示词，并把 {{变量}} 替换为 variables 中的值。
     * 未提供的变量统一替换为空字符串，避免最终提示词中残留占位符。
     */
    public String getSystemPrompt(AgentType type, Map<String, ?> variables) {
        String raw = getSystemPrompt(type);
        return render(raw, variables);
    }

    /**
     * 从 classpath 加载模板文本，读取失败返回空串并记录日志。
     */
    private String loadTemplate(String path) {
        return templateCache.computeIfAbsent(path, p -> {
            try {
                Resource resource = resourceLoader.getResource(ResourceLoader.CLASSPATH_URL_PREFIX + p);
                if (!resource.exists()) {
                    log.error("提示词模板不存在：{}", p);
                    return "";
                }
                String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                log.info("加载提示词模板成功：{}", p);
                return content;
            } catch (IOException e) {
                log.error("加载提示词模板失败：{}", p, e);
                return "";
            }
        });
    }

    /**
     * 替换模板中的占位符；变量缺失时替换为空串，并记录 debug 日志。
     */
    private String render(String template, Map<String, ?> variables) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables == null ? null : variables.get(key);
            if (value == null) {
                log.debug("提示词变量 [{}] 未提供，已替换为空字符串", key);
                matcher.appendReplacement(result, "");
            } else {
                // 用 Matcher.quoteReplacement 防止替换值中的 $、\ 被当作特殊字符
                matcher.appendReplacement(result, Matcher.quoteReplacement(value.toString()));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
