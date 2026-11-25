package com.agri.cms.ai.agent;

import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.system.domain.AgriculturalMachinery;
import com.agri.cms.system.domain.AgriculturalMachineryLocation;
import com.agri.cms.system.domain.FarmWork;
import com.agri.cms.system.domain.FarmWorkLandPlot;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.WorkDispatch;
import com.agri.cms.system.mapper.AgriculturalMachineryInfoMapper;
import com.agri.cms.system.mapper.AgriculturalMachineryLocationMapper;
import com.agri.cms.system.mapper.FarmWorkInfoMapper;
import com.agri.cms.system.mapper.FarmWorkLandPlotMapper;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.mapper.SysUserMapper;
import com.agri.cms.system.mapper.WorkDispatchMapper;
import com.agri.cms.system.service.DispatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 调度建议服务（A-16 MA-01）。
 *
 * 基于待派农活、机手空闲状态与历史派单表现、可用农机运力，组装可解释的
 * 派单建议。只做分析与建议，不写入派单数据；对无法真实获取的关联与位置
 * 信息，在数据缺口里如实说明，不编造距离或归属。
 *
 * @author agrinav
 */
@Service
public class DispatchSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(DispatchSuggestionService.class);

    private static final List<String> OPEN_STATUSES =
            List.of(WorkDispatch.STATUS_DISPATCHED, WorkDispatch.STATUS_ACCEPTED);

    private final FarmWorkInfoMapper farmWorkMapper;
    private final FarmWorkLandPlotMapper workLandPlotMapper;
    private final LandPlotMapper landPlotMapper;
    private final WorkDispatchMapper dispatchMapper;
    private final SysUserMapper sysUserMapper;
    private final AgriculturalMachineryInfoMapper machineryMapper;
    private final AgriculturalMachineryLocationMapper machineryLocationMapper;
    private final DispatchService dispatchService;

    public DispatchSuggestionService(FarmWorkInfoMapper farmWorkMapper,
                                     FarmWorkLandPlotMapper workLandPlotMapper,
                                     LandPlotMapper landPlotMapper,
                                     WorkDispatchMapper dispatchMapper,
                                     SysUserMapper sysUserMapper,
                                     AgriculturalMachineryInfoMapper machineryMapper,
                                     AgriculturalMachineryLocationMapper machineryLocationMapper,
                                     DispatchService dispatchService) {
        this.farmWorkMapper = farmWorkMapper;
        this.workLandPlotMapper = workLandPlotMapper;
        this.landPlotMapper = landPlotMapper;
        this.dispatchMapper = dispatchMapper;
        this.sysUserMapper = sysUserMapper;
        this.machineryMapper = machineryMapper;
        this.machineryLocationMapper = machineryLocationMapper;
        this.dispatchService = dispatchService;
    }

    /**
     * 生成完整调度建议。
     */
    public DispatchSuggestionResult suggest() {
        // 先刷新超时状态，保证空闲判断准确
        dispatchService.markTimeout();

        List<DispatchSuggestionResult.PendingTaskCard> pendingTasks = buildPendingTasks();
        List<DispatchSuggestionResult.OperatorCandidate> candidates = buildOperatorCandidates();
        List<DispatchSuggestionResult.MachineryOption> machinery = buildMachineryOptions();
        List<String> recommendations = buildRecommendations(pendingTasks, candidates);
        List<String> dataGaps = buildDataGaps();

        return new DispatchSuggestionResult(pendingTasks, candidates, machinery,
                recommendations, dataGaps);
    }

    /**
     * 找出未结束、且没有在途或已完成派单的农活，组装待派卡片。
     * 仅超时/取消的历史派单不阻塞重新派单，但会在卡片上标注。
     */
    private List<DispatchSuggestionResult.PendingTaskCard> buildPendingTasks() {
        List<FarmWork> allWorks = farmWorkMapper.selectList(null);
        List<DispatchSuggestionResult.PendingTaskCard> result = new ArrayList<>();

        for (FarmWork work : allWorks) {
            if (work.getStatus() != null && work.getStatus() == 1) {
                continue;
            }

            List<WorkDispatch> workDispatches = dispatchMapper.selectList(
                    new LambdaQueryWrapper<WorkDispatch>()
                            .eq(WorkDispatch::getFarmWorkId, work.getId()));
            boolean occupied = workDispatches.stream().anyMatch(
                    d -> OPEN_STATUSES.contains(d.getStatus())
                            || WorkDispatch.STATUS_COMPLETED.equals(d.getStatus()));
            if (occupied) {
                continue;
            }
            String historyNote = buildHistoryNote(workDispatches);

            Long plotId = null;
            String plotName = null;
            Double lng = null;
            Double lat = null;
            FarmWorkLandPlot rel = workLandPlotMapper.selectOne(
                    new LambdaQueryWrapper<FarmWorkLandPlot>()
                            .eq(FarmWorkLandPlot::getFarmWorkId, work.getId())
                            .last("limit 1"));
            if (rel != null) {
                plotId = rel.getLandPlotId();
                LandPlot plot = landPlotMapper.selectById(plotId);
                if (plot != null) {
                    plotName = plot.getLandName();
                    if (plot.getCenterPoint() != null) {
                        lng = plot.getCenterPoint().getX();
                        lat = plot.getCenterPoint().getY();
                    }
                }
            }

            result.add(new DispatchSuggestionResult.PendingTaskCard(
                    work.getId(), work.getName(), work.getArea(), plotId, plotName,
                    lng, lat, work.getEndTime(), null, historyNote));
        }
        return result;
    }

    /**
     * 根据历史派单生成重新调度说明。
     */
    private String buildHistoryNote(List<WorkDispatch> workDispatches) {
        long timeout = countBy(workDispatches, WorkDispatch.STATUS_TIMEOUT);
        long cancelled = countBy(workDispatches, WorkDispatch.STATUS_CANCELLED);
        if (timeout == 0 && cancelled == 0) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (timeout > 0) {
            parts.add("此前派单超时 " + timeout + " 次");
        }
        if (cancelled > 0) {
            parts.add("此前派单取消 " + cancelled + " 次");
        }
        return String.join("，", parts) + "，建议重新调度。";
    }

    /**
     * 评估每个机手的空闲状态与历史派单表现。
     */
    private List<DispatchSuggestionResult.OperatorCandidate> buildOperatorCandidates() {
        List<SysUser> users = sysUserMapper.selectUserList(new SysUser());
        List<DispatchSuggestionResult.OperatorCandidate> result = new ArrayList<>();

        for (SysUser user : users) {
            List<WorkDispatch> history = dispatchMapper.selectList(
                    new LambdaQueryWrapper<WorkDispatch>()
                            .eq(WorkDispatch::getAssigneeId, String.valueOf(user.getUserId())));

            long completed = countBy(history, WorkDispatch.STATUS_COMPLETED);
            long accepted = countBy(history, WorkDispatch.STATUS_ACCEPTED);
            long dispatched = countBy(history, WorkDispatch.STATUS_DISPATCHED);
            long timeout = countBy(history, WorkDispatch.STATUS_TIMEOUT);
            long cancelled = countBy(history, WorkDispatch.STATUS_CANCELLED);
            long total = history.size();
            boolean idle = history.stream().noneMatch(d -> OPEN_STATUSES.contains(d.getStatus()));
            double rate = total == 0 ? 0
                    : Math.round(completed * 10000.0 / total) / 100.0;

            result.add(new DispatchSuggestionResult.OperatorCandidate(
                    user.getUserId(), user.getUserName(), user.getNickName(), idle,
                    completed, accepted, dispatched, timeout, cancelled, rate,
                    buildOperatorNote(total, completed, timeout, idle)));
        }
        return result;
    }

    /**
     * 组装未下架农机及其位置。
     */
    private List<DispatchSuggestionResult.MachineryOption> buildMachineryOptions() {
        List<AgriculturalMachinery> machines = machineryMapper.selectList(
                new LambdaQueryWrapper<AgriculturalMachinery>()
                        .eq(AgriculturalMachinery::getStatus, 0));

        List<AgriculturalMachineryLocation> locations = machineryLocationMapper.selectList(null);
        Map<String, String> locationMap = locations.stream()
                .collect(Collectors.toMap(
                        AgriculturalMachineryLocation::getAgriculturalMachineryId,
                        AgriculturalMachineryLocation::getLocation,
                        (a, b) -> a));

        return machines.stream()
                .map(m -> new DispatchSuggestionResult.MachineryOption(
                        m.getId(), m.getName(), m.getTypeId(),
                        locationMap.get(m.getId()), m.getPhoneNumber()))
                .toList();
    }

    /**
     * 针对每条待派农活，挑选空闲且历史完成率最高的机手，生成建议。
     */
    private List<String> buildRecommendations(
            List<DispatchSuggestionResult.PendingTaskCard> pendingTasks,
            List<DispatchSuggestionResult.OperatorCandidate> candidates) {
        List<String> recommendations = new ArrayList<>();

        if (pendingTasks.isEmpty()) {
            recommendations.add("当前没有待派农活，暂时无需调度。");
            return recommendations;
        }

        List<DispatchSuggestionResult.OperatorCandidate> idleCandidates =
                candidates.stream()
                        .filter(DispatchSuggestionResult.OperatorCandidate::idle)
                        .sorted((a, b) -> Double.compare(b.completionRate(), a.completionRate()))
                        .toList();

        for (DispatchSuggestionResult.PendingTaskCard task : pendingTasks) {
            if (idleCandidates.isEmpty()) {
                recommendations.add("农活 " + task.farmWorkId() + "（" + task.name()
                        + "）：目前没有空闲机手，建议等待在途机手释放或协调新增运力后再派。");
                continue;
            }
            DispatchSuggestionResult.OperatorCandidate preferred = idleCandidates.get(0);
            String prefix = task.historyNote() == null ? "" : task.historyNote() + " ";
            recommendations.add("农活 " + task.farmWorkId() + "（" + task.name() + "，约 "
                    + task.area() + " 亩）：" + prefix + "建议优先派给 " + preferred.nickName()
                    + "（" + preferred.userName() + "），其当前空闲、历史完成率 "
                    + preferred.completionRate() + "%。" + preferred.note()
                    + " 最终派单请人工确认后再生效。");
        }
        return recommendations;
    }

    /**
     * 机手建议说明。
     */
    private String buildOperatorNote(long total, long completed, long timeout, boolean idle) {
        if (total == 0) {
            return "暂无历史派单记录，可作为新运力考察。";
        }
        List<String> parts = new ArrayList<>();
        parts.add("历史累计 " + total + " 单、已完成 " + completed + " 单");
        if (timeout > 0) {
            parts.add("曾超时 " + timeout + " 单，需关注履约时效");
        }
        return String.join("，", parts) + "。";
    }

    /**
     * 数据缺口如实说明。
     */
    private List<String> buildDataGaps() {
        return List.of(
                "系统账号（sys_user）与农机（agricultural_machinery）的手机号不一致，当前未建立机手与农机的可靠关联，农机运力需人工对应。",
                "机手账号无实时位置字段，无法真实计算机手到待派地块的距离，就近程度需调度员结合农机乡镇位置人工核对。",
                "本结果仅为派单建议，不直接写入派单；任何派单均需调度员确认后通过调度接口执行。");
    }

    private long countBy(List<WorkDispatch> list, String status) {
        return list.stream().filter(d -> status.equals(d.getStatus())).count();
    }
}
