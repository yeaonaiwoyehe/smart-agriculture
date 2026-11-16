package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.Crop;
import com.agri.cms.system.domain.CropWork;
import com.agri.cms.system.domain.FieldWork;
import com.agri.cms.system.mapper.CropMapper;
import com.agri.cms.system.service.ICropService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author 李鸿麟
 */
@Service
@DS("pg")
public class CropServiceImpl extends ServiceImpl<CropMapper, Crop> implements ICropService {

    @Resource
    private CropMapper cropMapper;

    @Resource
    private CropWorkServiceImpl cropWorkService;
    @Resource
    private FieldWorkServiceImpl fieldWorkService;

    @Transactional
    public AjaxResult addCrop(Crop crop) {
        // 首先检查新增的作物类型是否已经存在，如果存在就不插入
        if (cropMapper.checkCropIsInsertedByName(crop.getCropName())) {
            return AjaxResult.warn("当前作物已经存在");
        }
        crop.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);

        if (cropMapper.insert(crop) > 0) {
            return AjaxResult.success("插入成功");
        }

        return AjaxResult.error("插入失败");
    }

    @Transactional
    public AjaxResult deleteCrop(Long cropId) {
        // 参数验证
        if (cropId == null || cropId <= 0) {
            return AjaxResult.warn("参数错误");
        }

        // 使用一个查询包裹器来确保只查找未被逻辑删除的作物
        QueryWrapper<Crop> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", cropId)
                .eq("is_deleted", Constants.IS_AVAILABLE);

        // 确认当前作物存在且未被删除
        Crop crop = cropMapper.selectOne(queryWrapper);
        if (crop != null) {
            // 逻辑删除作物
            crop.setIsDeleted(Constants.IS_DELETED);
            cropMapper.updateById(crop);

            // 逻辑删除 crop_work 表中的数据
            QueryWrapper<CropWork> cropWorkQueryWrapper = new QueryWrapper<>();
            cropWorkQueryWrapper.eq("crop_id", cropId)
                    .eq("is_available", Constants.IS_AVAILABLE);

            CropWork cropWork = new CropWork();
            cropWork.setAvailable(Constants.IS_DELETED);
            cropWorkService.update(cropWork, cropWorkQueryWrapper);

            // 记录操作日志
            log.trace("Deleted crop with id: " + cropId);

            return AjaxResult.success("删除成功");
        }

        return AjaxResult.error("删除失败");
    }

    @Transactional
    public AjaxResult updateCrop(Crop crop) {
        // 数据完整性校验
        if (crop == null || crop.getId() == null || crop.getCropName() == null) {
            return AjaxResult.error("作物ID或名称不合法");
        }

        // 尝试获取当前数据库中的记录
        Crop existingCrop = cropMapper.selectById(crop.getId());
        if (existingCrop == null) {
            return AjaxResult.warn("该作物不存在");
        }

        // 校验是否有需要修改的字段
        if (existingCrop.getCropName().equals(crop.getCropName())
                && existingCrop.getIsDeleted().equals(crop.getIsDeleted())) {
            return AjaxResult.warn("无需修改，数据无差异");
        }

        // 更新记录的更新时间
        crop.setUpdateTime(LocalDateTime.now());

        // 执行作物表的更新
        int updatedCount = cropMapper.updateById(crop);

        if (updatedCount > 0) {
            // 如果作物被标记为删除，则更新关联表中作物工作状态
            if (crop.getIsDeleted() != null && crop.getIsDeleted()) {
                QueryWrapper<CropWork> cropWorkQueryWrapper = new QueryWrapper<>();
                cropWorkQueryWrapper.eq("crop_id", crop.getId());
                cropWorkQueryWrapper.eq("is_available", Constants.IS_AVAILABLE);
                CropWork cropWork = new CropWork();
                cropWork.setAvailable(Constants.IS_DELETED);
                cropWorkService.update(cropWork, cropWorkQueryWrapper);
            }
            return AjaxResult.success("修改成功");
        }

        return AjaxResult.error("更新失败");
    }
}
