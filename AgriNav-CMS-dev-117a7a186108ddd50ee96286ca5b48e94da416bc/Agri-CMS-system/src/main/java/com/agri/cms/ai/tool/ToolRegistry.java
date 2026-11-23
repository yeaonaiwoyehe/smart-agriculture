package com.agri.cms.ai.tool;

import com.agri.cms.ai.model.ToolDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心（A-05）。
 *
 * Spring 启动时自动收集所有 {@link AgentTool} Bean，按名称登记，
 * 负责生成发送给大模型的 tools 定义，并提供按名查找。
 *
 * @author agrinav
 */
@Component
public class ToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolRegistry.class);

    /** 工具名 → 工具实例，保持注册顺序 */
    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AgentTool> registeredTools) {
        for (AgentTool tool : registeredTools) {
            if (tools.put(tool.name(), tool) != null) {
                log.warn("工具名重复，后注册的工具覆盖了同名工具：{}", tool.name());
            }
        }
        log.info("工具注册完成，共注册 {} 个工具：{}", tools.size(), tools.keySet());
    }

    /**
     * 按名查找工具，找不到返回 null。
     */
    public AgentTool getTool(String name) {
        return name == null ? null : tools.get(name);
    }

    /**
     * 生成全部工具的定义，用于请求体 tools 字段。
     */
    public List<ToolDefinition> listDefinitions() {
        return tools.values().stream()
                .map(this::toDefinition)
                .toList();
    }

    /**
     * 生成指定名称工具的定义，未知名称自动跳过。
     */
    public List<ToolDefinition> listDefinitions(List<String> toolNames) {
        if (toolNames == null || toolNames.isEmpty()) {
            return List.of();
        }
        return toolNames.stream()
                .map(tools::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toDefinition)
                .toList();
    }

    private ToolDefinition toDefinition(AgentTool tool) {
        ToolDefinition.FunctionDef functionDef = ToolDefinition.FunctionDef.of(
                tool.name(), tool.description(), tool.parametersSchema());
        return ToolDefinition.function(functionDef);
    }
}
