package com.agri.cms.ai.tool;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 工具参数校验器（A-05）。
 *
 * 在真正执行工具前，按工具声明的必填参数检查，返回缺失项；
 * 校验不通过时不执行，由上层决定追问或拒绝。
 *
 * @author agrinav
 */
@Component
public class ToolArgumentValidator {

    /**
     * 校验参数，返回缺失的必填参数名列表；为空表示校验通过。
     */
    public List<String> validate(AgentTool tool, Map<String, Object> arguments) {
        List<String> missing = new ArrayList<>();
        for (String required : tool.requiredParams()) {
            Object value = arguments == null ? null : arguments.get(required);
            if (value == null || value.toString().isBlank()) {
                missing.add(required);
            }
        }
        return missing;
    }
}
