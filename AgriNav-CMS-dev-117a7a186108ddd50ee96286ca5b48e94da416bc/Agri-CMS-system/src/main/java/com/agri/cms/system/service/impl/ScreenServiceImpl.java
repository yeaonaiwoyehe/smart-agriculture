package com.agri.cms.system.service.impl;

import cn.hutool.Hutool;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.agri.cms.common.constant.CacheConstants;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.common.exception.screen.ParamValidationException;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.common.utils.uuid.UUID;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.dto.ScreenDTO;
import com.agri.cms.system.domain.vo.*;
import com.agri.cms.system.mapper.*;
import com.agri.cms.system.service.IScreenService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.elasticsearch.action.admin.indices.template.post.SimulateTemplateAction;
import org.jetbrains.annotations.NotNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 大屏服务实现类
 *
 * @author 李鸿麟
 */
@Service
public class ScreenServiceImpl extends ServiceImpl<ScreenMapper, ScreenStatistics> implements IScreenService {

    @Resource
    private ScreenMapper screenMapper;

    @Resource
    private CropMapper cropMapper;

    @Resource
    private FieldWorkMapper fieldWorkMapper;

    @Resource
    private RedisCache redisCache;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private FarmWorkInfoMapper farmWorkInfoMapper;

    @Resource
    LandPlotMapper landPlotMapper;

    @Resource
    LandAddressMapper landAddressMapper;

    
    Map<String, Integer> provinceCount;

    Map<String, Integer> provinceTaskCount;

    // 定义所有省级区划列表
    private List<String> provinces = new ArrayList<>();

    private Map<String, String> municipality = new HashMap<>();


