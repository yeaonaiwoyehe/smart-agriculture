package com.agri.cms.ai.agent;

import com.agri.cms.ai.agent.mapper.AgentTaskClaimMapper;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.mapper.*;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务查询服务（A-11，AG-01 任务助手）。
 *
 * 负责把农活（farm_work）连同地块、作业类型、单价规则、认领状态与距离，
 * 组装成农机手视角的任务卡片，并按"可认领 / 我的任务"分组。
 *
 * 数据原则：所有字段均来自真实业务表；单价无规则、定位缺失时对应字段留空，
 * 由上层明确标注，绝不臆造。
 *
 * @author agrinav
 */
@Service
public class TaskQueryService {

    private static final Logger log = LoggerFactory.getLogger(TaskQueryService.class);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 农活类型（work_type 表 2001-2008）到单价规则类型（work 表 4001-4006）的语义映射。
     * 2007 无人机飞防、2008 粮食转运暂无单价规则，不在此表中。
     */
    private static final Map<Long, Long> WORK_TYPE_TO_PRICING_TYPE = Map.of(
            2001L, 4001L,
            2002L, 4002L,
            2003L, 4003L,
            2004L, 4004L,
            2005L, 4005L,
            2006L, 4006L
    );

    private final FarmWorkInfoMapper farmWorkMapper;
    private final FarmWorkLandPlotMapper workLandPlotMapper;
    private final FarmWorkTypeMapper workTypeRelMapper;
    private final LandPlotMapper landPlotMapper;
    private final WorkTypeMapper workTypeMapper;
    private final PricingRuleMapper pricingRuleMapper;
    private final AgentTaskClaimMapper claimMapper;

    public TaskQueryService(FarmWorkInfoMapper farmWorkMapper,
                            FarmWorkLandPlotMapper workLandPlotMapper,
                            FarmWorkTypeMapper workTypeRelMapper,
                            LandPlotMapper landPlotMapper,
                            WorkTypeMapper workTypeMapper,
                            PricingRuleMapper pricingRuleMapper,
                            AgentTaskClaimMapper claimMapper) {
        this.farmWorkMapper = farmWorkMapper;
        this.workLandPlotMapper = workLandPlotMapper;
        this.workTypeRelMapper = workTypeRelMapper;
        this.landPlotMapper = landPlotMapper;
        this.workTypeMapper = workTypeMapper;
        this.pricingRuleMapper = pricingRuleMapper;
        this.claimMapper = claimMapper;
    }

