package com.agri.cms.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.utils.bean.BeanUtils;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.dto.FarmWorkDTO;
import com.agri.cms.system.domain.vo.FarmWorkVO;
import com.agri.cms.system.service.*;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import com.agri.cms.common.utils.uuid.UUID;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 */
@Service
public class FarmWorkServiceImpl implements FarmWorkService {
    @Autowired
    private FarmWorkCropService farmWorkCropService;
    @Autowired
    private FarmWorkInfoService farmWorkInfoService;
    @Autowired
    private FarmWorkLandPlotService farmWorkLandPlotService;
    @Autowired
    private ICropService cropService;
    @Autowired
    private LandPlotService landPlotService;
    @Autowired
    private IFieldWorkService workTypeService;
    @Autowired
    private FarmWorkTypeService farmWorkTypeService;
    @Autowired
    private WorkTypeService uniqueWorkTypeService;
    @Autowired
    private SysUserServiceImpl userService;
    private static final int DELETE = 2;

    @Override
    @DS("pg")
    @Transactional(rollbackFor = Exception.class)
    public void insert(FarmWorkDTO farmWorkDTO, String userId) {
        UUID uuid = UUID.fastUUID();
        //获取FarmWork对象
        FarmWork farmWork = farmWorkDTO.toFarmWork(uuid);
        //设置状态
        farmWork.setStatus(Constants.AVAILABLE);
        //设置创建人
        farmWork.setCreateBy(userId);
        //设置创建时间
        farmWork.setCreateTime(LocalDateTime.now());
        //获取FarmWorkCrop集合
        List<FarmWorkCrop> farmWorkCrop = farmWorkDTO.toFarmWorkCrop(uuid);
        //获取FarmWorkLandPlot集合
        List<FarmWorkLandPlot> farmWorkLandPlot = farmWorkDTO.toFarmWorkLandPlot(uuid);
        //获取FarmWorkType集合
        List<FarmWorkType> farmWorkType = farmWorkDTO.toFarmWorkType(uuid);
        farmWorkInfoService.save(farmWork);
        farmWorkCropService.saveBatch(farmWorkCrop);
        farmWorkLandPlotService.saveBatch(farmWorkLandPlot);
        farmWorkTypeService.saveBatch(farmWorkType);
    }

    @Override
    @DS("pg")
    public void deleteById(String id) {
        LambdaUpdateWrapper<FarmWork> wrapper = new LambdaUpdateWrapper<>();
        //设置下架状态
        wrapper.set(FarmWork::getStatus, Constants.NO_LONGER_AVAILABLE);
        //根据id条件
        wrapper.eq(FarmWork::getId, id);
        farmWorkInfoService.update(wrapper);
    }

    @Override
    @DS("pg")
    public void update(FarmWorkDTO farmWorkDTO, String userId) {
        //将FarmWorkDTO转换为FarmWork对象
        FarmWork farmWork = farmWorkDTO.toFarmWork();
        //将FarmWorkDTO转为FarmWorkCrop集合
        List<FarmWorkCrop> farmWorkCrop = farmWorkDTO.toFarmWorkCrop();
        //将FarmWorkDTO转为FarmWorkLandPlot集合
        List<FarmWorkLandPlot> farmWorkLandPlot = farmWorkDTO.toFarmWorkLandPlot();
        //将FarmWorkDTO转为FarmWorkType集合
        List<FarmWorkType> farmWorkType = farmWorkDTO.toFarmWorkType();
        String id = farmWork.getId();
        //更新农活信息
        farmWork.setUpdateBy(userId);
        farmWork.setUpdateTime(LocalDateTime.now());
        //更新农活作物信息
        updateFarmWorkCrop(id, farmWorkCrop);
        //更新农活地块信息
        updateFarmWorkLandPlot(id, farmWorkLandPlot);
        //更新农活作业类型信息
        updateFarmWorkType(id, farmWorkType);
        farmWorkInfoService.updateById(farmWork);
    }

