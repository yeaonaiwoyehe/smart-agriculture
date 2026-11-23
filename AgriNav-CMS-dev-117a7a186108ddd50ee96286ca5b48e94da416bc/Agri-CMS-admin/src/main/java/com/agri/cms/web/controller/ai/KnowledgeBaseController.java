package com.agri.cms.web.controller.ai;

import com.agri.cms.ai.knowledge.KnowledgeDocument;
import com.agri.cms.ai.knowledge.KnowledgeIndexService;
import com.agri.cms.ai.knowledge.KnowledgeQaService;
import com.agri.cms.ai.knowledge.KnowledgeRetrievalService;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识库管理接口（A-08 RAG）。
 *
 * 提供知识库切片列表、统计、检索测试、问答测试与索引重建。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/knowledge")
@Tag(name = "知识库管理")
public class KnowledgeBaseController extends BaseController {

    private final KnowledgeIndexService indexService;
    private final KnowledgeRetrievalService retrievalService;
    private final KnowledgeQaService knowledgeQaService;

    public KnowledgeBaseController(KnowledgeIndexService indexService,
                                   KnowledgeRetrievalService retrievalService,
                                   KnowledgeQaService knowledgeQaService) {
        this.indexService = indexService;
        this.retrievalService = retrievalService;
        this.knowledgeQaService = knowledgeQaService;
    }

    @Operation(summary = "知识库统计")
    @GetMapping("/stats")
    public AjaxResult stats() throws Exception {
        return AjaxResult.success("切片总数", indexService.count());
    }

    @Operation(summary = "全部知识切片列表")
    @GetMapping("/list")
    public AjaxResult list() throws Exception {
        return success(indexService.listAll());
    }

    @Operation(summary = "检索测试（返回命中切片）")
    @GetMapping("/search")
    public AjaxResult search(@RequestParam String question,
                             @RequestParam(defaultValue = "4") Integer topK) {
        List<KnowledgeDocument> docs = retrievalService.retrieve(question, topK);
        return success(docs);
    }

    @Operation(summary = "知识问答测试（检索+生成）")
    @GetMapping("/ask")
    public AjaxResult ask(@RequestParam String question) {
        return success(knowledgeQaService.answer(question));
    }

    @Operation(summary = "重建知识库索引（管理员）")
    @PreAuthorize("@ss.hasRole('admin')")
    @PostMapping("/rebuild")
    public AjaxResult rebuild() throws Exception {
        long count = indexService.rebuild();
        return AjaxResult.success("知识库重建完成，共写入 " + count + " 个切片", count);
    }
}
