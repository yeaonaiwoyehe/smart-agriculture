package com.agri.cms.ai.model;

/**
 * 一条对话消息，对应 OpenAI/DeepSeek 接口中的 message 对象。
 *
 * role 取值：system（设定角色与规则）、user（用户发言）、assistant（模型回复）。
 *
 * @param role     角色
 * @param content  文本内容
 * @author agrinav
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