    private void updateFarmWorkType(String id, List<FarmWorkType> farmWorkType) {
        LambdaQueryWrapper<FarmWorkType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FarmWorkType::getFarmWorkId, id);
        farmWorkTypeService.remove(wrapper);
        farmWorkTypeService.saveBatch(farmWorkType);
    }

    @Override
    public AjaxResult list(FarmWorkDTO farmWorkDTO, String userId, int pageNum, int pageSize) {
        // 1. 构建基础查询条件
        var queryWrapper = new LambdaQueryWrapper<FarmWork>();
        queryWrapper
                .ne(FarmWork::getStatus, DELETE)
                .like(farmWorkDTO.getName() != null, FarmWork::getName, farmWorkDTO.getName())
                .le(farmWorkDTO.getMaxArea() != null, FarmWork::getArea, farmWorkDTO.getMaxArea())
                .ge(farmWorkDTO.getMinArea() != null, FarmWork::getArea, farmWorkDTO.getMinArea())
                .le(farmWorkDTO.getEndTime() != null, FarmWork::getEndTime, farmWorkDTO.getEndTime())
                .eq(farmWorkDTO.getStatus() != null, FarmWork::getStatus, farmWorkDTO.getStatus())
                .eq(farmWorkDTO.getMyFarmWork() != null && farmWorkDTO.getMyFarmWork(), FarmWork::getCreateBy, userId)
                .orderByDesc(farmWorkDTO.getOrderByCreateTimeDesc() != null && farmWorkDTO.getOrderByCreateTimeDesc(),
                        FarmWork::getCreateTime)
                .orderByAsc(farmWorkDTO.getOrderByCreateTimeDesc() != null && !farmWorkDTO.getOrderByCreateTimeDesc(), FarmWork::getCreateTime);
        // 2. 处理关联条件
        handleRelatedConditions(queryWrapper, farmWorkDTO);

        // 3. 执行分页查询获取基础数据
        Page<FarmWork> page = farmWorkInfoService.page(Page.of(pageNum, pageSize), queryWrapper);
        if (page.getRecords().isEmpty()) {
            return AjaxResult.success()
                    .put("data", Collections.emptyList())
                    .put("total", 0);
        }

        // 4. 批量查询关联数据
        List<FarmWorkVO> voList = batchQueryAndBuildVOs(page.getRecords());

        return AjaxResult.success()
                .put("data", voList)
                .put("total", page.getTotal());
    }

    @Override
    @Scheduled(cron = "0 59 23 * * ?")
    public void checkEffect() {
        //查询所有截止时间小于当前时间的农活
        var queryWrapper = new LambdaQueryWrapper<FarmWork>();
        queryWrapper.le(FarmWork::getEndTime, LocalDateTime.now());
        List<FarmWork> expiredList = farmWorkInfoService.list(queryWrapper);
        //将这些数据状态设置为已过期
        expiredList.forEach(item -> {
            item.setStatus(Constants.NO_LONGER_AVAILABLE);
        });
        farmWorkInfoService.updateBatchById(expiredList);
    }

    @Override
    @DSTransactional(rollbackFor = Exception.class)
    public void deleteByUsername(String username) {
        //通过username查找userid
        String userId = userService.selectUserByUserName(username).getUserId().toString();
        var wrapper = new LambdaUpdateWrapper<FarmWork>();
        wrapper.eq(FarmWork::getCreateBy, userId);
        wrapper.set(FarmWork::getStatus, DELETE);
        farmWorkInfoService.update(wrapper);

    }

    private void handleRelatedConditions(LambdaQueryWrapper<FarmWork> queryWrapper, FarmWorkDTO farmWorkDTO) {
        List<String> subQueries = new ArrayList<>();

        // 处理作物查询 - 必须包含所有选中的作物
        if (farmWorkDTO.getCropId() != null && !farmWorkDTO.getCropId().isEmpty()) {
            String cropQuery = farmWorkDTO.getCropId().stream()
                    .map(cropId -> "SELECT farm_work_id FROM farm_work_crop WHERE crop_id = " + cropId)
                    .collect(Collectors.joining(" INTERSECT "));
            subQueries.add("(" + cropQuery + ")");
        }

        // 处理作业类型查询 - 必须包含所有选中的作业类型
        if (farmWorkDTO.getWorkTypeId() != null && !farmWorkDTO.getWorkTypeId().isEmpty()) {
            String workTypeQuery = farmWorkDTO.getWorkTypeId().stream()
                    .map(typeId -> "SELECT farm_work_id FROM farm_work_type WHERE work_type_id = " + typeId)
                    .collect(Collectors.joining(" INTERSECT "));
            subQueries.add("(" + workTypeQuery + ")");
        }

        // 处理地址查询 - 必须包含所有选中的地址
        if (farmWorkDTO.getLocation() != null && !farmWorkDTO.getLocation().isEmpty()) {
            String locationQuery = farmWorkDTO.getLocation().stream()
                    .map(location ->
                            "SELECT DISTINCT fwlp.farm_work_id " +
                                    "FROM farm_work_land_plot fwlp " +
                                    "JOIN land_plot lp ON CAST(fwlp.land_plot_id AS VARCHAR) = CAST(lp.id AS VARCHAR) " +
                                    "WHERE lp.address LIKE '%" + location + "%'"
                    )
                    .collect(Collectors.joining(" INTERSECT "));
            subQueries.add("(" + locationQuery + ")");
        }
        //根据地块ID查询
        if (farmWorkDTO.getLandPlotId() != null && !farmWorkDTO.getLandPlotId().isEmpty()) {
            String landPlotQuery = farmWorkDTO.getLandPlotId().stream()
                    .map(landPlotId ->
                            "SELECT DISTINCT fwlp.farm_work_id " +
                                    "FROM farm_work_land_plot fwlp " +
                                    "WHERE fwlp.land_plot_id = '" + landPlotId + "'"
                    )
                    .collect(Collectors.joining(" INTERSECT "));
            subQueries.add("(" + landPlotQuery + ")");
        }

        // 组合所有查询条件
        if (!subQueries.isEmpty()) {
            queryWrapper.inSql(FarmWork::getId, String.join(" INTERSECT ", subQueries));
        }
    }

    private List<FarmWorkVO> batchQueryAndBuildVOs(List<FarmWork> farmWorks) {
        // 1. 收集所有ID
        final Set<String> farmWorkIds = farmWorks.stream()
                .map(FarmWork::getId)
                .collect(Collectors.toSet());

        // 2. 批量查询关联数据
        final Map<String, List<FarmWorkCrop>> cropMap = batchQueryCrops(farmWorkIds);
        final Map<String, List<FarmWorkType>> workTypeMap = batchQueryWorkTypes(farmWorkIds);
        final Map<String, List<FarmWorkLandPlot>> landPlotMap = batchQueryLandPlots(farmWorkIds);

        // 3. 批量查询详情数据
        Set<Long> cropIds = cropMap.values().stream()
                .flatMap(List::stream)
                .map(FarmWorkCrop::getCropId)
                .collect(Collectors.toSet());

        final Map<Long, Crop> cropDetailsMap;
        if (!cropIds.isEmpty()) {
            cropDetailsMap = cropService.listByIds(cropIds)
                    .stream()
                    .collect(Collectors.toMap(Crop::getId, crop -> crop));
        } else {
            cropDetailsMap = Collections.emptyMap();
        }

        Set<Long> landPlotIds = landPlotMap.values().stream()
                .flatMap(List::stream)
                .map(FarmWorkLandPlot::getLandPlotId)
                .collect(Collectors.toSet());

        final Map<Long, LandPlot> landPlotDetailsMap;
        if (!landPlotIds.isEmpty()) {
            landPlotDetailsMap = landPlotService.listByIds(landPlotIds)
                    .stream()
                    .collect(Collectors.toMap(
                            LandPlot::getId,
                            plot -> plot,
                            (existing, replacement) -> existing,
                            HashMap::new
                    ));
        } else {
            landPlotDetailsMap = Collections.emptyMap();
        }

        // 4. 构建VO对象
        return farmWorks.stream()
                .map(farmWork -> buildFarmWorkVO(farmWork, cropMap, workTypeMap, landPlotMap,
                        cropDetailsMap, landPlotDetailsMap))
                .collect(Collectors.toList());
    }

    private Map<String, List<FarmWorkType>> batchQueryWorkTypes(Set<String> farmWorkIds) {
        return farmWorkTypeService.list(new LambdaQueryWrapper<FarmWorkType>()
                        .in(FarmWorkType::getFarmWorkId, farmWorkIds))
                .stream()
                .collect(Collectors.groupingBy(FarmWorkType::getFarmWorkId));
    }

    private Map<String, List<FarmWorkLandPlot>> batchQueryLandPlots(Set<String> farmWorkIds) {
        return farmWorkLandPlotService.list(new LambdaQueryWrapper<FarmWorkLandPlot>()
                        .in(FarmWorkLandPlot::getFarmWorkId, farmWorkIds))
                .stream()
                .collect(Collectors.groupingBy(FarmWorkLandPlot::getFarmWorkId));
    }

    private Map<String, List<FarmWorkCrop>> batchQueryCrops(Set<String> farmWorkIds) {
        return farmWorkCropService.list(new LambdaQueryWrapper<FarmWorkCrop>()
                        .in(FarmWorkCrop::getFarmWorkId, farmWorkIds))
                .stream()
                .collect(Collectors.groupingBy(FarmWorkCrop::getFarmWorkId));
    }

    private FarmWorkVO buildFarmWorkVO(FarmWork farmWork,
                                       Map<String, List<FarmWorkCrop>> cropMap,
                                       Map<String, List<FarmWorkType>> workTypeMap,
                                       Map<String, List<FarmWorkLandPlot>> landPlotMap,
                                       Map<Long, Crop> cropDetailsMap,
                                       Map<Long, LandPlot> landPlotDetailsMap) {
        FarmWorkVO vo = new FarmWorkVO();
        BeanUtils.copyProperties(farmWork, vo);
        vo.setCreateBy(userService.selectUserById(Long.parseLong(farmWork.getCreateBy())).getUserName());
        if (ObjectUtil.isNotEmpty(farmWork.getUpdateBy())){
            String updateBy = userService.selectUserById(Long.parseLong(farmWork.getUpdateBy())).getUserName();
            vo.setUpdateBy(updateBy);
        }
        // 设置作物信息
        List<FarmWorkCrop> crops = cropMap.getOrDefault(vo.getId(), Collections.emptyList());
        vo.setCropId(crops.stream()
                .map(FarmWorkCrop::getCropId)
                .collect(Collectors.toList()));
        vo.setCropName(crops.stream()
                .map(crop -> cropDetailsMap.get(crop.getCropId()))
                .filter(Objects::nonNull)
                .map(Crop::getCropName)
                .collect(Collectors.toList()));

        // 设置作业类型信息
        List<FarmWorkType> workTypes = workTypeMap.getOrDefault(vo.getId(), Collections.emptyList());
        vo.setWorkTypeId(workTypes.stream()
                .map(FarmWorkType::getWorkTypeId)
                .collect(Collectors.toList()));
        vo.setWorkTypeName(workTypes.stream()
                .map(type -> uniqueWorkTypeService.getNameById(type.getWorkTypeId()))
                .collect(Collectors.toList()));

        // 设置地块信息
        List<FarmWorkLandPlot> landPlots = landPlotMap.getOrDefault(vo.getId(), Collections.emptyList());
        List<String> landPlotIds = landPlots.stream()
                .map(FarmWorkLandPlot::getLandPlotId)
                .map(String::valueOf)
                .toList();
        vo.setLandPlotId(landPlotIds);


        List<LandPlot> landPlotDetails = landPlots.stream()
                .map(plot -> landPlotDetailsMap.get(plot.getLandPlotId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        vo.setLandPlotName(landPlotDetails.stream()
                .map(LandPlot::getLandName)
                .collect(Collectors.toList()));
        vo.setLandPlotImage(landPlotDetails.stream()
                .map(LandPlot::getImage)
                .collect(Collectors.toList()));
        vo.setLandAddress(landPlotDetails.stream()
                .map(LandPlot::getAddress)
                .collect(Collectors.toList()));

        return vo;
    }

    private void updateFarmWorkLandPlot(String id, List<FarmWorkLandPlot> farmWorkLandPlot) {
        var queryWrapper = new LambdaQueryWrapper<FarmWorkLandPlot>();
        queryWrapper.eq(FarmWorkLandPlot::getFarmWorkId, id);
        farmWorkLandPlotService.remove(queryWrapper);
        farmWorkLandPlotService.saveBatch(farmWorkLandPlot);
    }

    private void updateFarmWorkCrop(String id, List<FarmWorkCrop> farmWorkCrop) {
        var queryWrapper = new LambdaQueryWrapper<FarmWorkCrop>();
        queryWrapper.eq(FarmWorkCrop::getFarmWorkId, id);
        farmWorkCropService.remove(queryWrapper);
        farmWorkCropService.saveBatch(farmWorkCrop);
    }
}