    /**
     * 初始化省份列表
     * @author 李鸿麟
     * @since 2024/11/24     */
    @PostConstruct
    public void initProvinces() {
        // 1.首先去数据库里面查出来所有的省份和直辖市
        LambdaQueryWrapper<LandAddress> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LandAddress::getLevel, 1);
        List<LandAddress> addresses = landAddressMapper.selectList(queryWrapper);
        // 2.初始化对应的变量，避免因政策变化导致数据出现变动
        addresses.forEach(address -> {
            provinces.add(address.getName());
            municipality.put(address.getCode().toString(), address.getName() );
        });
    }


    /**
     * 此方法用于返回发布任务最多的省份
     * @return ProvinceVO
     */
    @Override
    public ProvinceVO getProvinceWithMostFarmWork() {
        // 1.先从缓存中获取数据
        ProvinceVO provinceVO = redisCache.getCacheObject(CacheConstants.PROVINCE_WITH_MOST_FARM_WORK);
        if (provinceVO != null) {
            return provinceVO;
        }
        try {
            // 2.如果没有，再进行查询
            List<ScreenTaskVO> farmWorkStatistics = getFarmWorkStatistics();

            // 初始化省份任务数量，默认所有的省份的发布农活数量为0
            provinceTaskCount = new HashMap<>();
            provinces.forEach(province -> provinceTaskCount.put(province, 0));

            // 利用之前任务详情的接口先查询出当前数据库中所有未被删除的任务，提取出省份名称
            farmWorkStatistics.forEach(statistic -> {
                String address = statistic.getTaskAddress();
                if (StringUtils.isNotEmpty(address)) {
                    String extractedProvinceName = extractProvince(address);
                    provinceTaskCount.put(extractedProvinceName, provinceTaskCount.getOrDefault(extractedProvinceName, 0) + 1);
                }
            });

            // 开始排序，找出农活数量最多的省份
            provinceVO = provinceTaskCount.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(entry -> {
                        ProvinceVO vo = new ProvinceVO();
                        vo.setName(entry.getKey());
                        vo.setValue(entry.getValue());
                        return vo;
                    })
                    .orElse(new ProvinceVO());

            // 将结果存入缓存，并设置缓存时间为1天
            redisCache.setCacheObject(CacheConstants.PROVINCE_WITH_MOST_FARM_WORK, provinceVO, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);

            return provinceVO;
        } catch (Exception e) {
            // 3.如果发生异常，则返回一个空值
            log.error(e.getMessage());
            return new ProvinceVO();
        }
    }

    /**
     * 此方法用于返回工作面积最大的田地工作类型
     * @return ScreenStatisticsVO
     */
    @Override
    public ScreenStatisticsVO getFieldWorkTypeWithBiggestArea() {
        // 1.先从缓存里面拿数据
        List<ScreenStatisticsVO> fieldWorkStatistics = getFieldWorkStatistics();

        // 2.按照工作总面积进行排序
        // 这个地方提一嘴为什么我没有像其他类似的方法一样使用try-catch保证这块代码一定能够查到数据，因为我是直接调用的方法，调用的方法里已经包含过try-catch了
        if (fieldWorkStatistics != null) {
            return fieldWorkStatistics.stream()
                    .max(Comparator.comparing(ScreenStatisticsVO::getTotalArea))
                    .orElse(new ScreenStatisticsVO());
        }
        return new ScreenStatisticsVO();
    }


    /**
     * 此方法用于返回工作次数最多的田地工作类型
     * @return ScreenStatisticsVO
     */
    @Override
    public ScreenStatisticsVO getFieldWorkTypeWithBiggestCountNum() {
        // 1.先从缓存里面拿数据
        List<ScreenStatistics> fieldWorkStatistics = redisCache.getCacheObject(Constants.SCREEN_REDIS_KEY);

        // 2.处理数据，依据工作类型进行分组，然后对于工作次数进行累加，最后返回最大的那个
        Map.Entry<String, Integer> stringIntegerEntry = fieldWorkStatistics.stream()
                .collect(Collectors.groupingBy(
                        ScreenStatistics::getWorkName,
                        Collectors.summingInt(ScreenStatistics::getWorkCount)
                ))
                .entrySet()
                .stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .get();

        if (stringIntegerEntry != null) {
            ScreenStatisticsVO vo = new ScreenStatisticsVO();
            vo.setWorkName(stringIntegerEntry.getKey());
            vo.setTotalArea(BigDecimal.valueOf(stringIntegerEntry.getValue()));
            return vo;
        }

        return new ScreenStatisticsVO();
    }


    /**
     * 此方法用于返回最热门的作物信息
     * @return ScreenStatisticsVO
     */
    @Override
    public ScreenStatisticsVO getHottestCrop() {
        try {
            // 1. 从缓存获取数据
            List<ScreenStatistics> cropStatistics = redisCache.getCacheObject(Constants.SCREEN_REDIS_KEY);


            if (cropStatistics.isEmpty()) {
                log.warn("No crop statistics found in cache");
                return new ScreenStatisticsVO();
            }

            // 2. 按作物名称分组，计算总面积
            Map.Entry<String, BigDecimal> result = cropStatistics.stream()
                    .filter(stat -> stat.getCropName() != null && stat.getTotalArea() != null)
                    .collect(Collectors.groupingBy(
                            ScreenStatistics::getCropName,
                            Collectors.reducing(
                                    BigDecimal.ZERO,
                                    ScreenStatistics::getTotalArea,
                                    BigDecimal::add
                            )
                    ))
                    .entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            // 3. 转换结果
            if (result != null) {
                ScreenStatisticsVO vo = new ScreenStatisticsVO();
                vo.setWorkName(result.getKey());
                vo.setTotalArea(result.getValue());
                return vo;
            }

            return new ScreenStatisticsVO();
        } catch (Exception e) {
            log.error("Error in finding the hottest crop", e);
            return new ScreenStatisticsVO();
        }
    }


    /**
     * 此方法用于统计各个作物的工作总亩数
     * @return List<ProvinceVO>
     */
    @Override
    public List<ProvinceVO> getCropDetails() {
        // 1.先从redis里拿数据
        List<ScreenStatistics> cropStatistics = redisCache.getCacheObject(Constants.SCREEN_REDIS_KEY);

        // 2.转换数据，统计出cropStatistics中每个作物的总亩数
        Map<String, BigDecimal> collect = cropStatistics.stream()
                .filter(stat -> stat.getCropName() != null && stat.getTotalArea() != null)
                .collect(Collectors.groupingBy(
                        ScreenStatistics::getCropName,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                ScreenStatistics::getTotalArea,
                                BigDecimal::add
                        )
                ));

        return collect.entrySet()
                .stream()
                .map(entry -> {
                    ProvinceVO vo = new ProvinceVO();
                    vo.setName(entry.getKey());
                    vo.setValue(entry.getValue().intValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }


    /**
     * 此方法用于统计每个月的地块轨迹的数量
     * @return List<ProvinceVO>
     */
    @Override
    @DS("pg")
    public List<ProvinceVO> getTrajectoryCountsByMonth() {

        List<ProvinceVO> cacheList = redisCache.getCacheObject(Constants.EVERY_MONTH_TRAJECTORY_COUNT);
        if (cacheList != null) {
            return cacheList;
        }

        List<Map<String, Object>> maps = screenMapper.countTrajectoriesByMonth();
        List<ProvinceVO> result = new ArrayList<>();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM月");

        // 获取当前年份
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        maps.stream().forEach(item -> {
            try {
                Date date = inputFormat.parse(item.get("month").toString());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);

                // 检查年份是否为当前年份
                if (calendar.get(Calendar.YEAR) == currentYear) {
                    ProvinceVO vo = new ProvinceVO();
                    String formattedMonth = outputFormat.format(date);
                    vo.setName(formattedMonth);
                    vo.setValue(Integer.parseInt(item.get("count").toString()));
                    result.add(vo);

                }

                // 添加结束后，把结果放到redis里
                redisCache.setCacheObject(Constants.EVERY_MONTH_TRAJECTORY_COUNT, result);
                redisCache.expire(Constants.EVERY_MONTH_TRAJECTORY_COUNT, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);
            } catch (Exception e) {
                log.error("Error parsing date: " + item.get("month"));
                throw new RuntimeException(e);
            }
        });
        return result;
    }

    /**
     * 此方法用于返回每个月的地块数量
     * @return List<ProvinceVO>
     */
    @Override
    public List<ProvinceVO> getLandPlotCountsByMonth() {
        List<ProvinceVO> cacheObject = redisCache.getCacheObject(Constants.EVERY_MONTH_LANDPLOT_COUNT);
        if (cacheObject != null) {
            return cacheObject;
        }

        List<Map<String, Object>> maps = screenMapper.countLanPlotsByMonth();
        List<ProvinceVO> result = new ArrayList<>();

        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
        SimpleDateFormat outputFormat = new SimpleDateFormat("MM月");

        // 获取当前年份
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        maps.stream().forEach(item -> {
            try {
                Date date = inputFormat.parse(item.get("month").toString());
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);

                // 检查年份是否为当前年份
                if (calendar.get(Calendar.YEAR) == currentYear) {
                    ProvinceVO vo = new ProvinceVO();
                    String formattedMonth = outputFormat.format(date);
                    vo.setName(formattedMonth);
                    vo.setValue(Integer.parseInt(item.get("count").toString()));
                    result.add(vo);
                }

                redisCache.setCacheObject(Constants.EVERY_MONTH_LANDPLOT_COUNT, result);
                redisCache.expire(Constants.EVERY_MONTH_LANDPLOT_COUNT, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    /**
     * 此方法统计所有地块总面积
     * @return ProvinceVO
     */
    @Override
    public LandPlotAreaVO countLandPlotsTotalArea() {
        // 1.从缓存获取数据
        LandPlotAreaVO cacheData = redisCache.getCacheObject(Constants.LANDPLOTS_TOTAL_AREA);
        if (cacheData != null) {
            return cacheData;
        }

        // 2.缓存不存在,从数据库查询并计算
        try {
            // 使用数据库sum函数直接计算总面积
            Float totalArea = landPlotMapper.sumLandArea();

            // 构建返回对象
            cacheData = new LandPlotAreaVO();
            cacheData.setLandArea(totalArea != null ? totalArea : 0.0f);

            // 设置缓存,添加过期时间
            redisCache.setCacheObject(Constants.LANDPLOTS_TOTAL_AREA, cacheData);
            redisCache.expire(Constants.LANDPLOTS_TOTAL_AREA, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);
            return cacheData;
        } catch (Exception e) {
            log.error("计算土地面积总和失败", e);
            throw new RuntimeException("获取土地面积数据失败", e);
        }
    }

    /**
     * 此方法用于返回某个省份下级行政区划的地块数量
     * @param provinceCode 省份编码
     * @return List<ProvinceVO>
     */
    @Override
    public List<ProvinceVO> getOneProvinceLandPlotCounts(String provinceCode) {
        // 1.首先判断传进来的编码是不是直辖市，如果是直辖市，需要走直辖市的接口
        if (StrUtil.isEmpty(provinceCode)) {
            throw new ParamValidationException(501, "省份编码为空");
        }
        if (municipality.containsKey(provinceCode) &&
                StrUtil.contains(municipality.get(provinceCode), "市")) {
            List<ProvinceVO> result = screenMapper.countLandPlotCountsInMunicipality(provinceCode);
            return result;
        }

        // 2.如果不是直辖市，从数据库里面查，但是针对湖北省、海南省、新疆维吾尔自治区需要特别处理
        // 2.1 处理湖北、新疆、海南三个特殊省份
        if (StrUtil.equalsAny(provinceCode, Constants.HUBEI_CODE, Constants.XINJIANG_CODE, Constants.HAINAN_CODE)) {
            List<ScreenDTO> list = screenMapper.selectProvinceNamesAndCodes(provinceCode);
            List<ProvinceVO> result = new ArrayList<>();
            list.forEach(item -> {
                if (StrUtil.contains(item.getName(), Constants.SPECIAL_DISCTRICT)) {
                    List<ProvinceVO> provinceVOS = screenMapper.countLandPlotCountsInHubeiHainanXinjiang(item.getValue());
                    result.addAll(provinceVOS);
                }
            });
            result.addAll(screenMapper.countLandPlotCountsInDetailedProvince(provinceCode));
            return result;
        }
        List<ProvinceVO> result = screenMapper.countLandPlotCountsInDetailedProvince(provinceCode);
        if (CollUtil.isNotEmpty(result)) {
            return result;
        }
        // 3.为了保证数据的时效性，5min刷新一次
        return null;
    }


    /**
     * 统计地块数量最多的省份
     * @return ProvinceVO
     * @author 李鸿麟
     * @since 2024/11/11
     */
    public ProvinceVO getProvinceWithMostPlots() {
        return provinceCount.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> {
                    ProvinceVO provinceVO = new ProvinceVO();
                    provinceVO.setName(entry.getKey());
                    provinceVO.setValue(entry.getValue());
                    return provinceVO;
                })
                .orElse(new ProvinceVO());
    }


    /**
     * 统计数量前n多的省份
     * @param n 限制多少条记录
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/11
     */
    public List<ProvinceVO> getTopNProvinces(int n) {
        return provinceCount.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(n)
                .map(entry -> {
                    ProvinceVO provinceVO = new ProvinceVO();
                    provinceVO.setName(entry.getKey());
                    provinceVO.setValue(entry.getValue());
                    return provinceVO;
                })
                .collect(Collectors.toList());
    }


    /**
     * 此方法用于返回各个省份包含的地块数
     * @return List<ProvinceVO>
     * @author 李鸿麟
     * @since 2024/11/11
     */
    public List<ProvinceVO> getLandPlotsWithinProvince() {

         List<ProvinceVO> provinceLandPlotList = redisCache.getCacheObject(CacheConstants.LAND_PLOT_COUNT_BY_PROVINCE);
        if (provinceLandPlotList != null) {
            return provinceLandPlotList;
        }

        try {
            // 1.查询有效的地块
            LambdaQueryWrapper<LandPlot> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.ne(LandPlot::getIsDeleted, Constants.DELETED);
            List<LandPlot> landPlots = landPlotMapper.selectList(queryWrapper);

            // 2.统计每个省份里有多少个地块

            // 用HashMap可以自动去重，更方便
            provinceCount = new HashMap<>();
            provinces.forEach(province -> provinceCount.put(province, 0));

            landPlots.stream().forEach(landPlot -> {
                if (StringUtils.isNotEmpty(landPlot.getAddress())) {
                    String province = extractProvince(landPlot.getAddress());
                    provinceCount.put(province, provinceCount.getOrDefault(province, 0) + 1);
                }
            });


            provinceLandPlotList = new ArrayList<>(provinceCount.entrySet().stream()
                    .map(entry -> {
                        ProvinceVO vo = new ProvinceVO();
                        vo.setName(entry.getKey());
                        vo.setValue(entry.getValue());
                        return vo;
                    })
                    .collect(Collectors.toList()));

            redisCache.setCacheObject(CacheConstants.LAND_PLOT_COUNT_BY_PROVINCE, provinceLandPlotList);
            redisCache.expire(CacheConstants.LAND_PLOT_COUNT_BY_PROVINCE, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);

            return provinceLandPlotList;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 此方法用于从地址中提取省级区划
     * @param address 完整地址
     * @return String
     * @author 李鸿麟
     * @since 2024/11/11
     */
    private  String extractProvince(String address) {
        if (address == null) {
            return null;
        }

        // 处理自治区
        for (String province : provinces) {
            if (province.contains("自治区") && address.startsWith(province.substring(0, 2))) {
                return province;
            }
        }

        // 处理特别行政区
        for (String province : provinces) {
            if (province.contains("特别行政区") && address.startsWith(province.substring(0, 2))) {
                return province;
            }
        }

        // 处理直辖市
        for (String province : provinces) {
            if (province.endsWith("市") && address.startsWith(province.substring(0, 2))) {
                return province;
            }
        }

        // 处理普通省份
        for (String province : provinces) {
            if (province.endsWith("省") && address.startsWith(province.substring(0, 2))) {
                return province;
            }
        }

        return null;
    }

    /**
     * 获取工作情况统计数据 例如播种了多少多少亩
     * @return List<ScreenStatisticsVO>
     */
    @Override
    public List<ScreenStatisticsVO> getFieldWorkStatistics() {
        // 1. 首先检查Redis缓存
        List<ScreenStatistics> cacheData = redisCache.getCacheObject(Constants.SCREEN_REDIS_KEY);
        if (cacheData != null && !cacheData.isEmpty()) {
            // 如果缓存存在，处理并返回缓存数据
            return transformData(cacheData, 5);
        }

        // 2. 如果缓存不存在，查询数据库
        try {
            // 构建查询条件, 因为定时任务并不是凌晨一点准时执行, 而是会略微延迟一会儿
            LocalDateTime startTime = LocalDateTime.now().toLocalDate().atTime(1, 0);
            LocalDateTime endTime = startTime.plusMinutes(1);

            LambdaQueryWrapper<ScreenStatistics> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.ge(ScreenStatistics::getLastUpdateTime, startTime)
                    .lt(ScreenStatistics::getLastUpdateTime, endTime).orderByAsc(ScreenStatistics::getTotalArea);

            // 查询数据库
            Page<ScreenStatistics> page = new Page<>(1, 5, false);
            List<ScreenStatistics> screenStatistics = screenMapper.selectPage(page, queryWrapper).getRecords();

            // 处理数据, 将screenStatics中的日期转换成形似2024-01-01格式
            screenStatistics.forEach(statistics -> {
                statistics.setLastUpdateTime(statistics.getLastUpdateTime().toLocalDate().atStartOfDay());
            });


            // 如果查询结果不为空，放入缓存
            if (screenStatistics != null && !screenStatistics.isEmpty()) {
                redisCache.setCacheObject(Constants.SCREEN_REDIS_KEY, screenStatistics);
                redisCache.expire(Constants.SCREEN_REDIS_KEY, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);
            }

            // 转换并返回数据
            return transformData(screenStatistics);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 转换数据的方法，将数据库中status存的数字转换成汉字
     * @param cacheData 从缓存里拿出来的数据
     * @return List<ScreenStatisticsVO>
     * @author 李鸿麟
     * @since 2024/11/9
     */
    @NotNull
    private List<ScreenStatisticsVO> transformData(List<ScreenStatistics> cacheData) {
        Map<String, BigDecimal> workAreaMap = cacheData.stream()
                .collect(Collectors.toMap(
                        ScreenStatistics::getWorkName,
                        ScreenStatistics::getTotalArea,
                        BigDecimal::add
                ));

        return workAreaMap.entrySet().stream()
                .map(entry -> {
                    ScreenStatisticsVO vo = new ScreenStatisticsVO();
                    vo.setWorkName(entry.getKey());
                    vo.setTotalArea(entry.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @NotNull
    private List<ScreenStatisticsVO> transformData(List<ScreenStatistics> cacheData, int n) {
        Map<String, BigDecimal> workAreaMap = cacheData.stream()
                .collect(Collectors.toMap(
                        ScreenStatistics::getWorkName,
                        ScreenStatistics::getTotalArea,
                        BigDecimal::add
                ));

        return workAreaMap.entrySet().stream()
                // 按照TotalArea降序排序
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                // 限制返回前n条数据
                .limit(n)
                .map(entry -> {
                    ScreenStatisticsVO vo = new ScreenStatisticsVO();
                    vo.setWorkName(entry.getKey());
                    vo.setTotalArea(entry.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取农机任务详情,用于实时展示
     */
    @Override
    public List<ScreenTaskVO> getFarmWorkStatistics() {
        // 先从redis里查询是否有数据 如果有的话就直接返回 如果没有的话再查询数据库
        List<ScreenTaskVO> screenAgricultureMachineryTask = redisCache.getCacheObject(CacheConstants.AGRICULTURE_MACHINERY_STATISTICS_KEY);

        if (screenAgricultureMachineryTask != null) {
            return screenAgricultureMachineryTask;
        }

        return getFarmWorkTaskVO();
    }

    /**
     * 与getFarmWorkStatistics配套的方法，保证能查询到数据
     * @return ScreenTaskVO
     * @author 李鸿麟
     * @since 2024/10/31
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @DS("pg")
    public List<ScreenTaskVO> getFarmWorkTaskVO() {

        // 1. 多表查询
        List<ScreenTaskVO> taskList = screenMapper.selectScreenList();

        // 2. 转换一下taskList里的status属性，变成能够在大屏上展示的文字，同时在转换一下日期
        taskList.forEach(task -> {
            switch (task.getStatus()) {
                case Constants.AGRICULTURAL_MACHINERY_UNFINISHED_STATUS:
                    task.setStatus(Constants.AGRICULTURAL_MACHINERY_UNFINISHED_STATEMENT);
                    break;
                case Constants.AGRICULTURAL_MACHINERY_FINISHED_STATUS:
                    task.setStatus(Constants.AGRICULTURAL_MACHINERY_FINISHED_STATEMENT);
                    break;
                default:
                    task.setStatus(Constants.AGRICULTURAL_MACHINERY_UNFINISHED_STATEMENT);
            }
        });

        // 3. 结果放redis里

        // 先查一下之前在redis里数据删除了没，如果没被删除就先删掉
        if (redisCache.getCacheObject(CacheConstants.AGRICULTURE_MACHINERY_STATISTICS_KEY) != null) {
            redisCache.deleteObject(CacheConstants.AGRICULTURE_MACHINERY_STATISTICS_KEY);
        }
        // 放到redis里
        redisCache.setCacheObject(CacheConstants.AGRICULTURE_MACHINERY_STATISTICS_KEY, taskList);
        redisCache.expire(CacheConstants.AGRICULTURE_MACHINERY_STATISTICS_KEY, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);

        // 4. 返回
        return taskList;
    }


    /**
     * 获取当日活跃用户占用户总数的百分比
     */
    @Override
    public ScreenPercentVO getUserStatistics() {
        // 先从redis里查询是否有数据 如果有的话就直接返回 如果没有的话再查询数据库
        ScreenPercentVO screenUserStatistics = redisCache.getCacheObject(CacheConstants.USER_STATISTICS_KEY);
        return Objects.requireNonNullElseGet(screenUserStatistics, this::getTotalUserCount);
    }


    /**
     * 和getUserStatistics方法配套的方法
     * @return ScreenTaskVO
     * @author 李鸿麟
     * @since 2024/10/31
     */
    private ScreenPercentVO getTotalUserCount() {
        // 构造查询条件
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(SysUser::getDelFlag, Constants.DELETED);

        // 查询总数
        int totalUserCount = sysUserMapper.selectCount(queryWrapper);
        Long activeUserCount = redisCache.pfcount(CacheConstants.DAU_KEY_PREFIX);

        // 创建统计结果对象
        ScreenPercentVO percentVO = new ScreenPercentVO();

        // 计算百分比并保留两位小数
        BigDecimal percent = BigDecimal.valueOf((activeUserCount * 1.0 / totalUserCount) * 100)
                .setScale(2, RoundingMode.HALF_UP);
        percentVO.setPercent(percent.doubleValue());

        // 把结果存到redis里
        redisCache.setCacheObject(CacheConstants.USER_STATISTICS_KEY, percentVO);
        redisCache.expire(CacheConstants.USER_STATISTICS_KEY, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);

        return percentVO;
    }

    /**
     * 获取当前已完成任务的百分比
     * @return ScreenPercentVO
     */
    @Override
    public ScreenPercentVO getFarmWorkTaskStatistics() {
        ScreenPercentVO screenTaskStatistics = redisCache.getCacheObject(CacheConstants.TASK_STATISTICS_KEY);
        if (screenTaskStatistics != null) {
            return screenTaskStatistics;
        }
        return getTotalTaskCount();
    }


    /**
     * 与getFarmWorkTaskStatistics配套的方法，每日一点钟定时更新数据并放到Redis里
     * @return ScreenPercentVO
     * @author 李鸿麟
     * @since 2024/11/10
     */
    private ScreenPercentVO getTotalTaskCount() {
        LambdaQueryWrapper<FarmWork> totalCountWrapper = new LambdaQueryWrapper<>();
        totalCountWrapper.ne(FarmWork::getStatus, Constants.FARM_WORK_DELETE_STATUS_STATEMENT);
        LambdaQueryWrapper<FarmWork> activeCountWrapper = new LambdaQueryWrapper<>();
        activeCountWrapper.eq(FarmWork::getStatus, Integer.parseInt(Constants.AGRICULTURAL_MACHINERY_FINISHED_STATUS));

        // 查询总数与已完成任务的数量
        Long totalWorkCount = farmWorkInfoMapper.selectCount(totalCountWrapper);
        Long finishedWorkCount = farmWorkInfoMapper.selectCount(activeCountWrapper);

        // 创建统计结果对象
        ScreenPercentVO percentVO = new ScreenPercentVO();

        // 计算百分比并保留两位小数
        BigDecimal percent = BigDecimal.valueOf((finishedWorkCount * 1.0 / totalWorkCount) * 100)
                .setScale(2, RoundingMode.HALF_UP);
        percentVO.setPercent(percent.doubleValue());

        // 把统计的结果存储到redis中
        redisCache.setCacheObject(CacheConstants.TASK_STATISTICS_KEY, percentVO);
        redisCache.expire(CacheConstants.TASK_STATISTICS_KEY, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);

        return percentVO;
    }


    /**
     * 此方法为定时任务,每天凌晨一点钟统计各个作物的工作情况,并写入到数据库中
     * @author 李鸿麟
     * @since 2024/10/29
     */

//    @Scheduled(fixedDelay = 60000)
    @Scheduled(cron = "0 0 1 * * ?", zone = "Asia/Shanghai")
    @DS("pg")
    @DSTransactional
    public void updateScreenStatistics() {

        // 1.查询基础统计数据
        List<ScreenStatistics> screenStatisticsList = screenMapper.getScreenStatistics();

        // 2.获取所有需要查询的ID
        List<Long> cropIds = screenStatisticsList.stream()
                .map(ScreenStatistics::getCropId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> workIds = screenStatisticsList.stream()
                .map(ScreenStatistics::getWorkId)
                .distinct()
                .collect(Collectors.toList());

        // 3.使用IN条件查询
        QueryWrapper<Crop> cropWrapper = new QueryWrapper<>();
        cropWrapper.in("id", cropIds);

        QueryWrapper<FieldWork> workWrapper = new QueryWrapper<>();
        workWrapper.in("id", workIds);

        List<Crop> crops = cropMapper.selectList(cropWrapper);
        List<FieldWork> works = fieldWorkMapper.selectList(workWrapper);

        // 4.转换为Map提高查询效率
        Map<Long, String> cropMap = crops.stream()
                .collect(Collectors.toMap(Crop::getId, Crop::getCropName));
        Map<Long, String> workMap = works.stream()
                .collect(Collectors.toMap(FieldWork::getId, FieldWork::getWorkName));

        // 5.设置名称和更新时间
        screenStatisticsList.forEach(screenStatistics -> {
            screenStatistics.setId(UUID.fastUUID().toString(true));
            screenStatistics.setCropName(cropMap.get(screenStatistics.getCropId()));
            screenStatistics.setWorkName(workMap.get(screenStatistics.getWorkId()));
            screenStatistics.setLastUpdateTime(LocalDateTime.now());
        });

        // 6.批量保存到数据库
        try {
            screenMapper.insert(screenStatisticsList);
        } catch (Exception e) {
            log.error("更新数据库失败", e);
            throw new RuntimeException(e);
        }

        // 7.把更新的数据放到redis里,现阶段默认过期时间为24h
        try {
            String redisKey = Constants.SCREEN_REDIS_KEY;
            redisCache.setCacheObject(redisKey, screenStatisticsList, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);
            redisCache.expire(redisKey, Constants.SCREEN_REFRESH_TIME, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("更新redis失败", e);
            throw new RuntimeException(e);
        }
    }


}

