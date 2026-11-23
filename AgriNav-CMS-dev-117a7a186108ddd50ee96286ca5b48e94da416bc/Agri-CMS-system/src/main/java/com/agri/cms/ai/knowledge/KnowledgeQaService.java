package com.agri.cms.ai.knowledge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 制度知识问答服务（A-07）。
 *
 * A-07 阶段 RAG 尚未建设，统一返回占位回答；
 * A-08 将在本服务内接入文档切片、向量检索与生成，
 * 保持方法签名不变即可平滑替换，路由层无需改动。
 *
 * @author agrinav
 */
@Service
public class KnowledgeQaService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQaService.class);

    /**
     * 回答制度知识类问题。
     *
     * @param question 用户问题
     * @return 知识问答结果；当前为占位回答
     */
    public KnowledgeAnswer answer(String question) {
        log.info("制度知识问答（A-08 前为占位）：{}", question);
        return KnowledgeAnswer.placeholder();
    }
}
