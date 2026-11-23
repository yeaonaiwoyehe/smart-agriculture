package com.agri.cms.system.service;

import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.FarmWork;
import com.agri.cms.system.domain.FarmWorkLandPlot;
import com.agri.cms.system.domain.WorkDispatch;
import com.agri.cms.system.domain.dto.BatchDispatchRequest;
import com.agri.cms.system.domain.dto.DispatchRequest;
import com.agri.cms.system.mapper.FarmWorkInfoMapper;
import com.agri.cms.system.mapper.FarmWorkLandPlotMapper;
import com.agri.cms.system.mapper.SysUserMapper;
import com.agri.cms.system.mapper.WorkDispatchMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 作业调度派单服务（A-19，MP-02）。
 *
 * 提供单选/批量派单、指定机手与时限、接单/完成/取消的状态流转，以及超时
 * 标记与完成率统计。派单为管理员操作；机手仅能对派给自己的单接单、完成。
 *
 * @author agrinav
 */
@Service
public class DispatchService {

    private static final Logger log = LoggerFactory.getLogger(DispatchService.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    /** 未结束、可被纳入超时判断的状态 */
    private static final List<String> OPEN_STATUSES =
            List.of(WorkDispatch.STATUS_DISPATCHED, WorkDispatch.STATUS_ACCEPTED);

    private final WorkDispatchMapper dispatchMapper;
    private final FarmWorkInfoMapper farmWorkMapper;
    private final FarmWorkLandPlotMapper workLandPlotMapper;
    private final SysUserMapper sysUserMapper;

    public DispatchService(WorkDispatchMapper dispatchMapper,
                           FarmWorkInfoMapper farmWorkMapper,
                           FarmWorkLandPlotMapper workLandPlotMapper,
                           SysUserMapper sysUserMapper) {
        this.dispatchMapper = dispatchMapper;
        this.farmWorkMapper = farmWorkMapper;
        this.workLandPlotMapper = workLandPlotMapper;
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 单选派单。
     */
    public WorkDispatch dispatchOne(DispatchRequest request, String operatorId) {
        if (request == null) {
            throw new ServiceException("派单参数不能为空");
        }
        if (request.getAssigneeId() == null) {
            throw new ServiceException("派单需要指定机手");
        }
        if (request.getFarmWorkId() == null || request.getFarmWorkId().isBlank()) {
            throw new ServiceException("派单需要指定农活");
        }
        return doDispatch(request.getFarmWorkId(), request.getAssigneeId(),
                request.getDeadline(), request.getRemark(), operatorId);
    }

    /**
     * 批量派单：多个农活派给同一机手，统一时限。
     */
    public List<WorkDispatch> dispatchBatch(BatchDispatchRequest request, String operatorId) {
        if (request == null || request.getFarmWorkIds() == null || request.getFarmWorkIds().isEmpty()) {
            throw new ServiceException("请至少选择一个农活进行批量派单");
        }
        if (request.getAssigneeId() == null) {
            throw new ServiceException("批量派单需要指定机手");
        }

        List<WorkDispatch> result = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (String farmWorkId : request.getFarmWorkIds()) {
            try {
                result.add(doDispatch(farmWorkId, request.getAssigneeId(),
                        request.getDeadline(), request.getRemark(), operatorId));
            } catch (ServiceException e) {
                // 批量中个别农活失败不影响其余，收集后统一提示
                failed.add(farmWorkId + "（" + e.getMessage() + "）");
            }
        }
        if (!failed.isEmpty()) {
            log.warn("[A-19] 批量派单部分失败：{}", failed);
            throw new ServiceException("以下农活派单失败：" + String.join("；", failed));
        }
        return result;
    }

    /**
     * 派单核心逻辑：校验农活、机手，防重复，落库。
     */
    private WorkDispatch doDispatch(String farmWorkId, Long assigneeId,
                                    LocalDateTime deadline, String remark, String operatorId) {
        // 校验农活存在且未结束
        FarmWork work = farmWorkMapper.selectById(farmWorkId);
        if (work == null) {
            throw new ServiceException("农活 " + farmWorkId + " 不存在");
        }
        if (work.getStatus() != null && work.getStatus() == 1) {
            throw new ServiceException("农活 " + farmWorkId + " 已结束，无法派单");
        }

        // 防重复：同一农活已有未结束的派单
        Long activeCount = dispatchMapper.selectCount(
                new LambdaQueryWrapper<WorkDispatch>()
                        .eq(WorkDispatch::getFarmWorkId, farmWorkId)
                        .in(WorkDispatch::getStatus, OPEN_STATUSES));
        if (activeCount != null && activeCount > 0) {
            throw new ServiceException("农活 " + farmWorkId + " 已有进行中的派单，请勿重复派单");
        }

        // 校验机手存在
        SysUser assignee = sysUserMapper.selectUserById(assigneeId);
        if (assignee == null) {
            throw new ServiceException("机手 " + assigneeId + " 不存在");
        }

        // 农活关联地块（地图定位用）
        FarmWorkLandPlot rel = workLandPlotMapper.selectOne(
                new LambdaQueryWrapper<FarmWorkLandPlot>()
                        .eq(FarmWorkLandPlot::getFarmWorkId, farmWorkId)
                        .last("limit 1"));
        Long plotId = rel == null ? null : rel.getLandPlotId();

        LocalDateTime now = LocalDateTime.now();
        WorkDispatch dispatch = new WorkDispatch();
        dispatch.setId(IdWorker.getId());
        dispatch.setDispatchNo(nextDispatchNo());
        dispatch.setFarmWorkId(farmWorkId);
        dispatch.setFarmWorkName(work.getName());
        dispatch.setPlotId(plotId);
        dispatch.setAssigneeId(String.valueOf(assigneeId));
        dispatch.setAssigneeName(assignee.getUserName());
        dispatch.setAssigneeNick(assignee.getNickName());
        dispatch.setDeadline(deadline);
        dispatch.setStatus(WorkDispatch.STATUS_DISPATCHED);
        dispatch.setDispatchBy(operatorId);
        dispatch.setDispatchTime(now);
        dispatch.setRemark(remark);
        dispatchMapper.insert(dispatch);

        log.info("[A-19] 派单成功：{} 农活={} 机手={} 时限={}",
                dispatch.getDispatchNo(), farmWorkId, assigneeId, deadline);
        return dispatch;
    }

    /**
     * 机手接单：仅被派机手本人可操作，待接单 -> 进行中。
     */
    public void accept(Long id, String userId) {
        WorkDispatch dispatch = mustGet(id);
        if (!WorkDispatch.STATUS_DISPATCHED.equals(dispatch.getStatus())) {
            throw new ServiceException("当前派单状态为「" + statusText(dispatch.getStatus()) + "」，无法接单");
        }
        if (!dispatch.getAssigneeId().equals(userId)) {
            throw new ServiceException("该派单不属于您，无法接单");
        }
        dispatch.setStatus(WorkDispatch.STATUS_ACCEPTED);
        dispatch.setAcceptTime(LocalDateTime.now());
        dispatchMapper.updateById(dispatch);
    }

    /**
     * 完成派单：仅被派机手本人可操作，进行中 -> 已完成。
     */
    public void complete(Long id, String userId) {
        WorkDispatch dispatch = mustGet(id);
        if (!WorkDispatch.STATUS_ACCEPTED.equals(dispatch.getStatus())) {
            throw new ServiceException("只有进行中的派单才能标记完成，当前状态为「"
                    + statusText(dispatch.getStatus()) + "」");
        }
        if (!dispatch.getAssigneeId().equals(userId)) {
            throw new ServiceException("该派单不属于您，无法操作");
        }
        dispatch.setStatus(WorkDispatch.STATUS_COMPLETED);
        dispatch.setCompleteTime(LocalDateTime.now());
        dispatchMapper.updateById(dispatch);
    }

    /**
     * 取消派单（管理员），已完成的不可取消。
     */
    public void cancel(Long id, String operatorId) {
        WorkDispatch dispatch = mustGet(id);
        if (WorkDispatch.STATUS_COMPLETED.equals(dispatch.getStatus())) {
            throw new ServiceException("已完成的派单无法取消");
        }
        if (WorkDispatch.STATUS_CANCELLED.equals(dispatch.getStatus())) {
            throw new ServiceException("该派单已取消，无需重复操作");
        }
        dispatch.setStatus(WorkDispatch.STATUS_CANCELLED);
        dispatchMapper.updateById(dispatch);
        log.info("[A-19] 管理员 {} 取消派单 {}", operatorId, dispatch.getDispatchNo());
    }

    /**
     * 超时标记：把所有未完成且已过时限的派单更新为 TIMEOUT，返回处理条数。
     */
    public int markTimeout() {
        List<WorkDispatch> overdue = dispatchMapper.selectList(
                new LambdaQueryWrapper<WorkDispatch>()
                        .in(WorkDispatch::getStatus, OPEN_STATUSES)
                        .isNotNull(WorkDispatch::getDeadline)
                        .lt(WorkDispatch::getDeadline, LocalDateTime.now()));
        for (WorkDispatch dispatch : overdue) {
            dispatch.setStatus(WorkDispatch.STATUS_TIMEOUT);
            dispatchMapper.updateById(dispatch);
        }
        if (!overdue.isEmpty()) {
            log.info("[A-19] 标记超时派单 {} 条", overdue.size());
        }
        return overdue.size();
    }

    /**
     * 列表查询：先刷新超时状态，再按状态/机手/农活筛选，按派单时间倒序。
     */
    public List<WorkDispatch> selectList(WorkDispatch query) {
        markTimeout();
        LambdaQueryWrapper<WorkDispatch> wrapper = new LambdaQueryWrapper<>();
        if (query != null) {
            if (query.getStatus() != null && !query.getStatus().isBlank()) {
                wrapper.eq(WorkDispatch::getStatus, query.getStatus());
            }
            if (query.getAssigneeId() != null && !query.getAssigneeId().isBlank()) {
                wrapper.eq(WorkDispatch::getAssigneeId, query.getAssigneeId());
            }
            if (query.getFarmWorkId() != null && !query.getFarmWorkId().isBlank()) {
                wrapper.eq(WorkDispatch::getFarmWorkId, query.getFarmWorkId());
            }
        }
        wrapper.orderByDesc(WorkDispatch::getDispatchTime);
        return dispatchMapper.selectList(wrapper);
    }

    /**
     * 详情查询。
     */
    public WorkDispatch selectById(Long id) {
        return mustGet(id);
    }

    /**
     * 完成率统计：总数、各状态数量、完成率与超时率。
     */
    public Map<String, Object> completionStats() {
        markTimeout();
        List<WorkDispatch> all = dispatchMapper.selectList(null);

        int total = all.size();
        long completed = countByStatus(all, WorkDispatch.STATUS_COMPLETED);
        long timeout = countByStatus(all, WorkDispatch.STATUS_TIMEOUT);
        long accepted = countByStatus(all, WorkDispatch.STATUS_ACCEPTED);
        long dispatched = countByStatus(all, WorkDispatch.STATUS_DISPATCHED);
        long cancelled = countByStatus(all, WorkDispatch.STATUS_CANCELLED);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("accepted", accepted);
        stats.put("dispatched", dispatched);
        stats.put("timeout", timeout);
        stats.put("cancelled", cancelled);
        // 完成率按全部派单计，无单时为 0
        stats.put("completionRate", total == 0 ? 0 : Math.round(completed * 10000.0 / total) / 100.0);
        stats.put("timeoutRate", total == 0 ? 0 : Math.round(timeout * 10000.0 / total) / 100.0);
        return stats;
    }

    private long countByStatus(List<WorkDispatch> all, String status) {
        return all.stream().filter(d -> status.equals(d.getStatus())).count();
    }

    private WorkDispatch mustGet(Long id) {
        if (id == null) {
            throw new ServiceException("派单 ID 不能为空");
        }
        WorkDispatch dispatch = dispatchMapper.selectById(id);
        if (dispatch == null) {
            throw new ServiceException("未找到派单记录 " + id);
        }
        return dispatch;
    }

    private String nextDispatchNo() {
        String date = LocalDate.now().format(DATE_FMT);
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 4).toUpperCase();
        return "DP-" + date + "-" + random;
    }

    private String statusText(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case WorkDispatch.STATUS_DISPATCHED -> "待接单";
            case WorkDispatch.STATUS_ACCEPTED -> "进行中";
            case WorkDispatch.STATUS_COMPLETED -> "已完成";
            case WorkDispatch.STATUS_TIMEOUT -> "已超时";
            case WorkDispatch.STATUS_CANCELLED -> "已取消";
            default -> status;
        };
    }
}
