package com.agri.cms.web.controller.ai;

import com.agri.cms.ai.audit.AgentAuditLog;
import com.agri.cms.ai.audit.mapper.AgentAuditLogMapper;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.page.TableDataInfo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能体指令审计查询接口（A-06）。
 *
 * 提供分页列表与详情查询，支持按用户、意图、状态、端与时间范围过滤，
 * 实现智能体指令的可查、可追溯。查询需登录，不额外限制菜单权限。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/agent/audit")
@Tag(name = "智能体指令审计")
public class AgentAuditController extends BaseController {

    private final AgentAuditLogMapper auditLogMapper;

    public AgentAuditController(AgentAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Operation(summary = "分页查询审计记录")
    @GetMapping("/list")
    public TableDataInfo list(AgentAuditLog query,
                              @RequestParam(required = false) String beginTime,
                              @RequestParam(required = false) String endTime) {
        QueryWrapper<AgentAuditLog> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getUserName() != null, "user_name", query.getUserName());
        wrapper.eq(query.getIntent() != null, "intent", query.getIntent());
        wrapper.eq(query.getStatus() != null, "status", query.getStatus());
        wrapper.eq(query.getSide() != null, "side", query.getSide());
        wrapper.ge(beginTime != null, "create_time", beginTime);
        wrapper.le(endTime != null, "create_time", endTime);
        wrapper.orderByDesc("create_time");

        startPage();
        List<AgentAuditLog> list = auditLogMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Operation(summary = "查询审计记录详情")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(auditLogMapper.selectById(id));
    }
}
