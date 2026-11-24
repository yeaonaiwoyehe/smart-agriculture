package com.agri.cms.ai.agent;

import com.agri.cms.ai.agent.mapper.AgentAlertMapper;
import com.agri.cms.system.domain.EffectivePointBuffer;
import com.agri.cms.system.domain.ReviewRequest;
import com.agri.cms.system.domain.WorkDispatch;
import com.agri.cms.system.mapper.EffectivePointBufferMapper;
import com.agri.cms.system.mapper.ReviewRequestMapper;
import com.agri.cms.system.mapper.WorkDispatchMapper;
import com.agri.cms.system.service.DispatchService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 异常研判服务（A-17 MA-02）。
 *
 * 聚合异常工单、超时派单、面积争议与作业停滞，基于真实数据给出可追溯的
 * 研判，每条区分「数据事实」与「可能原因（推测）」。只分析不修改数据。
 *
 * @author agrinav
 */
@Service
public class AnomalyInsightService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyInsightService.class);

    /** 停滞判定：相邻点时间间隔超过该秒数且位移小于该距离，视为停滞 */
    private static final long STAGNATION_GAP_SECONDS = 300;
    private static final double STAGNATION_DISTANCE_METERS = 20.0;

    private final AgentAlertMapper alertMapper;
    private final WorkDispatchMapper dispatchMapper;
    private final ReviewRequestMapper reviewMapper;
    private final EffectivePointBufferMapper pointBufferMapper;
    private final DispatchService dispatchService;

    public AnomalyInsightService(AgentAlertMapper alertMapper,
                                 WorkDispatchMapper dispatchMapper,
                                 ReviewRequestMapper reviewMapper,
                                 EffectivePointBufferMapper pointBufferMapper,
                                 DispatchService dispatchService) {
        this.alertMapper = alertMapper;
        this.dispatchMapper = dispatchMapper;
        this.reviewMapper = reviewMapper;
        this.pointBufferMapper = pointBufferMapper;
        this.dispatchService = dispatchService;
    }

    /**
     * 生成综合异常研判。
     */
    public AnomalyInsightResult analyze() {
        dispatchService.markTimeout();
        LocalDateTime now = LocalDateTime.now();

        List<AnomalyInsightResult.AlertAnomaly> alerts = buildAlerts(now);
        List<AnomalyInsightResult.TimeoutAnomaly> timeouts = buildTimeouts(now);
        List<AnomalyInsightResult.DisputeAnomaly> disputes = buildDisputes();
        List<AnomalyInsightResult.StagnationAnomaly> stagnations = buildStagnations();

        long pendingAlerts = alerts.stream()
                .filter(a -> AgentAlert.STATUS_PENDING.equals(a.status())
                        || AgentAlert.STATUS_PROCESSING.equals(a.status()))
                .count();
        long total = pendingAlerts + timeouts.size() + disputes.size() + stagnations.size();

        AnomalyInsightResult.Overview overview = new AnomalyInsightResult.Overview(
                pendingAlerts, timeouts.size(), disputes.size(), stagnations.size(), total);

        return new AnomalyInsightResult(overview, alerts, timeouts, disputes,
                stagnations, buildDataGaps());
    }

    /**
     * 异常工单研判。
     */
    private List<AnomalyInsightResult.AlertAnomaly> buildAlerts(LocalDateTime now) {
        List<AgentAlert> list = alertMapper.selectList(null);
        List<AnomalyInsightResult.AlertAnomaly> result = new ArrayList<>();

        for (AgentAlert a : list) {
            boolean overdue = a.getDeadline() != null && a.getDeadline().isBefore(now);
            String fact = "工单 " + a.getAlertNo() + "，类型「" + a.getAlertType()
                    + "」，状态 " + a.getStatus() + "，处理人 " + a.getHandlerName()
                    + "，处理时限 " + fmt(a.getDeadline()) + "；现象描述："
                    + a.getDescription();
            result.add(new AnomalyInsightResult.AlertAnomaly(
                    a.getAlertNo(), a.getAlertType(), a.getDescription(), a.getStatus(),
                    a.getHandlerName(), a.getDeadline(), overdue, fact,
                    alertPossibleCauses(a.getAlertType())));
        }
        return result;
    }

    /**
     * 超时派单研判。
     */
    private List<AnomalyInsightResult.TimeoutAnomaly> buildTimeouts(LocalDateTime now) {
        List<WorkDispatch> list = dispatchMapper.selectList(
                new LambdaQueryWrapper<WorkDispatch>()
                        .eq(WorkDispatch::getStatus, WorkDispatch.STATUS_TIMEOUT));
        List<AnomalyInsightResult.TimeoutAnomaly> result = new ArrayList<>();

        for (WorkDispatch d : list) {
            long overdueDays = 0;
            if (d.getDeadline() != null) {
                overdueDays = Math.max(0, Duration.between(d.getDeadline(), now).toDays());
            }
            String fact = "派单 " + d.getDispatchNo() + "，农活 " + d.getFarmWorkId()
                    + "，机手 " + d.getAssigneeId() + "，应在 " + fmt(d.getDeadline())
                    + " 前完成，当前已超时，超过截止约 " + overdueDays + " 天。";
            List<String> causes = List.of(
                    "可能原因（推测）：机手未及时接单、路途较远或任务量超出预期，需向机手核实实际情况。",
                    "建议：联系机手确认能否继续，必要时改派其他空闲运力。");
            result.add(new AnomalyInsightResult.TimeoutAnomaly(
                    d.getDispatchNo(), d.getFarmWorkId(), d.getAssigneeId(),
                    d.getDeadline(), overdueDays, fact, causes));
        }
        return result;
    }

    /**
     * 面积争议研判。
     */
    private List<AnomalyInsightResult.DisputeAnomaly> buildDisputes() {
        List<ReviewRequest> list = reviewMapper.selectList(null);
        List<AnomalyInsightResult.DisputeAnomaly> result = new ArrayList<>();

        for (ReviewRequest r : list) {
            BigDecimal gap = r.getClaimedArea() == null || r.getSystemArea() == null
                    ? null : r.getClaimedArea().subtract(r.getSystemArea());
            String fact = "复核单 " + r.getReviewNo() + "，作业单 " + r.getTrajectoryInfoId()
                    + "，系统核算 " + r.getSystemArea() + " 亩，机手主张 "
                    + r.getClaimedArea() + " 亩，相差 " + (gap == null ? "—" : gap)
                    + " 亩；复核状态 " + r.getStatus() + "，终审意见：" + r.getFinalComment();
            List<String> causes = List.of(
                    "可能原因（推测）：作业边界理解不一致，或存在漏算、重复计算。",
                    "应以完整轨迹与地块边界的核算结果为准，机手主张需有轨迹证据支撑。");
            result.add(new AnomalyInsightResult.DisputeAnomaly(
                    r.getReviewNo(), r.getTrajectoryInfoId(), r.getSystemArea(),
                    r.getClaimedArea(), gap, r.getStatus(), r.getFinalComment(), fact, causes));
        }
        return result;
    }

    /**
     * 作业停滞研判：扫描相邻作业点，时间间隔过长且位移很小则标记。
     */
    private List<AnomalyInsightResult.StagnationAnomaly> buildStagnations() {
        List<EffectivePointBuffer> points = pointBufferMapper.selectList(
                new LambdaQueryWrapper<EffectivePointBuffer>()
                        .orderByAsc(EffectivePointBuffer::getPhoneNumber)
                        .orderByAsc(EffectivePointBuffer::getDeviceTime));
        List<AnomalyInsightResult.StagnationAnomaly> result = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            EffectivePointBuffer prev = points.get(i - 1);
            EffectivePointBuffer cur = points.get(i);
            if (!samePhone(prev, cur) || prev.getDeviceTime() == null || cur.getDeviceTime() == null) {
                continue;
            }
            long gapSeconds = Duration.between(prev.getDeviceTime(), cur.getDeviceTime()).getSeconds();
            double distance = GeoUtils.haversineMeters(
                    prev.getLongitude(), prev.getLatitude(),
                    cur.getLongitude(), cur.getLatitude());
            if (gapSeconds >= STAGNATION_GAP_SECONDS && distance <= STAGNATION_DISTANCE_METERS) {
                String fact = "机手 " + cur.getPhoneNumber() + " 在 (" + cur.getLongitude()
                        + ", " + cur.getLatitude() + ") 附近约 " + gapSeconds
                        + " 秒无明显移动（位移约 " + Math.round(distance) + " 米）。";
                List<String> causes = List.of(
                        "可能原因（推测）：设备故障、陷车、临时避让或信号中断，需结合现场与工单核实。");
                result.add(new AnomalyInsightResult.StagnationAnomaly(
                        cur.getPhoneNumber(), prev.getDeviceTime(), cur.getDeviceTime(),
                        gapSeconds, cur.getLongitude(), cur.getLatitude(), fact, causes));
            }
        }
        return result;
    }

    /**
     * 异常工单的可能原因（按类型，均标注为推测）。
     */
    private List<String> alertPossibleCauses(String type) {
        if (AgentAlert.TYPE_TRAPPED.equals(type)) {
            return List.of(
                    "可能原因（推测）：土壤湿软、田间承载不足导致陷车。",
                    "建议：现场核实并安排牵引、排水，确认安全后再作业。");
        }
        if (AgentAlert.TYPE_EQUIPMENT.equals(type)) {
            return List.of(
                    "可能原因（推测）：部件磨损或作业负荷过大。",
                    "建议：停机检修，排除故障后再投入作业。");
        }
        return List.of(
                "可能原因（推测）：需结合现场情况进一步判断。",
                "建议：联系上报人核实并留存处置记录。");
    }

    private List<String> buildDataGaps() {
        return List.of(
                "研判中的「可能原因」均为基于数据的推测，不能直接作为责任认定或故障结论，需现场核实。",
                "作业点目前仅覆盖部分时段，停滞与轨迹异常判断受数据完整性限制，缺失时段无法分析。",
                "本结果只做分析解释，不修改任何异常状态或业务数据，处置动作需管理人员执行。");
    }

    private boolean samePhone(EffectivePointBuffer a, EffectivePointBuffer b) {
        return a.getPhoneNumber() != null && a.getPhoneNumber().equals(b.getPhoneNumber());
    }

    private String fmt(LocalDateTime t) {
        return t == null ? "—" : t.toString().replace('T', ' ');
    }
}
