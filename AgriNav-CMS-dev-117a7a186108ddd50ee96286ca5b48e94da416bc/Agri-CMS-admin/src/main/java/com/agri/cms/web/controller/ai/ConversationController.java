package com.agri.cms.web.controller.ai;

import com.agri.cms.ai.conversation.AgentConversation;
import com.agri.cms.ai.conversation.AgentConversationMessage;
import com.agri.cms.ai.conversation.mapper.AgentConversationMapper;
import com.agri.cms.ai.conversation.mapper.AgentConversationMessageMapper;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体会话留存查询接口（A-10）。
 *
 * 提供会话分页列表与会话详情（含全部消息，按时间排序），
 * 支持按用户、所属端与时间范围过滤，支撑每条问答点开核对与业务单跳转。
 * 查询需登录，不额外限制菜单权限。
 *
 * @author agrinav
 */
@RestController
@RequestMapping("/agent/conversation")
@Tag(name = "智能体会话留存")
public class ConversationController extends BaseController {

    private final AgentConversationMapper conversationMapper;
    private final AgentConversationMessageMapper messageMapper;

    public ConversationController(AgentConversationMapper conversationMapper,
                                  AgentConversationMessageMapper messageMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
    }

    @Operation(summary = "分页查询会话列表")
    @GetMapping("/list")
    public TableDataInfo list(AgentConversation query,
                              @RequestParam(required = false) String beginTime,
                              @RequestParam(required = false) String endTime) {
        QueryWrapper<AgentConversation> wrapper = new QueryWrapper<>();
        wrapper.eq(query.getUserName() != null, "user_name", query.getUserName());
        wrapper.like(query.getNickName() != null, "nick_name", query.getNickName());
        wrapper.eq(query.getSide() != null, "side", query.getSide());
        wrapper.ge(beginTime != null, "create_time", beginTime);
        wrapper.le(endTime != null, "create_time", endTime);
        wrapper.orderByDesc("last_message_time");

        startPage();
        List<AgentConversation> list = conversationMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Operation(summary = "查询会话详情（含全部消息）")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        AgentConversation conversation = conversationMapper.selectById(id);
        if (conversation == null) {
            return AjaxResult.error("会话不存在或已被清理");
        }

        QueryWrapper<AgentConversationMessage> wrapper = new QueryWrapper<>();
        wrapper.eq("conversation_id", id).orderByAsc("create_time").orderByAsc("id");
        List<AgentConversationMessage> messages = messageMapper.selectList(wrapper);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("conversation", conversation);
        data.put("messages", messages);
        return AjaxResult.success("操作成功", data);
    }
}
