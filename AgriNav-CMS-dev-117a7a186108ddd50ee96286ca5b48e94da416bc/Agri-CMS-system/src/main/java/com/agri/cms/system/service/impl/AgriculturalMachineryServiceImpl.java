package com.agri.cms.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.domain.entity.SysUser;
import com.agri.cms.common.utils.bean.BeanUtils;
import com.agri.cms.common.utils.uuid.UUID;
import com.agri.cms.system.domain.*;
import com.agri.cms.system.domain.dto.AgriculturalMachineryDTO;
import com.agri.cms.system.domain.vo.AgriculturalMachineryVO;
import com.agri.cms.system.service.*;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.dynamic.datasource.annotation.DSTransactional;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.ConstraintViolation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.agri.cms.common.utils.file.FileUploadUtils;
import jakarta.validation.Validator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 姚锦伦
 */
@Service
public class AgriculturalMachineryServiceImpl implements AgriculturalMachineryService {
    @Autowired
    private AgriculturalMachineryCropService agriculturalMachineryCropService;
    @Autowired
    private AgriculturalMachineryImageService agriculturalMachineryImageService;
    @Autowired
    private AgriculturalMachineryInfoService agriculturalMachineryInfoService;
    @Autowired
    private AgriculturalMachineryLocationService agriculturalMachineryLocationService;
    @Autowired
    private AgriculturalMachineryTypeService agriculturalMachineryTypeService;
    @Autowired
    private ICropService cropService;
    @Autowired
    private Validator validator;
    @Autowired
    private SysUserServiceImpl userService;
    private final int DELETE = 2;
    private static final String BUCKET_NAME = "agricultural-machinery";

    @Override
    public void insert(AgriculturalMachineryDTO agriculturalMachineryDTO, String userId) {
        //将DTO转换成实体类
        UUID uuid = UUID.fastUUID();
        List<AgriculturalMachineryCrop> agriculturalMachineryCrop = agriculturalMachineryDTO.toAgriculturalMachineryCrop(uuid);
        List<AgriculturalMachineryImage> agriculturalMachineryImage = agriculturalMachineryDTO.toAgriculturalMachineryImage(uuid);
        List<AgriculturalMachineryLocation> agriculturalMachineryLocation = agriculturalMachineryDTO.toAgriculturalMachineryLocation(uuid);
        AgriculturalMachinery agriculturalMachinery = agriculturalMachineryDTO.toAgriculturalMachinery(uuid);
        //设置状态
        agriculturalMachinery.setStatus(Constants.AVAILABLE);
        //设置创建人
        agriculturalMachinery.setCreateBy(userId);
        //设置创建时间
        agriculturalMachinery.setCreateTime(LocalDateTime.now());
        agriculturalMachineryInfoService.save(agriculturalMachinery);
        agriculturalMachineryCropService.saveBatch(agriculturalMachineryCrop);
        agriculturalMachineryImageService.saveBatch(agriculturalMachineryImage);
        agriculturalMachineryLocationService.saveBatch(agriculturalMachineryLocation);
    }