    /**
     * 查询当前可认领任务与我的任务。
     *
     * @param currentUserId 当前登录用户 ID（字符串形式）
     * @param longitude     可选，农机手当前经度
     * @param latitude      可选，农机手当前纬度
     * @return 分组后的任务卡片
     */
    @DS("pg")
    public TaskQueryResult queryTasks(String currentUserId, Double longitude, Double latitude) {
        boolean hasLocation = longitude != null && latitude != null;

        // 1. 查所有"可认领"状态的农活（status=0）
        List<FarmWork> farmWorks = farmWorkMapper.selectList(
                new LambdaQueryWrapper<FarmWork>()
                        .eq(FarmWork::getStatus, Constants.AVAILABLE)
                        .orderByDesc(FarmWork::getCreateTime));

        if (farmWorks.isEmpty()) {
            log.info("[A-11] 当前没有可认领状态的农活");
            return new TaskQueryResult(List.of(), List.of(), hasLocation, 0);
        }

        List<String> workIds = farmWorks.stream().map(FarmWork::getId).toList();

        // 2. 农活 -> 地块
        Map<String, Long> workToLand = workLandPlotMapper.selectList(
                        new LambdaQueryWrapper<FarmWorkLandPlot>().in(FarmWorkLandPlot::getFarmWorkId, workIds))
                .stream()
                .collect(Collectors.toMap(FarmWorkLandPlot::getFarmWorkId, FarmWorkLandPlot::getLandPlotId, (a, b) -> a));

        // 3. 农活 -> 作业类型（2001-2008）
        Map<String, Long> workToType = workTypeRelMapper.selectList(
                        new LambdaQueryWrapper<FarmWorkType>().in(FarmWorkType::getFarmWorkId, workIds))
                .stream()
                .collect(Collectors.toMap(FarmWorkType::getFarmWorkId, FarmWorkType::getWorkTypeId, (a, b) -> a));

        // 4. 地块详情
        Set<Long> landIds = new HashSet<>(workToLand.values());
        Map<Long, LandPlot> landMap = landIds.isEmpty()
                ? Map.of()
                : landPlotMapper.selectBatchIds(landIds).stream()
                .collect(Collectors.toMap(LandPlot::getId, Function.identity(), (a, b) -> a));

        // 5. 作业类型名称（work_type 2001-2008）
        Set<Long> typeIds = new HashSet<>(workToType.values());
        Map<Long, WorkType> typeMap = typeIds.isEmpty()
                ? Map.of()
                : workTypeMapper.selectBatchIds(typeIds).stream()
                .collect(Collectors.toMap(WorkType::getId, Function.identity(), (a, b) -> a));

        // 6. 启用中的单价规则，按其 work_type（4001-4006）索引
        Map<Long, PricingRule> pricingMap = pricingRuleMapper.selectList(
                        new LambdaQueryWrapper<PricingRule>().eq(PricingRule::getStatus, (short) 0))
                .stream()
                .filter(pr -> pr.getWorkType() != null)
                .collect(Collectors.toMap(PricingRule::getWorkType, Function.identity(), (a, b) -> a));

        // 7. 已生效的认领记录（CLAIMED），按农活 ID 索引
        Map<String, AgentTaskClaim> claimMap = claimMapper.selectList(
                        new LambdaQueryWrapper<AgentTaskClaim>()
                                .in(AgentTaskClaim::getFarmWorkId, workIds)
                                .eq(AgentTaskClaim::getStatus, ClaimStatus.CLAIMED.code()))
                .stream()
                .collect(Collectors.toMap(AgentTaskClaim::getFarmWorkId, Function.identity(), (a, b) -> a));

        // 8. 逐张组装卡片
        List<TaskCard> available = new ArrayList<>();
        List<TaskCard> myTasks = new ArrayList<>();

        for (FarmWork work : farmWorks) {
            Long landId = workToLand.get(work.getId());
            LandPlot plot = landId == null ? null : landMap.get(landId);
            Long typeId = workToType.get(work.getId());
            WorkType workType = typeId == null ? null : typeMap.get(typeId);
            AgentTaskClaim claim = claimMap.get(work.getId());

            // 单价：农活类型 -> 单价规则类型 -> 单价
            BigDecimal unitPrice = null;
            Long pricingType = typeId == null ? null : WORK_TYPE_TO_PRICING_TYPE.get(typeId);
            if (pricingType != null) {
                PricingRule rule = pricingMap.get(pricingType);
                if (rule != null) {
                    unitPrice = rule.getUnitPrice();
                }
            }

            // 预估金额 = 单价 × 本次作业亩数
            BigDecimal estimated = null;
            if (unitPrice != null && work.getArea() != null) {
                estimated = unitPrice.multiply(work.getArea()).setScale(2, RoundingMode.HALF_UP);
            }

            // 距离：需要定位 + 地块中心
            Double distance = null;
            if (hasLocation && plot != null && plot.getCenterPoint() != null) {
                distance = GeoUtils.haversineMeters(
                        longitude, latitude,
                        plot.getCenterPoint().getX(), plot.getCenterPoint().getY());
            }

            Double lng = null;
            Double lat = null;
            if (plot != null && plot.getCenterPoint() != null) {
                lng = plot.getCenterPoint().getX();
                lat = plot.getCenterPoint().getY();
            }

            boolean claimed = claim != null;
            boolean claimedByMe = claimed && currentUserId != null
                    && currentUserId.equals(String.valueOf(claim.getUserId()));

            TaskCard card = new TaskCard(
                    work.getId(),
                    work.getName(),
                    typeId,
                    workType == null ? null : workType.getWorkName(),
                    landId,
                    plot == null ? null : plot.getLandName(),
                    plot == null ? null : plot.getAddress(),
                    work.getArea(),
                    unitPrice,
                    estimated,
                    distance,
                    lng,
                    lat,
                    work.getEndTime() == null ? null : work.getEndTime().format(FMT),
                    work.getPhoneNumber(),
                    claimed,
                    claimedByMe,
                    claimedByMe ? claim.getReceiptNo() : null
            );

            if (claimedByMe) {
                myTasks.add(card);
            } else if (!claimed) {
                available.add(card);
            }
            // 被他人认领的卡片不在当前用户的两组中展示
        }

        // 可认领任务按距离排序（有定位时），方便就近选择
        if (hasLocation) {
            available.sort(Comparator.comparing(c -> c.distanceMeters() == null
                    ? Double.MAX_VALUE : c.distanceMeters()));
        }

        int total = available.size() + myTasks.size();
        log.info("[A-11] 组装任务卡片完成：可认领 {} 张，我的任务 {} 张，定位={}",
                available.size(), myTasks.size(), hasLocation);
        return new TaskQueryResult(available, myTasks, hasLocation, total);
    }

    /**
     * 认领状态常量（与 agent_task_claim.status 对应）。
     */
    public enum ClaimStatus {
        CLAIMED("CLAIMED"),
        CANCELLED("CANCELLED");

        private final String code;

        ClaimStatus(String code) {
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
