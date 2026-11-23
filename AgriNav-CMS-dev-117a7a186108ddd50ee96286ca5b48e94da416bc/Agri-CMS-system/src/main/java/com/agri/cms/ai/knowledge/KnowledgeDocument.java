package com.agri.cms.ai.knowledge;

/**
 * 知识库文档切片（A-08 RAG）。
 *
 * 一条制度文档按长度切成若干片段后写入 Elasticsearch，
 * 检索时以片段为单位命中，再由大模型基于片段内容生成答案。
 *
 * @param id       ES 文档主键（docId + 切片序号）
 * @param docId    源文档编号，如 KB-001
 * @param title    文档标题
 * @param category 文档分类（作业规范/安全管理/结算制度/异常处理/平台使用）
 * @param source   来源名称，用于回答中的来源标注
 * @param chunkNo  切片序号（从 0 开始）
 * @param content  切片正文
 * @author agrinav
 */
public record KnowledgeDocument(
        String id,
        String docId,
        String title,
        String category,
        String source,
        Integer chunkNo,
        String content
) {
}