    @Override
    @DS("pg")
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        LambdaUpdateWrapper<AgriculturalMachinery> wrapper = new LambdaUpdateWrapper<>();
        //设置删除状态
        wrapper.set(AgriculturalMachinery::getStatus, Constants.NO_LONGER_AVAILABLE);
        //根据id条件
        wrapper.eq(AgriculturalMachinery::getId, id);
        agriculturalMachineryInfoService.update(wrapper);
    }

    @Override
    public void update(AgriculturalMachineryDTO agriculturalMachineryDTO, String userId) {
        //将DTO转换成实体类
        List<AgriculturalMachineryCrop> agriculturalMachineryCrop = agriculturalMachineryDTO.toAgriculturalMachineryCrop();
        List<AgriculturalMachineryImage> agriculturalMachineryImage = agriculturalMachineryDTO.toAgriculturalMachineryImage();
        List<AgriculturalMachineryLocation> agriculturalMachineryLocation = agriculturalMachineryDTO.toAgriculturalMachineryLocation();
        AgriculturalMachinery agriculturalMachinery = agriculturalMachineryDTO.toAgriculturalMachinery();

        String id = agriculturalMachinery.getId();
        //更新农机作物信息
        updateAgriculturalMachineryCrop(id, agriculturalMachineryCrop);
        //更新农机图片信息
        updateAgriculturalMachineryImage(id, agriculturalMachineryImage);
        //更新农机位置信息
        updateAgriculturalMachineryLocation(id, agriculturalMachineryLocation);
        //更新农机信息
        agriculturalMachinery.setUpdateBy(userId);
        agriculturalMachinery.setUpdateTime(LocalDateTime.now());
        agriculturalMachineryInfoService.updateById(agriculturalMachinery);
    }

    @Override
    public AjaxResult list(AgriculturalMachineryDTO agriculturalMachineryDTO, String userId, int pageNum, int pageSize) {
        // 1. 构建基础查询条件
        var queryWrapper = buildBaseQueryWrapper(agriculturalMachineryDTO, userId);

        // 2. 处理位置和作物条件
        handleLocationAndCropConditions(queryWrapper, agriculturalMachineryDTO);

        // 3. 执行分页查询
        Page<AgriculturalMachinery> page = agriculturalMachineryInfoService.page(Page.of(pageNum, pageSize), queryWrapper);
        if (page.getRecords().isEmpty()) {
            return AjaxResult.success()
                    .put("data", Collections.emptyList())
                    .put("total", 0);
        }

        // 4. 批量构建VO列表
        List<AgriculturalMachineryVO> voList = batchBuildVOs(page.getRecords());

        return AjaxResult.success()
                .put("data", voList)
                .put("total", page.getTotal());
    }

    @Override
    @Scheduled(cron = "0 59 23 * * ?")
    public void checkEffect() {
        LocalDateTime now = LocalDateTime.now();
        var queryWrapper = new LambdaQueryWrapper<AgriculturalMachinery>();
        //查询所有截止时间小于当前时间的数据
        queryWrapper.le(AgriculturalMachinery::getEndTime, now);
        List<AgriculturalMachinery> expiredList = agriculturalMachineryInfoService.list(queryWrapper);
        //将这些数据状态设置为已过期
        expiredList.forEach(item -> {
            item.setStatus(Constants.NO_LONGER_AVAILABLE);
        });
        agriculturalMachineryInfoService.updateBatchById(expiredList);
    }

    @Override
    public void deleteByUsername(String username) {
        //通过username查找userid
        SysUser sysUser = userService.selectUserByUserName(username);
        Long userId = userService.selectUserByUserName(username).getUserId();
        //删除该用户的农机信息
        deleteAgriculturalMachinery(userId.toString());
    }
    @DS("pg")
    private void deleteAgriculturalMachinery(String userId) {
        LambdaUpdateWrapper<AgriculturalMachinery> wrapper = new LambdaUpdateWrapper<>();
        //设置删除状态
        wrapper.set(AgriculturalMachinery::getStatus, DELETE);
        //根据id条件
        wrapper.eq(AgriculturalMachinery::getCreateBy, userId);
        agriculturalMachineryInfoService.update(wrapper);
    }

    @Override
    @DS("pg")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult add(String agriculturalMachineryDTO, List<MultipartFile> image, String userId) {
        AgriculturalMachineryDTO switchAgriculturalMachineryDTO = JSON.parseObject(agriculturalMachineryDTO, AgriculturalMachineryDTO.class);
        // 手动验证 DTO 对象
        Set<ConstraintViolation<AgriculturalMachineryDTO>> violations = validator.validate(switchAgriculturalMachineryDTO);
        if (!violations.isEmpty()) {
            // 如果有验证错误，收集错误信息并返回
            List<String> errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());
            return AjaxResult.error("验证失败: " + String.join(", ", errors));
        }
        //判断截止时间是否大于当前时间
        if (switchAgriculturalMachineryDTO.getEndTime().isBefore(LocalDateTime.now())) {
            return AjaxResult.error("截止时间不得小于当前时间");
        }
        List<String> urls = new ArrayList<>();
        //获取每个图片上传成功后的url
        if (image != null) {
            image.forEach(item -> {
                try {
                    if (item.isEmpty()) {
                        return;
                    }
                    String url = FileUploadUtils.uploadMinio(item, BUCKET_NAME);
                    urls.add(url);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        if (urls.isEmpty()) {
            return AjaxResult.error("图片不得为空");
        }
        switchAgriculturalMachineryDTO.setImage(urls);
        insert(switchAgriculturalMachineryDTO, userId);
        return AjaxResult.success();
    }

    @Override
    @DS("pg")
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult updateAgriculturalMachinery(String agriculturalMachineryDTO, List<MultipartFile> image, String userId) {
        //将String转为对象
        AgriculturalMachineryDTO switchAgriculturalMachineryDTO = JSON.parseObject(agriculturalMachineryDTO, AgriculturalMachineryDTO.class);
        // 手动验证 DTO 对象
        Set<ConstraintViolation<AgriculturalMachineryDTO>> violations = validator.validate(switchAgriculturalMachineryDTO);
        if (!violations.isEmpty()) {
            // 如果有验证错误，收集错误信息并返回
            List<String> errors = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.toList());
            return AjaxResult.error(String.join(", ", errors));
        }
        //判断截止时间是否大于当前时间
        if (switchAgriculturalMachineryDTO.getEndTime().isBefore(LocalDateTime.now())) {
            return AjaxResult.error("截止时间不得小于当前时间");
        }
        List<String> urls = new ArrayList<>();
        //保存已有的图片url
        if (switchAgriculturalMachineryDTO.getImage() != null) {
            urls.addAll(switchAgriculturalMachineryDTO.getImage());
        }
        //处理新上传的图片
        if (image != null) {
            image.forEach(item -> {
                if (item.isEmpty()) {
                    return;
                }
                try {
                    String url = FileUploadUtils.uploadMinio(item, BUCKET_NAME);
                    urls.add(url);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        switchAgriculturalMachineryDTO.setImage(urls);
        update(switchAgriculturalMachineryDTO, userId);
        return AjaxResult.success();
    }

    private LambdaQueryWrapper<AgriculturalMachinery> buildBaseQueryWrapper(
            AgriculturalMachineryDTO dto, String userId) {
        return new LambdaQueryWrapper<AgriculturalMachinery>()
                .ne(AgriculturalMachinery::getStatus, DELETE)
                // 名称
                .like(dto.getName() != null, AgriculturalMachinery::getName, dto.getName())
                //是否有粮车
                .eq(dto.getHavingGrainCarrier() != null,
                        AgriculturalMachinery::getHavingGrainCarrier, dto.getHavingGrainCarrier())
                //农机类型
                .eq(dto.getTypeId() != null, AgriculturalMachinery::getTypeId, dto.getTypeId())
                // 状态
                .eq(dto.getStatus() != null, AgriculturalMachinery::getStatus, dto.getStatus())
                // 联系号码
                .eq(dto.getPhoneNumber() != null, AgriculturalMachinery::getPhoneNumber, dto.getPhoneNumber())
                //是否查询自己本人发布的开关
                .eq(dto.getMyAgriculturalMachinery() != null && dto.getMyAgriculturalMachinery(),
                        AgriculturalMachinery::getCreateBy, userId)
                //发布时间
                .eq(dto.getCreateTime() != null, AgriculturalMachinery::getCreateTime, dto.getCreateTime())
                //发布时间降序
                .orderByDesc(dto.getOrderByCreateTimeDesc() != null && dto.getOrderByCreateTimeDesc(),
                        AgriculturalMachinery::getCreateTime)
                .orderByAsc(dto.getOrderByCreateTimeDesc() != null && !dto.getOrderByCreateTimeDesc(), AgriculturalMachinery::getCreateTime);
    }

    private void handleLocationAndCropConditions(LambdaQueryWrapper<AgriculturalMachinery> wrapper,
                                                 AgriculturalMachineryDTO dto) {
        List<String> subQueries = new ArrayList<>();

        // 处理地址查询 - 必须包含所有选中的地址
        if (dto.getLocation() != null && !dto.getLocation().isEmpty()) {
            String locationQuery = dto.getLocation().stream()
                    .map(location ->
                            "SELECT DISTINCT agriculture_machinery_id " +
                                    "FROM agricultural_machinery_location " +
                                    "WHERE location LIKE '%" + location + "%'"
                    )
                    .collect(Collectors.joining(" INTERSECT "));
            subQueries.add("(" + locationQuery + ")");
        }

        // 处理作物查询 - 必须包含所有选中的作物
        if (dto.getCropId() != null && !dto.getCropId().isEmpty()) {
            String cropQuery = dto.getCropId().stream()
                    .map(cropId ->
                            "SELECT DISTINCT agricultural_machinery_id " +
                                    "FROM agricultural_machinery_crop " +
                                    "WHERE crop_id = " + cropId
                    )
                    .collect(Collectors.joining(" INTERSECT "));
            subQueries.add("(" + cropQuery + ")");
        }

        // 组合所有查询条件
        if (!subQueries.isEmpty()) {
            wrapper.inSql(AgriculturalMachinery::getId, String.join(" INTERSECT ", subQueries));
        }
    }

    private List<AgriculturalMachineryVO> batchBuildVOs(List<AgriculturalMachinery> machineries) {
        if (machineries.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 收集所有需要的ID
        Set<String> machineryIds = machineries.stream()
                .map(AgriculturalMachinery::getId)
                .collect(Collectors.toSet());

        Set<String> typeIds = machineries.stream()
                .map(AgriculturalMachinery::getTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 批量查询关联数据
        Map<String, AgriculturalMachineryType> typeMap = batchQueryTypes(typeIds);
        Map<String, List<AgriculturalMachineryImage>> imageMap = batchQueryImages(machineryIds);
        Map<String, List<AgriculturalMachineryLocation>> locationMap = batchQueryLocations(machineryIds);
        Map<String, List<AgriculturalMachineryCrop>> cropMap = batchQueryCrops(machineryIds);

        // 3. 批量查询作物信息
        Set<Long> cropIds = cropMap.values().stream()
                .flatMap(List::stream)
                .map(AgriculturalMachineryCrop::getCropId)
                .collect(Collectors.toSet());

        Map<Long, Crop> cropsMap = cropService.listByIds(cropIds)
                .stream()
                .collect(Collectors.toMap(Crop::getId, crop -> crop));

        // 4. 构建VO列表
        return machineries.stream()
                .map(machinery -> buildSingleVO(machinery, typeMap, imageMap, locationMap, cropMap, cropsMap))
                .collect(Collectors.toList());
    }

    private Map<String, AgriculturalMachineryType> batchQueryTypes(Set<String> typeIds) {
        if (typeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return agriculturalMachineryTypeService.listByIds(typeIds)
                .stream()
                .collect(Collectors.toMap(AgriculturalMachineryType::getId, type -> type));
    }

    private Map<String, List<AgriculturalMachineryImage>> batchQueryImages(Set<String> machineryIds) {
        return agriculturalMachineryImageService.list(new LambdaQueryWrapper<AgriculturalMachineryImage>()
                        .in(AgriculturalMachineryImage::getAgriculturalMachineryId, machineryIds))
                .stream()
                .collect(Collectors.groupingBy(AgriculturalMachineryImage::getAgriculturalMachineryId));
    }

    private Map<String, List<AgriculturalMachineryLocation>> batchQueryLocations(Set<String> machineryIds) {
        return agriculturalMachineryLocationService.list(new LambdaQueryWrapper<AgriculturalMachineryLocation>()
                        .in(AgriculturalMachineryLocation::getAgriculturalMachineryId, machineryIds))
                .stream()
                .collect(Collectors.groupingBy(AgriculturalMachineryLocation::getAgriculturalMachineryId));
    }

    private Map<String, List<AgriculturalMachineryCrop>> batchQueryCrops(Set<String> machineryIds) {
        return agriculturalMachineryCropService.list(new LambdaQueryWrapper<AgriculturalMachineryCrop>()
                        .in(AgriculturalMachineryCrop::getAgriculturalMachineryId, machineryIds))
                .stream()
                .collect(Collectors.groupingBy(AgriculturalMachineryCrop::getAgriculturalMachineryId));
    }

    private AgriculturalMachineryVO buildSingleVO(
            AgriculturalMachinery machinery,
            Map<String, AgriculturalMachineryType> typeMap,
            Map<String, List<AgriculturalMachineryImage>> imageMap,
            Map<String, List<AgriculturalMachineryLocation>> locationMap,
            Map<String, List<AgriculturalMachineryCrop>> cropMap,
            Map<Long, Crop> cropsMap) {

        AgriculturalMachineryVO vo = new AgriculturalMachineryVO();
        BeanUtils.copyProperties(machinery, vo);
        //根据用户id查找用户名
        String userName = userService.selectUserById(Long.parseLong(machinery.getCreateBy())).getUserName();
        vo.setCreateBy(userName);
        if (ObjectUtil.isNotEmpty(machinery.getUpdateBy())){
            String updateBy = userService.selectUserById(Long.parseLong(machinery.getUpdateBy())).getUserName();
            vo.setUpdateBy(updateBy);
        }
        
        // 设置类型信息
        AgriculturalMachineryType type = typeMap.get(machinery.getTypeId());
        if (type != null) {
            vo.setTypeId(type.getId());
            vo.setTypeName(type.getName());
        }

        // 设置图片
        List<AgriculturalMachineryImage> images = imageMap.getOrDefault(vo.getId(), Collections.emptyList());
        vo.setImage(images.stream()
                .map(AgriculturalMachineryImage::getImage)
                .collect(Collectors.toList()));

        // 设置地区
        List<AgriculturalMachineryLocation> locations = locationMap.getOrDefault(vo.getId(), Collections.emptyList());
        vo.setLocation(locations.stream()
                .map(AgriculturalMachineryLocation::getLocation)
                .collect(Collectors.toList()));

        // 设置作物
        List<AgriculturalMachineryCrop> crops = cropMap.getOrDefault(vo.getId(), Collections.emptyList());
        vo.setCropId(crops.stream()
                .map(AgriculturalMachineryCrop::getCropId)
                .collect(Collectors.toList()));
        vo.setCrop(crops.stream()
                .map(crop -> cropsMap.get(crop.getCropId()))
                .filter(Objects::nonNull)
                .map(Crop::getCropName)
                .collect(Collectors.toList()));

        return vo;
    }

    private void updateAgriculturalMachineryLocation(String id, List<AgriculturalMachineryLocation> agriculturalMachineryLocation) {
        var queryWrapper = new LambdaQueryWrapper<AgriculturalMachineryLocation>();
        queryWrapper.eq(AgriculturalMachineryLocation::getAgriculturalMachineryId, id);
        agriculturalMachineryLocationService.remove(queryWrapper);
        agriculturalMachineryLocationService.saveBatch(agriculturalMachineryLocation);
    }

    private void updateAgriculturalMachineryImage(String id, List<AgriculturalMachineryImage> agriculturalMachineryImage) {
        var queryWrapper = new LambdaQueryWrapper<AgriculturalMachineryImage>();
        queryWrapper.eq(AgriculturalMachineryImage::getAgriculturalMachineryId, id);
        agriculturalMachineryImageService.remove(queryWrapper);
        agriculturalMachineryImageService.saveBatch(agriculturalMachineryImage);
    }

    private void updateAgriculturalMachineryCrop(String id, List<AgriculturalMachineryCrop> agriculturalMachineryCrop) {
        var queryWrapper = new LambdaQueryWrapper<AgriculturalMachineryCrop>();
        queryWrapper.eq(AgriculturalMachineryCrop::getAgriculturalMachineryId, id);
        agriculturalMachineryCropService.remove(queryWrapper);
        agriculturalMachineryCropService.saveBatch(agriculturalMachineryCrop);
    }
}
