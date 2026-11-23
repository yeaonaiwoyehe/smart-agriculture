package com.agri.cms.ai.knowledge;

import java.util.List;

/**
 * 常见问题标准话术（A-15 AG-05）。
 *
 * 每条记录对应一个机手常见问题的统一固定回答，
 * 保证同一问题在不同时间、不同机手处得到一致口径。
 *
 * @param id       ES 文档主键（同 faqId）
 * @param faqId    问答编号
 * @param category 问题分类
 * @param question 标准问题
 * @param keywords 检索关键词
 * @param answer   固定话术答案
 * @author agrinav
 */
public record FaqDocument(
        String id,
        String faqId,
        String category,
        String question,
        List<String> keywords,
        String answer
) {
}
