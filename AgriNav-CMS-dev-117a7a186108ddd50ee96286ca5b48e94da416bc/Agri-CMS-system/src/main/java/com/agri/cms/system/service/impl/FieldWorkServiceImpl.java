package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.Crop;
import com.agri.cms.system.domain.CropWork;
import com.agri.cms.system.domain.FieldWork;
import com.agri.cms.system.mapper.CropMapper;
import com.agri.cms.system.mapper.CropWorkMapper;
import com.agri.cms.system.mapper.FieldWorkMapper;
import com.agri.cms.system.service.IFieldWorkService;
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

/**
 * 田间工作服务实现类
 *
 * @author 李鸿麟
 */
@Service
@DS("pg")
public class FieldWorkServiceImpl extends ServiceImpl<FieldWorkMapper, FieldWork> implements IFieldWorkService {

    @Resource
    private FieldWorkMapper fieldWorkMapper;

    @Resource
    private CropWorkServiceImpl cropWorkService;

    @Resource
    private CropWorkMapper cropWorkMapper;

    @Resource
    private CropMapper cropMapper;

    /**
     * 获取指定作物可用的田间工作
     *
     * @param cropId 作物ID
     * @return 可用的田间工作列表
     */
    public AjaxResult getAvailableFieldWorks(Long cropId) {
        // 参数校验
        if (cropId == null || cropId <= 0) {
            return AjaxResult.warn("作物ID不能为空");
        }

        // 检查作物是否存在
        Crop crop = cropMapper.selectById(cropId);
        if (crop == null) {
            return AjaxResult.warn("指定的作物不存在");
        }

        // 查询符合条件的 CropWork
        QueryWrapper<CropWork> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("crop_id", cropId)
                .eq("is_available", Constants.IS_AVAILABLE);

        // 查找符合条件的作物作业
        List<CropWork> cropWorks = cropWorkMapper.selectList(queryWrapper);

        // 判断是否有可用作业
        if (cropWorks.isEmpty()) {
            return AjaxResult.warn("当前作物没有可用作业");
        }

        // 提取可用作业对应的 workId
        List<Long> cropWorkIds = cropWorks.stream()
                .map(CropWork::getWorkId)
                .collect(Collectors.toList());

        // 查询对应的田间工作
        List<FieldWork> fieldWorks = fieldWorkMapper.selectBatchIds(cropWorkIds);

        return AjaxResult.success(fieldWorks);
    }

    /**
     * 添加新的田间工作
     *
     * @param work   田间工作对象
     * @param cropId 作物ID
     * @return 操作结果
     */
    @Transactional
    public AjaxResult addWork(FieldWork work, Long cropId) {
        // 参数校验
        if (cropId == null || cropId <= 0) {
            return AjaxResult.warn("作物ID不能为空");
        }
        if (work == null || work.getWorkName() == null || work.getWorkName().isEmpty()) {
            return AjaxResult.warn("工作名称不能为空");
        }

        // 检查作物是否存在
        Crop crop = cropMapper.selectById(cropId);
        if (crop == null) {
            return AjaxResult.warn("指定的作物不存在");
        }

        // 检查新增工作是否已经存在
        QueryWrapper<FieldWork> checkWrapper = new QueryWrapper<>();
        checkWrapper.eq("work_name", work.getWorkName());
        if (fieldWorkMapper.selectCount(checkWrapper) > 0) {
            return AjaxResult.warn("该工作名称已存在");
        }

        // 生成新的UUID
        work.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        work.setCreateTime(LocalDateTime.now());
        work.setUpdateTime(LocalDateTime.now());
        work.setIsDeleted(false);

        // 插入新工作
        int insertResult = fieldWorkMapper.insert(work);
        if (insertResult <= 0) {
            return AjaxResult.error("插入工作失败");
        }

        // 将新增的工作与作物关联
        CropWork cropWork = new CropWork(work.getId(), cropId, Constants.IS_AVAILABLE);
        int linkResult = cropWorkMapper.insert(cropWork);
        if (linkResult <= 0) {
            return AjaxResult.error("关联作物和工作失败");
        }

        return AjaxResult.success("添加工作成功");
    }

    /**
     * 删除田间工作
     *
     * @param workId 工作ID
     * @return 操作结果
     */
    @Transactional
    public AjaxResult deleteWork(Long workId) {
        if (workId == null || workId <= 0) {
            return AjaxResult.warn("工作ID不能为空");
        }

        // 查询工作是否存在且未删除
        FieldWork existingWork = fieldWorkMapper.selectById(workId);
        if (existingWork == null || existingWork.getIsDeleted()) {
            return AjaxResult.warn("该工作不存在或已被删除");
        }

        try {
            // 更新work表的is_deleted字段
            existingWork.setIsDeleted(Constants.IS_DELETED);
            existingWork.setUpdateTime(LocalDateTime.now());
            fieldWorkMapper.updateById(existingWork);

            // 更新crop_work表的is_available字段
            QueryWrapper<CropWork> cropWorkWrapper = new QueryWrapper<>();
            cropWorkWrapper.eq("work_id", workId);
            CropWork cropWork = new CropWork();
            cropWork.setAvailable(Constants.IS_DELETED);
            cropWorkService.update(cropWork, cropWorkWrapper);

            return AjaxResult.success("删除成功");
        } catch (Exception e) {
            log.error("删除工作id为:" + workId + "时失败", e);
            return AjaxResult.error("删除失败");
        }
    }

    /**
     * 更新田间工作
     *
     * @param work 更新后的工作对象
     * @return 操作结果
     */
    @Transactional
    public AjaxResult updateWork(FieldWork work) {
        // 数据完整性校验
        if (work == null || work.getId() == null || work.getId() <= 0) {
            return AjaxResult.error("工作ID不能为空");
        }

        // 尝试获取当前数据库中的记录
        FieldWork existingWork = fieldWorkMapper.selectById(work.getId());
        if (existingWork == null) {
            return AjaxResult.warn("该工作不存在");
        }

        // 检查工作名称是否重复
        if (existingWork.getWorkName().equals(work.getWorkName())) {
            return AjaxResult.warn("该工作名称已存在");
        }

        // 更新记录
        work.setUpdateTime(LocalDateTime.now());
        int updatedCount = fieldWorkMapper.updateById(work);

        if (updatedCount > 0) {
            // 如果工作被标记为删除，更新关联表
            if (!work.getIsDeleted()) {
                QueryWrapper<CropWork> cropWorkQueryWrapper = new QueryWrapper<>();
                cropWorkQueryWrapper.eq("work_id", work.getId());
                CropWork cropWork = new CropWork();
                cropWork.setAvailable(Constants.IS_DELETED);
                cropWorkService.update(cropWork, cropWorkQueryWrapper);
            }
            return AjaxResult.success("更新成功");
        }

        return AjaxResult.error("更新失败");
    }

    @Override
    public String getWorkTypeNames(Long workTypeId) {
        return fieldWorkMapper.selectById(workTypeId).getWorkName();
    }
}
