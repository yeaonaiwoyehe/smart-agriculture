package com.agri.cms.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.enums.ActivityLevel;
import com.agri.cms.common.enums.RiskLevel;
import com.agri.cms.common.exception.ServiceException;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.dto.UserActivityProfileDTO;
import com.agri.cms.system.domain.dto.UserBehaviorAnalysisDTO;
import com.agri.cms.system.domain.dto.UserRiskProfileDTO;
import com.agri.cms.system.domain.vo.Point;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.mapper.*;
import com.agri.cms.system.service.IUserBehaviorAnalysisService;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author 李鸿麟
 * 用户数据分析实现类
 */
@Service
public class UserBehaviorAnalysisServiceImpl implements IUserBehaviorAnalysisService {

    @Resource
    private SysLogininforMapper logininforMapper;

    @Resource
    private LandPlotMapper landPlotMapper;
    @Resource
    private TrajectoryInfoMapper trajectoryInfoMapper;
    @Resource
    private FarmWorkInfoMapper farmWorkMapper;
    @Resource
    private AgriculturalMachineryInfoMapper agriculturalMachineryMapper;
    @Resource
    private TrajectoryPointsMapper trajectoryPointsMapper;
    @Resource
    private SysUserMapper sysUserMapper;

    // 定义常量
    /**分页查询页数**/
    private static final int BATCH_SIZE = 1000;

    /**用户数据分析时间间隔**/
    private static final int ANALYSIS_MONTHS = 6;

    /**评分权重**/
    private static final double LAND_WEIGHT = 0.3;
    private static final double TRAJECTORY_WEIGHT = 0.3;
    private static final double FARMWORK_WEIGHT = 0.2;
    private static final double MACHINERY_WEIGHT = 0.2;


    /**
     * 分析某一个用户的活跃度
     * @param userName 用户名
     * @return UserActivityProfileDTO 用户活跃度档案DTO
     */
    @Override
    public UserActivityProfileDTO analyzeUserActivity(String userName) {
        if (StrUtil.isEmpty(userName)) {
            return new UserActivityProfileDTO();
        }

        List<SysLogininfor> loginRecords = logininforMapper
                .selectLoginRecordsByUserName(userName);

        if (CollUtil.isEmpty(loginRecords)) {
            return new UserActivityProfileDTO();
        }

        UserActivityProfileDTO profile = new UserActivityProfileDTO();
        profile.setUserName(userName);
        profile.setTotalLoginCount(loginRecords.size());

        // 登录成功率计算
        long successCount = loginRecords.stream()
                .filter(record -> "0".equals(record.getStatus()))
                .count();
        profile.setLoginSuccessRate(
                successCount * 1.0 / loginRecords.size()
        );

        // 登录频率评分
        profile.setLoginFrequencyScore(
                calculateLoginFrequencyScore(loginRecords)
        );

        // 地理位置多样性
        profile.setLoginLocationDiversity(
                calculateLocationDiversity(loginRecords)
        );

        // 设备多样性
        profile.setDeviceDiversityScore(
                calculateDeviceDiversityScore(loginRecords)
        );

        // 最近登录时间
        profile.setLatestLoginTime(
                loginRecords.stream()
                        .max(Comparator.comparing(SysLogininfor::getLoginTime))
                        .map(record -> new java.sql.Date(record.getLoginTime().getTime()))
                        .orElse(null)
        );

        // 活跃度评估
        profile.setActivityLevel(assessActivityLevel(profile));

        return profile;
    }

    /**
     * 评估某一个特定用户的风险水平
     * @param userName 用户名
     * @return UserRiskProfileDTO 用户风险档案DTO
     */
    @Override
    public UserRiskProfileDTO assessUserRisk(String userName) {
        List<SysLogininfor> records = logininforMapper
                .selectLoginRecordsByUserName(userName);

        UserRiskProfileDTO riskProfile = new UserRiskProfileDTO();
        riskProfile.setUserName(userName);

        // 异常登录检测
        riskProfile.setAbnormalLoginCount(
                detectAbnormalLogins(records)
        );

        // 唯一IP数量
        riskProfile.setUniqueIpCount(
                calculateUniqueIpCount(records)
        );

        // 登录失败次数
        riskProfile.setLoginFailureCount(
                logininforMapper.countLoginFailures(userName)
        );

        // 风险评估
        riskProfile.setRiskLevel(assessRiskLevel(riskProfile));

        return riskProfile;
    }

