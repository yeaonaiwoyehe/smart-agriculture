package com.agri.cms.ai.agent;

import com.agri.cms.ai.agent.mapper.WorkOrderGeoMapper;
import com.agri.cms.ai.tool.ReceiptGenerator;
import com.agri.cms.system.domain.FieldWork;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.mapper.FieldWorkMapper;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.mapper.TrajectoryInfoMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作业单服务（A-12，AG-02 作业助手）。
 *
 * 负责开始作业（创建作业单）与结束作业（更新作业单），含归属、状态、
 * 超界提醒等完整校验。所有数值坚持"取数回填、不估算"：作业面积不在此
 * 计算，留待轨迹核算流程回填，结束单明确标注"待核算"。
 *
 * @author agrinav
 */
@Service
@DS("pg")
public class WorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);

    /** 作业单状态：0 进行中 / 1 已完成 */
    public static final short STATUS_IN_PROGRESS = 0;
    public static final short STATUS_FINISHED = 1;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TrajectoryInfoMapper trajectoryInfoMapper;
    private final WorkOrderGeoMapper geoMapper;
    private final LandPlotMapper landPlotMapper;
    private final FieldWorkMapper fieldWorkMapper;
    private final ReceiptGenerator receiptGenerator;

    public WorkOrderService(TrajectoryInfoMapper trajectoryInfoMapper,
                            WorkOrderGeoMapper geoMapper,
                            LandPlotMapper landPlotMapper,
                            FieldWorkMapper fieldWorkMapper,
                            ReceiptGenerator receiptGenerator) {
        this.trajectoryInfoMapper = trajectoryInfoMapper;
        this.geoMapper = geoMapper;
        this.landPlotMapper = landPlotMapper;
        this.fieldWorkMapper = fieldWorkMapper;
        this.receiptGenerator = receiptGenerator;
    }

    /**
     * 开始作业：创建作业单。
     *
     * @param plotId     地块 ID（必填）
     * @param workTypeId 作业类型 ID（work 表 4001-4006，必填）
     * @param width      作业幅宽（厘米，可选）
     * @param longitude  开始定位经度（可选）
     * @param latitude   开始定位纬度（可选）
     * @param userId     当前用户 ID
     * @param userName   当前登录账号（手机号）
     * @return 作业单结果
     */
    public WorkOrderResult startWork(Long plotId, Long workTypeId, Integer width,
                                     Double longitude, Double latitude,
                                     String userId, String userName) {
        if (plotId == null) {
            return fail(null, "开始作业需要指定地块，请告诉我在哪块地作业。");
        }
        if (workTypeId == null) {
            return fail(null, "开始作业需要指定作业类型（如耕地、播种、植保等）。");
        }

        // 校验地块
        LandPlot plot = landPlotMapper.selectById(plotId);
        if (plot == null || plot.getIsDeleted() == 1) {
            return fail(null, "未找到编号为 " + plotId + " 的地块，请核对后再试。");
        }

        // 校验作业类型
        FieldWork work = fieldWorkMapper.selectById(workTypeId);
        if (work == null || Boolean.TRUE.equals(work.getIsDeleted())) {
            return fail(null, "作业类型 " + workTypeId + " 不存在或已停用，请选择有效类型。");
        }

        // 防止同一用户同时存在多个进行中作业单
        TrajectoryInfo active = findActiveWorkOrder(userId);
        if (active != null) {
            return fail(active.getTrajectoryInfoId(),
                    "您已有一个进行中的作业单（" + active.getTrajectoryInfoId()
                            + "），请先结束当前作业再开始新的作业。");
        }

        // 开始点超界提醒（仅提醒，不阻止开始）
        String boundaryWarning = checkBoundary(plotId, longitude, latitude);

        // 生成业务作业单号，并作为作业单主键
        String workOrderNo = nextWorkOrderNo();
        LocalDateTime now = LocalDateTime.now();

        TrajectoryInfo info = new TrajectoryInfo();
        info.setTrajectoryInfoId(workOrderNo);
        info.setWorkType(String.valueOf(workTypeId));
        info.setPhoneNumber(userName);
        info.setWidth(width);
        info.setCreatedBy(userId);
        info.setCreatedTime(now);
        info.setStatus(STATUS_IN_PROGRESS);
        info.setStartTime(now);
        info.setPlotId(plotId);
        info.setArea(null); // 面积待轨迹核算，不估算
        trajectoryInfoMapper.insert(info);

        String receiptNo = receiptGenerator.next();
        log.info("[A-12] 用户 {} 开始作业，作业单 {}，回执 {}，超界提醒={}",
                userId, workOrderNo, receiptNo, boundaryWarning);

        String message = "作业已开始。作业单号：" + workOrderNo
                + "，地块：" + plot.getLandName()
                + "，类型：" + work.getWorkName()
                + "。作业面积将在结束后按轨迹核算。";
        return new WorkOrderResult(true, message, workOrderNo, workOrderNo,
                STATUS_IN_PROGRESS, "进行中", receiptNo, boundaryWarning);
    }

    /**
     * 结束作业：更新作业单。
     *
     * @param workOrderId 作业单号（必填）
     * @param longitude   结束定位经度（可选）
     * @param latitude    结束定位纬度（可选）
     * @param userId      当前用户 ID
     * @return 作业单结果
     */
    public WorkOrderResult endWork(String workOrderId, Double longitude, Double latitude, String userId) {
        if (workOrderId == null || workOrderId.isBlank()) {
            return fail(null, "结束作业需要提供作业单号。若您只有一个进行中的作业，也可以直接告诉我地块。");
        }

        TrajectoryInfo info = trajectoryInfoMapper.selectById(workOrderId);
        if (info == null) {
            return fail(workOrderId, "未找到作业单 " + workOrderId + "，请核对单号。");
        }

        // 归属校验
        if (info.getCreatedBy() != null && !info.getCreatedBy().equals(userId)) {
            return fail(workOrderId, "该作业单不属于您，无法结束。");
        }

        // 状态校验
        if (info.getStatus() == null || info.getStatus() != STATUS_IN_PROGRESS) {
            return fail(workOrderId, "该作业单已结束，无需重复操作。");
        }

        // 结束点超界提醒
        String boundaryWarning = checkBoundary(info.getPlotId(), longitude, latitude);

        info.setEndTime(LocalDateTime.now());
        info.setStatus(STATUS_FINISHED);
        // area 保持待核算，由后续轨迹核算流程回填，不在此估算
        trajectoryInfoMapper.updateById(info);

        String receiptNo = receiptGenerator.next();
        log.info("[A-12] 用户 {} 结束作业单 {}，回执 {}，超界提醒={}",
                userId, workOrderId, receiptNo, boundaryWarning);

        String message = "作业已结束。作业单号：" + workOrderId
                + "，开始时间：" + (info.getStartTime() == null ? "—" : info.getStartTime().format(DT_FMT))
                + "，结束时间：" + info.getEndTime().format(DT_FMT)
                + "。作业面积待轨迹核算后回填，可凭回执核对。";
        return new WorkOrderResult(true, message, workOrderId, workOrderId,
                STATUS_FINISHED, "已完成", receiptNo, boundaryWarning);
    }

    /**
     * 查询当前用户进行中的作业单，不存在返回 null。
     */
    public TrajectoryInfo findActiveWorkOrder(String userId) {
        return trajectoryInfoMapper.selectOne(
                new LambdaQueryWrapper<TrajectoryInfo>()
                        .eq(TrajectoryInfo::getCreatedBy, userId)
                        .eq(TrajectoryInfo::getStatus, STATUS_IN_PROGRESS)
                        .orderByDesc(TrajectoryInfo::getStartTime)
                        .last("limit 1"));
    }

    /**
     * 查询当前用户的全部作业单（按开始时间倒序），组装成卡片。
     *
     * @param userId 当前用户 ID
     * @return 作业单卡片列表
     */
    public List<WorkOrderCard> listMyWorkOrders(String userId) {
        List<TrajectoryInfo> infos = trajectoryInfoMapper.selectList(
                new LambdaQueryWrapper<TrajectoryInfo>()
                        .eq(TrajectoryInfo::getCreatedBy, userId)
                        .orderByDesc(TrajectoryInfo::getStartTime));

        if (infos.isEmpty()) {
            return List.of();
        }

        // 批量查地块
        Set<Long> plotIds = infos.stream().map(TrajectoryInfo::getPlotId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, LandPlot> plotMap = plotIds.isEmpty()
                ? Map.of()
                : landPlotMapper.selectBatchIds(plotIds).stream()
                .collect(Collectors.toMap(LandPlot::getId, Function.identity(), (a, b) -> a));

        // 批量查作业类型（workType 存的是 4001-4006 字符串）
        Set<Long> workTypeIds = infos.stream()
                .map(TrajectoryInfo::getWorkType)
                .filter(t -> t != null && !t.isBlank())
                .map(t -> Long.valueOf(t.trim()))
                .collect(Collectors.toSet());
        Map<Long, FieldWork> workMap = workTypeIds.isEmpty()
                ? Map.of()
                : fieldWorkMapper.selectBatchIds(workTypeIds).stream()
                .collect(Collectors.toMap(FieldWork::getId, Function.identity(), (a, b) -> a));

        List<WorkOrderCard> cards = new ArrayList<>();
        for (TrajectoryInfo info : infos) {
            Long plotId = info.getPlotId();
            LandPlot plot = plotId == null ? null : plotMap.get(plotId);

            Long workTypeId = info.getWorkType() == null ? null : Long.valueOf(info.getWorkType().trim());
            FieldWork work = workTypeId == null ? null : workMap.get(workTypeId);

            boolean finished = info.getStatus() != null && info.getStatus() == STATUS_FINISHED;
            boolean areaPending = info.getArea() == null;

            cards.add(new WorkOrderCard(
                    info.getTrajectoryInfoId(),
                    info.getStatus(),
                    finished ? "已完成" : "进行中",
                    plotId,
                    plot == null ? null : plot.getLandName(),
                    info.getWorkType(),
                    work == null ? null : work.getWorkName(),
                    info.getWidth(),
                    info.getStartTime() == null ? null : info.getStartTime().format(DT_FMT),
                    info.getEndTime() == null ? null : info.getEndTime().format(DT_FMT),
                    info.getArea(),
                    areaPending
            ));
        }
        return cards;
    }

    /**
     * 超界判断：定位点不在地块边界内时返回提醒文本，否则返回 null。
     */
    private String checkBoundary(Long plotId, Double longitude, Double latitude) {
        if (plotId == null || longitude == null || latitude == null) {
            return null;
        }
        Integer inside = geoMapper.countPointInside(plotId, longitude, latitude);
        if (inside != null && inside > 0) {
            return null;
        }
        return "提醒：当前定位点（" + longitude + ", " + latitude
                + "）不在该地块边界内，可能已超界，请核对作业位置。";
    }

    /**
     * 生成业务作业单号：WO-yyyyMMdd-6位大写随机。
     */
    private String nextWorkOrderNo() {
        String date = LocalDate.now().format(DATE_FMT);
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 6).toUpperCase();
        return "WO-" + date + "-" + random;
    }

    private WorkOrderResult fail(String workOrderId, String message) {
        log.info("[A-12] 作业单操作失败 workOrderId={} 原因={}", workOrderId, message);
        return new WorkOrderResult(false, message, workOrderId, null, null, null, null, null);
    }
}
