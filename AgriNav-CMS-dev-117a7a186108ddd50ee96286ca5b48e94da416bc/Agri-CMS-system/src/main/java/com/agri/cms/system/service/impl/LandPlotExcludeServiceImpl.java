package com.agri.cms.system.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONException;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.LandPlotConstants;
import com.agri.cms.common.exception.LandPlotException;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.LandPlotExclude;
import com.agri.cms.system.domain.vo.LandPlotExcludeVO;
import com.agri.cms.system.mapper.LandPlotExcludeMapper;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.service.ILandPlotExcludeService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.postgis.Polygon;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 无用区域服务实现类
 * @author mfr
 * @since 2024/11/18 */
@Service
@DS("pg")
public class LandPlotExcludeServiceImpl extends ServiceImpl<LandPlotExcludeMapper, LandPlotExclude> implements ILandPlotExcludeService {
    @Resource
    LandPlotMapper landPlotMapper;

    /**
     * 添加无用区域
     * @author mfr
     * @since 2024/11/18     */
    @Override
    @Transactional
    public void addLandPlotExclude(LandPlotExclude landPlotExclude) {
        // 先去除父亲地块的无用区域的面积
        // 先判断无用区域面积删除完地块面积是否大于0
//        LandPlotRecord plot = landPlotMapper.selectOne(new LambdaQueryWrapper<LandPlotRecord>().eq(LandPlotRecord::getId, landPlotExclude.getLandPlotId()));
//        if (plot.getLandArea() - landPlotExclude.getArea() < 0) {
//            throw new LandPlotException(LandPlotConstants.LAND_PLOT_ADD_EXCEPTION+"插花地异常");
//        }
//        landPlotMapper.subtractArea(landPlotExclude.getLandPlotId(), landPlotExclude.getArea());
//        // 再添加无用区域
//        save(landPlotExclude);
        return;
    }


    /**
     * 根据地块id查询无用区域
     * @return List<LandPlotExclude>
     * @author mfr
     * @since 2024/11/18     */
    @Override
    public List<LandPlotExclude> queryByLandPlotId(Long landPlotId) {
        LambdaQueryWrapper<LandPlotExclude> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LandPlotExclude::getLandPlotId, landPlotId);
        queryWrapper.eq(LandPlotExclude::getIsDeleted, Constants.NOT_DELETED);

        return list(queryWrapper);
    }


    /**
     * 删除无用区域
     * @author mfr
     * @since 2024/11/18     */
    @Override
    @Transactional
    public void deleteLandPlotExclude(Long landPlotId) {
        // 先判断是否有这些无用区域，没有则说明有问题
        if (count(new LambdaQueryWrapper<LandPlotExclude>().eq(LandPlotExclude::getLandPlotId, landPlotId)) == 0) {
            throw new LandPlotException(LandPlotConstants.LAND_PLOT_DELETE_EXCEPTION+"无用区域不存在");
        }
        UpdateWrapper<LandPlotExclude> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("land_plot_id", landPlotId)
                .set("is_deleted", Constants.DELETED);

        if (!update(updateWrapper)) {
            throw new LandPlotException(LandPlotConstants.LAND_PLOT_DELETE_EXCEPTION+ "无用区域删除失败");
        }
    }

    @Override
    public List<LandPlotExcludeVO> queryBatch(List<Long> landPlotIds) {
        if (CollectionUtil.isEmpty(landPlotIds)) {
            return null;
        }

        // 批量查询所有符合条件的无用地记录
        LambdaQueryWrapper<LandPlotExclude> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(LandPlotExclude::getLandPlotId, landPlotIds);
        List<LandPlotExclude> excludes = baseMapper.selectList(wrapper);
        //封装一个list<LandPlotExcludeVO>
        List<LandPlotExcludeVO> result = excludes.stream().
                //根据地块id分组
                collect(Collectors.groupingBy(LandPlotExclude::getLandPlotId)).
                //将每个分组转换成一个LandPlotExcludeVO对象
                entrySet().stream().
                map(entry -> {
                    LandPlotExcludeVO vo = new LandPlotExcludeVO();
                    vo.setLandPlotId(entry.getKey());
                    vo.setLandPlotExcludes(entry.getValue());
                    return vo;
                })
                .toList();
        return result;
    }

    @Override
    @Transactional
    public void batchSave(List<String> landPlotExcludes, Long id) {
        List<LandPlotExclude> landPlotExcludesStr = convertLandPlotExcludes(landPlotExcludes);
        // 遍历landPlotExcludes, 保存
        landPlotExcludesStr.forEach(landPlotExclude -> {
            landPlotExclude.setLandPlotId(id);
            save(landPlotExclude);
        });
    }

    /**
     * 将字符串列表转换为LandPlotExclude对象列表
     * @return List<LandPlotExclude>
     * @author mfr
     * @since 2024/11/29 */
    public List<LandPlotExclude> convertLandPlotExcludes(List<String> excludeStrs) {
        List<LandPlotExclude> result = new ArrayList<>();

        for (String str : excludeStrs) {
            try {
                JSONObject json = JSON.parseObject(str);

                LandPlotExclude exclude = new LandPlotExclude();
                // 直接获取字段值
                exclude.setArea(json.getFloatValue("area"));
                exclude.setBoundary(new Polygon(json.getString("boundary")));
                exclude.setIsDeleted(Constants.NOT_DELETED);

                result.add(exclude);
            } catch (Exception e) {
                throw new LandPlotException(LandPlotConstants.LAND_PLOT_ADD_EXCEPTION+"无用区域解析失败");
            }
        }

        return result;
    }


}
