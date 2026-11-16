package com.agri.cms.system.service.impl;

import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.system.domain.CropWork;
import com.agri.cms.system.domain.FieldWork;
import com.agri.cms.system.mapper.CropWorkMapper;
import com.agri.cms.system.mapper.FieldWorkMapper;
import com.agri.cms.system.service.ICropWorkService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 李鸿麟
 */
@DS("pg")
@Service
public class CropWorkServiceImpl extends ServiceImpl<CropWorkMapper, CropWork> implements ICropWorkService {

    @Resource
    CropWorkMapper cropWorkMapper;

    @Resource
    FieldWorkMapper fieldWorkMapper;

    public AjaxResult getAvailableFieldWorks(int cropId) {
        if (cropId <= 0) {
            return AjaxResult.warn("参数错误");
        }

        // 先查询 CropWork 表中的最大 cropId
        Integer maxCropId = cropWorkMapper.selectMaxCropId();
        if (maxCropId == null || cropId > maxCropId) {
            return AjaxResult.warn("无效的 cropId");
        }

        // 查询 crop_work 表中所有的 work_id
        QueryWrapper<CropWork> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("crop_id", cropId);
        queryWrapper.eq("is_available", Constants.IS_AVAILABLE);
        List<CropWork> cropWorks = cropWorkMapper.selectList(queryWrapper);

        // 获取 work_id 列表
        List<Integer> workIds = cropWorks.stream()
                .map(cropWork -> cropWork.getWorkId().intValue())
                .distinct() // 去重工作 ID
                .toList();

        // 如果 workIds 为空，返回提示
        if (workIds.isEmpty()) {
            return AjaxResult.warn("当前作物没有可用的工作 ID");
        }

        return AjaxResult.success(fieldWorkMapper.selectBatchIds(workIds));
    }
}
