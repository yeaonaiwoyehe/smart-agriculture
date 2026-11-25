package com.agri.cms.ai.knowledge;

import java.util.List;

/**
 * 制度知识问答结果（A-07 定义，A-08 扩展真实检索）。
 *
 * @param content   回答内容
 * @param sources   命中的来源文档名称；占位阶段为空
 * @param retrieved 是否来自真实知识库检索；A-08 前为 false
 * @author agrinav
 */
public record KnowledgeAnswer(
        String content,
        List<String> sources,
        boolean retrieved
) {

    /**
     * 知识库尚未建成时的占位回答，来源列表为空。
     */
    public static KnowledgeAnswer placeholder() {
        return new KnowledgeAnswer(
                "制度知识库正在建设中（任务 A-08），当前为占位回复，暂无法基于制度文档给出准确口径；"
                        + "知识库上线后将在此回答并标注具体文档来源。",
                List.of(),
                false);
    }
}