    /**
      * 此方法返回所有用户的行为分析结果，包括活跃度分析以及风险评估结果
      * @return UserBehaviorAnalysisDTO 用户行为分析DTO
     */
    @Override
    public UserBehaviorAnalysisDTO analyzeAllUsersBehavior() {
        UserBehaviorAnalysisDTO analysisResult = new UserBehaviorAnalysisDTO();

        // 计算时间范围
        // 使用 LocalDate 而不是 LocalDateTime
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minus(ANALYSIS_MONTHS, ChronoUnit.MONTHS);

        // 转换为 java.sql.Date
        Date endTime = Date.valueOf(endDate);
        Date startTime = Date.valueOf(startDate);

        // 获取总记录数
        long totalRecords = logininforMapper.countLoginRecordsInTimeRange(startTime, endTime);

        // 用于存储所有记录
        List<SysLogininfor> allLoginRecords = new ArrayList<>();

        // 分页查询数据
        for (int offset = 0; offset < totalRecords; offset += BATCH_SIZE) {
            List<SysLogininfor> batchRecords = logininforMapper.selectLoginRecordsWithPagination(
                    startTime, endTime, offset, BATCH_SIZE);
            allLoginRecords.addAll(batchRecords);
        }

        // 按用户名分组处理数据
        Map<String, List<SysLogininfor>> userLoginRecordsMap = allLoginRecords.stream()
                .filter(record -> StrUtil.isNotEmpty(record.getUserName()))
                .collect(Collectors.groupingBy(SysLogininfor::getUserName));

        // 处理用户活跃度分析
        Map<ActivityLevel, List<UserActivityProfileDTO>> activityProfiles = userLoginRecordsMap.entrySet()
                .stream()
                .map(entry -> createUserActivityProfile(entry.getKey(), entry.getValue()))
                .collect(Collectors.groupingBy(UserActivityProfileDTO::getActivityLevel));

        // 处理用户风险分析
        Map<RiskLevel, List<UserRiskProfileDTO>> riskProfiles = userLoginRecordsMap.entrySet()
                .stream()
                .map(entry -> createUserRiskProfile(entry.getKey(), entry.getValue()))
                .collect(Collectors.groupingBy(UserRiskProfileDTO::getRiskLevel));

        // 计算分布情况
        Map<String, Integer> activityDistribution = activityProfiles.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().size()
                ));

        Map<String, Integer> riskDistribution = riskProfiles.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().toString(),
                        e -> e.getValue().size()
                ));

        // 设置结果
        analysisResult.setUserActivityProfiles(activityProfiles);
        analysisResult.setUserRiskProfiles(riskProfiles);
        analysisResult.setActivityLevelDistribution(activityDistribution);
        analysisResult.setRiskLevelDistribution(riskDistribution);

        return analysisResult;
    }

    /**
     * 此方法用于返回活跃度排名前 N的用户
     * @param limit N值
     * @return List<UserActivityProfileDTO>
     */
    @Override
    public List<UserActivityProfileDTO> getTopActiveUsers(int limit) {
        // 计算时间范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minus(ANALYSIS_MONTHS, ChronoUnit.MONTHS);

        // 转换为 java.sql.Date
        Date endTime = Date.valueOf(endDate);
        Date startTime = Date.valueOf(startDate);

        // 获取总记录数
        long totalRecords = logininforMapper.countLoginRecordsInTimeRange(startTime, endTime);

        // 用于存储所有记录
        List<SysLogininfor> allLoginRecords = new ArrayList<>();

        // 分页查询数据
        for (int offset = 0; offset < totalRecords; offset += BATCH_SIZE) {
            List<SysLogininfor> batchRecords = logininforMapper.selectLoginRecordsWithPagination(
                    startTime, endTime, offset, BATCH_SIZE);
            allLoginRecords.addAll(batchRecords);
        }

        // 记录无效数据
        long invalidRecords = allLoginRecords.stream()
                .filter(record -> record.getUserName() == null || record.getUserName().trim().isEmpty())
                .count();

        // 按用户名分组处理数据
        Map<String, List<SysLogininfor>> userLoginRecordsMap = allLoginRecords.stream()
                .filter(record -> record.getUserName() != null && !record.getUserName().trim().isEmpty())
                .collect(Collectors.groupingBy(SysLogininfor::getUserName));

        // 计算每个用户的活跃度并排序
        return userLoginRecordsMap.entrySet().parallelStream()
                .map(entry -> createUserActivityProfile(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(UserActivityProfileDTO::getLoginFrequencyScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 此方法用于返回高风险用户
      * @return List<UserRiskProfileDTO>
     */
    @Override
    public List<UserRiskProfileDTO> getHighRiskUsers() {
        // 计算时间范围
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minus(ANALYSIS_MONTHS, ChronoUnit.MONTHS);

        // 转换为 java.sql.Date
        Date endTime = Date.valueOf(endDate);
        Date startTime = Date.valueOf(startDate);

        // 获取总记录数
        long totalRecords = logininforMapper.countLoginRecordsInTimeRange(startTime, endTime);

        // 用于存储所有记录
        List<SysLogininfor> allLoginRecords = new ArrayList<>();

        // 分页查询数据
        for (int offset = 0; offset < totalRecords; offset += BATCH_SIZE) {
            List<SysLogininfor> batchRecords = logininforMapper.selectLoginRecordsWithPagination(
                    startTime, endTime, offset, BATCH_SIZE);
            allLoginRecords.addAll(batchRecords);
        }

        // 记录无效数据
        long invalidRecords = allLoginRecords.stream()
                .filter(record -> record.getUserName() == null || record.getUserName().trim().isEmpty())
                .count();

        // 按用户名分组处理数据
        Map<String, List<SysLogininfor>> userLoginRecordsMap = allLoginRecords.stream()
                .filter(record -> record.getUserName() != null && !record.getUserName().trim().isEmpty())
                .collect(Collectors.groupingBy(SysLogininfor::getUserName));

        // 计算每个用户的风险等级，只返回高风险用户
        return userLoginRecordsMap.entrySet().stream()
                .map(entry -> createUserRiskProfile(entry.getKey(), entry.getValue()))
                .filter(profile -> profile.getRiskLevel() == RiskLevel.HIGH)
                .sorted(Comparator.comparing(UserRiskProfileDTO::getRiskScore).reversed())
                .collect(Collectors.toList());
    }

    // 辅助方法实现

    /**
     * 分值计算--用户活跃度评估
     * @param records
     * @return int
     * @author 李鸿麟
     * @since 2024/12/2     */
    private int calculateLoginFrequencyScore(List<SysLogininfor> records) {
        Map<String, List<SysLogininfor>> loginByDay = records.stream()
                .collect(Collectors.groupingBy(
                        record -> new SimpleDateFormat("yyyy-MM-dd")
                                .format(record.getLoginTime())
                ));
        return loginByDay.size() * 10;
    }

    /**
     * 分值计算--用户多ip登录
     * @param records
     * @return int
     * @author 李鸿麟
     * @since 2024/12/2     */
    private int calculateLocationDiversity(List<SysLogininfor> records) {
        Set<String> uniqueLocations = records.stream()
                .map(SysLogininfor::getLoginLocation)
                .collect(Collectors.toSet());
        return uniqueLocations.size() * 5;
    }

    /**
     * 分值计算--多设备登录
     * @param records 登录记录
     * @return int
     * @author 李鸿麟
     * @since 2024/12/2     */
    private int calculateDeviceDiversityScore(List<SysLogininfor> records) {
        Set<String> uniqueDevices = records.stream()
                .map(record -> record.getBrowser() + record.getOs())
                .collect(Collectors.toSet());
        return uniqueDevices.size() * 3;
    }

    /**
     * 用户活跃度评级
     * @param profile 用户活跃度档案
     * @return ActivityLevel
     * @author 李鸿麟
     * @since 2024/12/1     */
    private ActivityLevel assessActivityLevel(UserActivityProfileDTO profile) {
        int totalScore = profile.getLoginFrequencyScore() +
                (int)(profile.getLoginSuccessRate() * 100) +
                profile.getLoginLocationDiversity() +
                profile.getDeviceDiversityScore();

        if (totalScore > 200) {
            return ActivityLevel.HIGH;
        }
        if (totalScore > 100) {
            return ActivityLevel.MEDIUM;
        }
        return ActivityLevel.LOW;
    }

    /**
     * 检测异常登录
     * @param records 登录记录
     * @return int
     * @author 李鸿麟
     * @since 2024/12/2     */
    private int detectAbnormalLogins(List<SysLogininfor> records) {
        return (int) records.stream()
                .filter(record -> !"0".equals(record.getStatus()))
                .count();
    }

    /**
     * 计算用户不同ip
     * @param records 登录记录
     * @return int
     * @author 李鸿麟
     * @since 2024/12/1     */
    private int calculateUniqueIpCount(List<SysLogininfor> records) {
        return records.stream()
                .map(SysLogininfor::getIpaddr)
                .collect(Collectors.toSet())
                .size();
    }

    private RiskLevel assessRiskLevel(UserRiskProfileDTO profile) {
        int riskScore = profile.getAbnormalLoginCount() * 10 +
                profile.getUniqueIpCount() * 5 +
                profile.getLoginFailureCount() * 3;

        if (riskScore > 50) {
            return RiskLevel.HIGH;
        }
        if (riskScore > 20) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }




    /**
     * 计算非正常时间段的登录次数
     * @param records 登录记录
     * @return int
     * @author 李鸿麟
     * @since 2024/12/1     */
    private int calculateUnusualTimeLogins(List<SysLogininfor> records) {
        return (int) records.stream()
                .filter(record -> {
                    LocalDateTime loginTime = record.getLoginTime()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    int hour = loginTime.getHour();
                    // 判断是否在非正常时间段（例如：23:00-6:00）
                    return hour >= 23 || hour <= 6;
                })
                .count();
    }


    /**
     * 计算IP变化频率
     * @param records 登录记录
     * @return double
     * @author 李鸿麟
     * @since 2024/12/1     */
    private double calculateIpChangingRate(List<SysLogininfor> records) {
        if (records.size() <= 1) {
            return 0.0;
        }

        // 按时间排序
        List<SysLogininfor> sortedRecords = records.stream()
                .sorted(Comparator.comparing(SysLogininfor::getLoginTime))
                .collect(Collectors.toList());

        // 计算IP变化次数
        int ipChanges = 0;
        for (int i = 1; i < sortedRecords.size(); i++) {
            if (!sortedRecords.get(i).getIpaddr().equals(
                    sortedRecords.get(i-1).getIpaddr())) {
                ipChanges++;
            }
        }

        // 返回变化率
        return (double) ipChanges / (sortedRecords.size() - 1);
    }

    // 计算风险分数

    /**
     * 计算风险分数
     * @param profile 用户风险档案
     * @return int
     * @author 李鸿麟
     * @since 2024/12/1     */
    private int calculateRiskScore(UserRiskProfileDTO profile) {
        return
                profile.getAbnormalLoginCount() * 10 +
                profile.getUniqueIpCount() * 5 +
                profile.getLoginFailureCount() * 3 +
                profile.getUnusualTimeLoginCount() * 8 +
                (int)(profile.getIpChangingRate() * 100);
    }



    /**
     * 创建用户活跃度档案
     * @param userName 用户名
     * @param records 登录记录列表
     * @return UserActivityProfileDTO
     * @author 李鸿麟
     * @since 2024/12/1     */
    private UserActivityProfileDTO createUserActivityProfile(String userName, List<SysLogininfor> records) {
        UserActivityProfileDTO profile = new UserActivityProfileDTO();
        profile.setUserName(userName);
        profile.setTotalLoginCount(records.size());

        // 计算登录成功率
        long successCount = records.stream()
                .filter(record -> "0".equals(record.getStatus()))
                .count();
        profile.setLoginSuccessRate(records.isEmpty() ? 0 : successCount * 1.0 / records.size());

        // 计算其他指标
        profile.setLoginFrequencyScore(calculateLoginFrequencyScore(records));
        profile.setLoginLocationDiversity(calculateLocationDiversity(records));
        profile.setDeviceDiversityScore(calculateDeviceDiversityScore(records));

        // 设置最近登录时间
        if (CollUtil.isNotEmpty(records)) {
            profile.setLatestLoginTime(
                    new java.sql.Date(records.stream()
                            .max(Comparator.comparing(SysLogininfor::getLoginTime))
                            .get().getLoginTime().getTime())
            );
        }
        profile.setActivityLevel(assessActivityLevel(profile));
        return profile;
    }


    /**
     * 创建用户风险档案
     * @param userName 用户名
     * @param records 登录记录列表
     * @return UserRiskProfileDTO
     * @author 李鸿麟
     * @since 2024/12/1     */
    private UserRiskProfileDTO createUserRiskProfile(String userName, List<SysLogininfor> records) {
        UserRiskProfileDTO riskProfile = new UserRiskProfileDTO();
        riskProfile.setUserName(userName);

        // 计算异常登录次数
        int abnormalLoginCount = detectAbnormalLogins(records);
        riskProfile.setAbnormalLoginCount(abnormalLoginCount);

        // 计算唯一IP数量
        int uniqueIpCount = calculateUniqueIpCount(records);
        riskProfile.setUniqueIpCount(uniqueIpCount);

        // 计算登录失败次数
        int loginFailureCount = (int) records.stream()
                .filter(record -> !"0".equals(record.getStatus()))
                .count();
        riskProfile.setLoginFailureCount(loginFailureCount);

        // 计算时间异常登录次数（比如凌晨登录）
        int unusualTimeLoginCount = calculateUnusualTimeLogins(records);
        riskProfile.setUnusualTimeLoginCount(unusualTimeLoginCount);

        // 计算IP地址变化频繁程度
        double ipChangingRate = calculateIpChangingRate(records);
        riskProfile.setIpChangingRate(ipChangingRate);

        // 计算总体风险分数
        int riskScore = calculateRiskScore(riskProfile);
        riskProfile.setRiskScore(riskScore);

        // 评估风险等级
        riskProfile.setRiskLevel(assessRiskLevel(riskProfile));

        return riskProfile;
    }

    /**
     * 批量分析所有用户画像
     * @return Map<String, UserAuthenticityResult> 用户ID到分析结果的映射
     */
    @Override
    public Map<String, UserAuthenticityResult> analyzeAllUsersAuthenticity() {
        long pageSize = 100L;
        long current = 1L;
        Map<String, UserAuthenticityResult> resultMap = new ConcurrentHashMap<>();

        while (true) {
            Page<LandPlot> page = new Page<>(current, pageSize);
            IPage<String> userIdsPage = landPlotMapper.selectUserIdsPage(page);

            if (CollUtil.isEmpty(userIdsPage.getRecords())) {
                break;
            }

            // 批量获取用户信息
            List<SysUser> userList = sysUserMapper.selectUsersByIds(userIdsPage.getRecords());
            Map<String, SysUser> userMap = userList.stream()
                    .collect(Collectors.toMap(user -> String.valueOf(user.getUserId()), user -> user));

            // 并行处理每个用户
            userIdsPage.getRecords().parallelStream().forEach(userId -> {
                UserAuthenticityResult result = analyzeUserAuthenticity(userId);
                if (result != null) {
                    // 添加用户名和手机号
                    SysUser user = userMap.get(userId);
                    if (user != null) {
                        result.setUserName(user.getUserName());
                        result.setPhoneNumber(user.getPhonenumber());
                    }
                    resultMap.put(userId, result);
                }
            });

            if (current * pageSize >= userIdsPage.getTotal()) {
                break;
            }
            current++;
        }

        return resultMap;
    }

    /**
     * 单一用户画像分析
     * @param userId 用户id
     * @return UserAuthenticityResult
     */
    @Override
    public UserAuthenticityResult analyzeUserAuthenticity(String userId) {
        try {
            // 使用CompletableFuture进行并行计算
            CompletableFuture<Double> landMarkingFuture = CompletableFuture.supplyAsync(() ->
                    analyzeLandMarkingBehavior(userId));

            CompletableFuture<Double> trajectoryFuture = CompletableFuture.supplyAsync(() ->
                    analyzeTrajectoryBehavior(userId));

            CompletableFuture<Double> farmWorkFuture = CompletableFuture.supplyAsync(() ->
                    analyzeFarmWorkBehavior(userId));

            CompletableFuture<Double> machineryFuture = CompletableFuture.supplyAsync(() ->
                    analyzeMachineryBehavior(userId));

            // 等待所有异步任务完成
            CompletableFuture.allOf(landMarkingFuture, trajectoryFuture,
                    farmWorkFuture, machineryFuture).join();

            UserAuthenticityResult result = new UserAuthenticityResult();
            result.setUserId(userId);

            // 获取计算结果
            result.setLandMarkingScore(landMarkingFuture.join());
            result.setTrajectoryScore(trajectoryFuture.join());
            result.setFarmWorkScore(farmWorkFuture.join());
            result.setMachineryScore(machineryFuture.join());

            double overallScore = calculateOverallScore(result);
            result.setOverallScore(overallScore);
            result.setUserTags(generateUserTags(result));
            result.setUserType(overallScore >= 70 ? "真实农户" : "非农户");

            return result;
        } catch (Exception e) {
            throw new ServiceException("用户画像分析失败");
        }
    }

    /**
     * 分析圈地行为
     */
    private double analyzeLandMarkingBehavior(String userId) {
        // 使用MyBatis-Plus的Lambda查询
        List<LandPlot> landPlots = landPlotMapper.selectList(
                new LambdaQueryWrapper<LandPlot>()
                        .select(LandPlot::getLandArea)
                        .eq(LandPlot::getCreateBy, userId)
        );

        if (CollUtil.isEmpty(landPlots)) {
            return 0.0;
        }

        // 1. 计算地块数量得分（1-10个地块为正常）
        int plotCount = landPlots.size();
        // 计算面积得分
        double areaScore = landPlots.stream()
                .mapToDouble(this::calculateAreaReasonability)
                .average()
                .orElse(0.0);

        // 计算数量得分
        double countScore = plotCount > 0 && plotCount <= 10 ? 100.0 :
                plotCount > 10 ? 70.0 : 0.0;

        return (countScore * 0.4 + areaScore * 0.6);
    }

    /**
     * 计算地块面积合理性
     */
    private double calculateAreaReasonability(LandPlot landPlot) {
        double area = landPlot.getLandArea().doubleValue();

        // 假设正常农田面积在0.1-100亩之间
        if (area < 0.1 || area > 100) {
            return 0.0;
        }

        // 0.1-100亩之间按比例给分
        if (area <= 10) {
            return 100.0;
        } else if (area <= 50) {
            return 80.0;
        } else {
            return 60.0;
        }
    }

    /**
     * 分析轨迹行为
     */
    private double analyzeTrajectoryBehavior(String userId) {
        // 使用MyBatis-Plus的Lambda查询
        List<TrajectoryInfo> trajectories = trajectoryInfoMapper.selectList(
                new LambdaQueryWrapper<TrajectoryInfo>()
                        .eq(TrajectoryInfo::getCreatedBy, userId)
        );

        if (CollUtil.isEmpty(trajectories)) {
            return 0.0;
        }

        // 计算轨迹形状得分
        double shapeScore = trajectories.stream()
                .mapToDouble(this::analyzeTrajectoryShape)
                .average()
                .orElse(0.0);

        // 计算数量得分
        int trajectoryCount = trajectories.size();
        double countScore = trajectoryCount > 0 && trajectoryCount <= 10 ? 100.0 :
                trajectoryCount > 10 ? 70.0 : 0.0;

        return (countScore * 0.4 + shapeScore * 0.6);
    }

    /**
     * 分析轨迹形状规则性
     */
    private double analyzeTrajectoryShape(TrajectoryInfo trajectory) {
        try {
            // 检查轨迹是否完整
            LambdaQueryWrapper<TrajectoryPoints> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TrajectoryPoints::getTrajectoryId, trajectory.getTrajectoryId());
            List<TrajectoryPoints> trajectoryPoints = trajectoryPointsMapper.selectList(wrapper);

            if (CollUtil.isEmpty(trajectoryPoints)) {
                return 0.0;
            }

            // 检查每个轨迹点集合 如果没有轨迹、或者说轨迹点小余三个，那就证明这段轨迹是闹着玩的
            for (TrajectoryPoints point : trajectoryPoints) {
                if (point.getPoints() == null) {
                    return 0.0;
                }

                // 检查轨迹点的数量
                if (trajectoryPointsMapper.countPoints(point.getPoints()) <= 3) {
                    return 0.0;
                }

            }

            // 检查轨迹是否在地块范围内
            if (!isTrajectoryInPlot(trajectory)) {
                return 0.0;
            }

            // 基础分数
            return 80.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 分析农活发布行为
     */
    private double analyzeFarmWorkBehavior(String userId) {
        List<FarmWork> farmWorks = farmWorkMapper.selectList(
                new LambdaQueryWrapper<FarmWork>()
                        .select(FarmWork::getArea)
                        .eq(FarmWork::getCreateBy, userId)
        );

        if (CollUtil.isEmpty(farmWorks)) {
            return 0.0;
        }

        // 计算面积合理性得分
        double areaScore = farmWorks.stream()
                .mapToDouble(this::calculateFarmWorkAreaReasonability)
                .average()
                .orElse(0.0);

        // 计算数量得分
        int workCount = farmWorks.size();
        double countScore = workCount > 0 && workCount <= 10 ? 70.0 :
                workCount > 10 ? 100.0 : 0.0;

        return (countScore * 0.5 + areaScore * 0.5);
    }

    /**
     * 计算农活面积合理性
     */
    private double calculateFarmWorkAreaReasonability(FarmWork farmWork) {
        if (farmWork.getArea() == null) {
            return 0.0;
        }

        double area = farmWork.getArea().doubleValue();

        // 使用与地块相同的面积判断标准
        if (area < 0.1 || area > 100) {
            return 0.0;
        }

        if (area <= 10) {
            return 100.0;
        } else if (area <= 50) {
            return 80.0;
        } else {
            return 60.0;
        }
    }

    /**
     * 分析农机发布行为
     */
    private double analyzeMachineryBehavior(String userId) {
        // 使用MyBatis-Plus的Lambda查询
        List<AgriculturalMachinery> machineries = agriculturalMachineryMapper.selectList(
                new LambdaQueryWrapper<AgriculturalMachinery>()
                        .select(
                                AgriculturalMachinery::getTypeId,
                                AgriculturalMachinery::getPhoneNumber,
                                AgriculturalMachinery::getName,
                                AgriculturalMachinery::getAddressPrefix
                        )  // 只查询需要的字段
                        .eq(AgriculturalMachinery::getCreateBy, userId)
        );

        if (CollUtil.isEmpty(machineries)) {
            return 0.0;
        }

        // 计算信息完整性得分
        double infoScore = machineries.stream()
                .mapToDouble(this::calculateMachineryInfoCompleteness)
                .average()
                .orElse(0.0);

        // 计算数量得分
        int machineCount = machineries.size();
        double countScore = machineCount > 0 && machineCount <= 5 ? 100.0 :
                machineCount > 5 ? 70.0 : 0.0;

        return (countScore * 0.5 + infoScore * 0.5);
    }

    /**
     * 计算农机信息完整性
     */
    private double calculateMachineryInfoCompleteness(AgriculturalMachinery machinery) {
        int score = 0;

        if (machinery.getTypeId() != null) {
            score += 25;
        }
        if (machinery.getPhoneNumber() != null) {
            score += 25;
        }
        if (machinery.getName() != null) {
            score += 25;
        }
        if (machinery.getAddressPrefix() != null) {
            score += 25;
        }

        return score;
    }

    /**
     * 计算综合得分
     */
    private double calculateOverallScore(UserAuthenticityResult result) {
        return result.getLandMarkingScore() * LAND_WEIGHT +
                result.getTrajectoryScore() * TRAJECTORY_WEIGHT +
                result.getFarmWorkScore() * FARMWORK_WEIGHT +
                result.getMachineryScore() * MACHINERY_WEIGHT;
    }

    /**
     * 生成用户标签
     */
    private List<String> generateUserTags(UserAuthenticityResult result) {
        List<String> tags = new ArrayList<>();

        if (result.getLandMarkingScore() >= 80) {
            tags.add("规范圈地户");
        }
        if (result.getTrajectoryScore() >= 80) {
            tags.add("作业规范户");
        }
        if (result.getFarmWorkScore() >= 80) {
            tags.add("活跃农户");
        }
        if (result.getMachineryScore() >= 80) {
            tags.add("农机服务户");
        }

        if (result.getOverallScore() >= 90) {
            tags.add("金牌农户");
        } else if (result.getOverallScore() >= 80) {
            tags.add("银牌农户");
        } else if (result.getOverallScore() >= 70) {
            tags.add("铜牌农户");
        }

        return tags;
    }

    /**
     * 检查轨迹是否在地块范围内
     */
    private boolean isTrajectoryInPlot(TrajectoryInfo trajectory) {
        // 简化实现：检查轨迹的地块ID是否存在
        return trajectory.getPlotId() != null;
    }

}
