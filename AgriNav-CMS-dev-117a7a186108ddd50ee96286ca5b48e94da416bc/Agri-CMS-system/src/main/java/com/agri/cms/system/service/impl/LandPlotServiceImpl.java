package com.agri.cms.system.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.constant.Constants;
import com.agri.cms.common.constant.LandPlotConstants;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.redis.RedisCache;
import com.agri.cms.common.exception.LandPlotException;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.common.utils.file.FileUploadUtils;
import com.agri.cms.system.domain.dto.AddressAreaDTO;
import com.agri.cms.system.domain.LandAddress;
import com.agri.cms.system.domain.LandPlot;
import com.agri.cms.system.domain.dto.LandPlotQueryDTO;
import com.agri.cms.system.domain.record.LandPlotRecord;
import com.agri.cms.system.domain.vo.HeatmapDataVO;
import com.agri.cms.system.domain.vo.LandPlotVO;
import com.agri.cms.system.mapper.LandPlotMapper;
import com.agri.cms.system.service.ILandAddressService;
import com.agri.cms.system.service.ILandPlotExcludeService;
import com.agri.cms.system.service.ISysUserService;
import com.agri.cms.system.service.LandPlotService;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.postgis.Polygon;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.agri.cms.common.utils.SecurityUtils.getUsername;

/**
 * @author simon
 * @since 2024/9/19
 */
@Service
//@Transactional(rollbackFor = Exception.class)
@DS("pg")
public class LandPlotServiceImpl extends ServiceImpl<LandPlotMapper, LandPlot> implements LandPlotService {


    @Resource
    private RedisCache redisCache;
    @Resource
    private LandPlotMapper landPlotMapper;
    @Resource
    private ILandAddressService landAddressService;
    @Resource
    private ILandPlotExcludeService landPlotExcludeService;
    @Resource
    @Lazy
    private CheckPathServiceImpl checkPathService;
    @Resource
    private ISysUserService sysUserService;

    @Resource
    private ElasticsearchTemplate template;

    @Override
    @Transactional
    public List<LandPlot> getLandPlotByCreatorName(Long userId, Long current, Long size) {
        LambdaQueryWrapper<LandPlot> plotQueryWrapper = new LambdaQueryWrapper<>();
        plotQueryWrapper.eq(LandPlot::getIsDeleted, Constants.NOT_DELETED);
        plotQueryWrapper.eq(LandPlot::getCreateBy, String.valueOf(userId));
        // 降序
        plotQueryWrapper.orderByDesc(LandPlot::getCreateTime);
        return list(new Page<>(current, size), plotQueryWrapper);
    }

    @Override
    @Transactional
    public AjaxResult updateLandPlot(LandPlotVO landPlotVO, MultipartFile file) {
        AjaxResult params = AjaxResult.success();
        LandPlot landPlot = new LandPlot();
        BeanUtils.copyProperties(landPlotVO, landPlot);
        try {
            // 检查重复性(排除自身ID)
            if (StringUtils.isNotBlank(landPlot.getLandName()) && StringUtils.isNotBlank(landPlot.getAddress())) {
                LambdaQueryWrapper<LandPlot> landPlotQueryWrapper = new LambdaQueryWrapper<>();
                landPlotQueryWrapper.eq(LandPlot::getLandName, landPlot.getLandName())
                        .eq(LandPlot::getIsDeleted, Constants.NOT_DELETED)
                        .eq(LandPlot::getAddress, landPlot.getAddress())
                        // 排除自身ID
                        .ne(LandPlot::getId, landPlot.getId());
                if (count(landPlotQueryWrapper) > 0) {
                    throw new LandPlotException(LandPlotConstants.LAND_PLOT_DUPLICATE_EXCEPTION);
                }
            }

            landPlot.setBoundary(new Polygon(landPlotVO.getBoundary()));
            landPlot.setUpdateBy(SecurityUtils.getUserId().toString());
            landPlot.setUpdateTime(LocalDateTime.now());
            if (file != null) {
                String image = FileUploadUtils.uploadMinio(file);
                landPlot.setImage(image);
                params.put("url", image);
            }
            params.put("data", saveOrUpdate(landPlot));
            return params;
        } catch (SQLException | IOException e) {
            throw new LandPlotException(LandPlotConstants.LAND_PLOT_UPDATE_EXCEPTION, e);
        }
    }

    @Override
    @Transactional
    public AjaxResult addLandPlot(LandPlotVO landPlotVO, MultipartFile file) {
        AjaxResult params = AjaxResult.success();
        LandPlot landPlot = new LandPlot();
        BeanUtils.copyProperties(landPlotVO, landPlot);
        try {
            landPlot.setBoundary(new Polygon(landPlotVO.getBoundary()));
            landPlot.setCenterPoint(new org.postgis.Point(landPlotVO.getCenterPoint()));
            landPlot.setIsDeleted(Constants.NOT_DELETED);
            landPlot.setCreateBy(SecurityUtils.getUserId().toString());
            landPlot.setUpdateBy(SecurityUtils.getUserId().toString());
            landPlot.setUpdateTime(LocalDateTime.now());
            landPlot.setCreateTime(LocalDateTime.now());
            String image = FileUploadUtils.uploadMinio(file);
            landPlot.setImage(image);

            // 判断地名和地址名称，除去本身的重复
            handleRepeat(landPlot);
            // 保存地块信息
            if (! save(landPlot)) {
                throw new LandPlotException(LandPlotConstants.LAND_PLOT_ADD_EXCEPTION);
            }
            params.put("data", landPlot);

            //如果是插花地则插入子地块
            if (StringUtils.isNotBlank(landPlotVO.getLandType()) && landPlotVO.getLandType().equals(LandPlotConstants.LAND_FOR_ARRANGING_FLOWERS)) {
                List<String> landPlotExcludes = landPlotVO.getLandPlotExcludes();
                landPlotExcludeService.batchSave(landPlotExcludes, landPlot.getId());
            }

            // 添加到redis
            String point = landPlotVO.getCenterPoint();
            redisCache.setCacheGeoValue(
                    Constants.LAND_PLOT_GEO_KEY,
                    new Point(Double.parseDouble(point.substring(point.indexOf(" ") + 1,
                            point.indexOf(")"))), Double.parseDouble(point.substring(point.indexOf("(") + 1,
                            point.indexOf(" ")))),
                    landPlot.getId()
            );
            checkPathService.initSingleLandPlot(landPlot.getId());
            return params;
        } catch (SQLException | IOException e) {
            throw new LandPlotException(LandPlotConstants.LAND_PLOT_ADD_EXCEPTION, e);
        }
    }

    @Override
    public void restoreLandPlot(Long id) {
        LandPlot one = getOne(new LambdaQueryWrapper<LandPlot>()
                .eq(LandPlot::getId, id)
                .eq(LandPlot::getIsDeleted, Constants.DELETED));
        if(ObjectUtil.isNull(one)){
            throw new LandPlotException(LandPlotConstants.LAND_PLOT_NOT_EXIST_EXCEPTION);
        }
        one.setIsDeleted(Constants.NOT_DELETED);
        updateById(one);
    }

    @Override
    public List<LandPlot> getHistoryLandPlots(Page<LandPlot> page,Long userId,String username) {
        return list(page,
                new LambdaQueryWrapper<LandPlot>()
                        .eq(LandPlot::getIsDeleted, Constants.DELETED)
                        .and(q->q
                                .eq(LandPlot::getCreateBy,String.valueOf(userId))
                                .or()
                                .eq(LandPlot::getOwnerPhone,username)
                        )
                        .orderByDesc(LandPlot::getUpdateTime)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUsername(String username) {
        LambdaUpdateWrapper<LandPlot> wrapper = new LambdaUpdateWrapper<>();
        //设置删除状态
        wrapper.set(LandPlot::getIsDeleted, Constants.DELETED);
        //根据创建人条件
        wrapper.eq(LandPlot::getCreateBy, username);
        landPlotMapper.update(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(String id) {
        if (StringUtils.isBlank(id)) {
            throw new LandPlotException("ID不能为空或无效");
        }

        try {
            Long longId = Long.parseLong(id);

            // 先查询地块信息
            LandPlot plot = lambdaQuery()
                    .eq(LandPlot::getId, longId)
                    // 确保未删除
                    .eq(LandPlot::getIsDeleted, Constants.NOT_DELETED)
                    .one();

            if (ObjectUtil.isNull(plot)) {
                throw new LandPlotException(LandPlotConstants.LAND_PLOT_NOT_EXIST_EXCEPTION);
            }

            // 更新删除状态
            LambdaUpdateWrapper<LandPlot> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(LandPlot::getId, longId)
                    .set(LandPlot::getIsDeleted, Constants.DELETED);

            if (!update(updateWrapper)) {
                throw new LandPlotException(LandPlotConstants.LAND_PLOT_DELETE_EXCEPTION);
            }

            // 如果是插花地则删除无用区域
            if (LandPlotConstants.LAND_FOR_ARRANGING_FLOWERS.equals(plot.getLandType())) {
                landPlotExcludeService.deleteLandPlotExclude(longId);
            }

            // 删除该地块边界(逻辑删除)
            checkPathService.deleteBoundary(longId);

            //原始轨迹删除

            //有效轨迹删除
            //删除缓存
            redisCache.deleteCacheGeoValue(Constants.LAND_PLOT_GEO_KEY, longId);

        } catch (NumberFormatException e) {
            throw new LandPlotException("无效的ID格式", e);
        } catch (LandPlotException e) {
            throw e;
        } catch (Exception e) {
            throw new LandPlotException("删除地块失败：" + e.getMessage(), e);
        }
    }

    /**
     * 处理重复
     *
     * @author mfr
     * @since 2024/10/25
     */
    private void handleRepeat(LandPlot landPlot) {
        if (StringUtils.isNotBlank(landPlot.getLandName()) && StringUtils.isNotBlank(landPlot.getAddress())) {
            LambdaQueryWrapper<LandPlot> landPlotQueryWrapper = new LambdaQueryWrapper<>();
            landPlotQueryWrapper.eq(LandPlot::getLandName, landPlot.getLandName());
            landPlotQueryWrapper.eq(LandPlot::getIsDeleted, Constants.NOT_DELETED);
            landPlotQueryWrapper.eq(LandPlot::getAddress, landPlot.getAddress());
            if (count(landPlotQueryWrapper) > 0) {
                throw new LandPlotException(LandPlotConstants.LAND_PLOT_DUPLICATE_EXCEPTION);
            }
        }
    }

    @Override
    public List<LandPlot> checkByCoordinate(Double longitude, Double latitude, Double distance) {
        //通过Redis查询附近地块的ID
        List<Integer> cacheGeoValue = redisCache.
                getCacheGeoValue(Constants.LAND_PLOT_GEO_KEY, longitude, latitude, distance);
        //通过Id查询地块详情
        return cacheGeoValue.isEmpty() ?
                List.of() : list(new LambdaQueryWrapper<LandPlot>().in(LandPlot::getId, cacheGeoValue).eq(LandPlot::getIsDeleted, Constants.NOT_DELETED));
    }

    @Override
    public AjaxResult queryByConditions(LandPlotQueryDTO landPlotQueryDTO) {
        LambdaQueryWrapper<LandPlot> plotQueryWrapper = new LambdaQueryWrapper<>();
        // 有效地块
        plotQueryWrapper.eq(LandPlot::getIsDeleted, landPlotQueryDTO.getIsDeleted());
        plotQueryWrapper.like(StringUtils.isNotBlank(landPlotQueryDTO.getLandType()), LandPlot::getLandType, landPlotQueryDTO.getLandType());
        plotQueryWrapper.like(StringUtils.isNotBlank(landPlotQueryDTO.getLandName()), LandPlot::getLandName, landPlotQueryDTO.getLandName());
        plotQueryWrapper.like(StringUtils.isNotBlank(landPlotQueryDTO.getAddress()), LandPlot::getAddress, landPlotQueryDTO.getAddress());
        plotQueryWrapper.like(StringUtils.isNotBlank(landPlotQueryDTO.getMeasurementMethod()), LandPlot::getMeasurementMethod, landPlotQueryDTO.getMeasurementMethod());

        // 时间比较逻辑,分开分别判断
        // 开始时间判断
        if (StringUtils.isNotBlank(landPlotQueryDTO.getStartTime())) {
            plotQueryWrapper.apply("create_time >= TO_TIMESTAMP({0}, 'YYYY-MM-DD HH24:MI:SS')",
                    landPlotQueryDTO.getStartTime());
        }
        // 结束时间判断
        if (StringUtils.isNotBlank(landPlotQueryDTO.getEndTime())) {
            plotQueryWrapper.apply("create_time <= TO_TIMESTAMP({0}, 'YYYY-MM-DD HH24:MI:SS')",
                    landPlotQueryDTO.getEndTime());
        }

        // 面积比较逻辑，使用 CAST 将字符串转换为 numeric 类型
        // 最小面积判断
        if (StringUtils.isNotBlank(landPlotQueryDTO.getBeginArea())) {
            plotQueryWrapper.apply("land_area >= CAST({0} AS NUMERIC)",
                    landPlotQueryDTO.getBeginArea());
        }

        // 最大面积判断
        if (StringUtils.isNotBlank(landPlotQueryDTO.getEndArea())) {
            plotQueryWrapper.apply("land_area <= CAST({0} AS NUMERIC)",
                    landPlotQueryDTO.getEndArea());
        }

        // 获取当前登录用户ID，只有明确传入isCurrent=false时才查询所有,否则就根据创建人或者拥有者查询
        plotQueryWrapper.eq(landPlotQueryDTO.isCurrent(), LandPlot::getCreateBy, SecurityUtils.getUserId().toString());
        // 降序
        plotQueryWrapper.orderByDesc(LandPlot::getCreateTime);

        try {
            IPage<LandPlot> page = page(new Page<>(landPlotQueryDTO.getCurrent(), landPlotQueryDTO.getSize()), plotQueryWrapper);
            // 处理记录，将createBy和updateBy转换为用户名，抽象方法
            List<LandPlot> records = handleRecords(page.getRecords());
            long total = page.getTotal();

            AjaxResult ajax = AjaxResult.success();
            ajax.put("data", records);
            ajax.put("total", total);

            return ajax;
        } catch (Exception e) {
            throw new LandPlotException("查询地块失败：" + e.getMessage(), e);
        }
    }

    @DS("master")
    public List<LandPlot> handleRecords(List<LandPlot> records) {
        records.forEach(landPlot -> {
            // 获取创建人
            String createBy = sysUserService.selectUserById(Long.parseLong(landPlot.getCreateBy())).getUserName();
            if (StringUtils.isNotEmpty(createBy)){
                landPlot.setCreateBy(createBy);
            }
            // 获取更新人
            String updateBy = sysUserService.selectUserById(Long.parseLong(landPlot.getUpdateBy())).getUserName();
            if (StringUtils.isNotEmpty(landPlot.getUpdateBy())){
                landPlot.setUpdateBy(updateBy);
            }

        });
        return records;
    }


    @Override
    public List<HeatmapDataVO> getHeatmapData() {
        return landPlotMapper.queryHeatmapData();
    }

    /**
     * 获取指定行政区划级别下的地块面积统计
     *
     * @param addressCode 行政区划代码
     * @param level       行政区划级别(省/市/区/乡/村)
     * @return 返回地址-面积映射的统计结果
     */
    @Override
    @Transactional
    public AjaxResult getArea(Long addressCode, int level) {
        try {
            if (!isValidLevel(level)) {
                throw new LandPlotException(LandPlotConstants.LAND_PLOT_AREA_ERROR_EXCEPTION + "无效的行政区划级别" + level);
            }

            List<LandAddress> addressList = getAddressList(addressCode, level);
            Map<String, AddressAreaDTO> areaStatistics = calculateAreaStatistics(addressList);

            return AjaxResult.success(areaStatistics);
        } catch (Exception e) {
            throw new LandPlotException(LandPlotConstants.LAND_PLOT_AREA_ERROR_EXCEPTION, e);
        }
    }

    @Override
    public double getAreaByAddress(String address) {
        return list(new LambdaQueryWrapper<LandPlot>()
                .like(LandPlot::getAddress, address).eq(LandPlot::getIsDeleted, Constants.NOT_DELETED))
                .stream()
                .mapToDouble(LandPlot::getLandArea)
                .sum();
    }

    /**
     * 校验行政区划级别是否有效
     */
    private boolean isValidLevel(int level) {
        return level == Constants.PROVINCE
                || level == Constants.CITY
                || level == Constants.DISTRICT
                || level == Constants.VILLAGE
                || level == Constants.HAMLET;
    }

    /**
     * 根据行政区划代码和级别获取地址列表
     *
     * @param addressCode 行政区划代码
     * @param level       行政区划级别
     * @return 地址列表
     */
    private List<LandAddress> getAddressList(Long addressCode, int level) {
        QueryWrapper<LandAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Constants.AREA_LEVEL, level);

        // 如果是查询全国省份数据,不需要上级编码条件
        if (!(addressCode == 0 && level == Constants.PROVINCE)) {
            queryWrapper.eq(Constants.SENIOR_CODE, addressCode);
        }

        return landAddressService.list(queryWrapper);
    }

    /**
     * 计算地址列表中每个地址的地块面积总和
     *
     * @param addressList 地址列表
     * @return 地址-面积映射
     */
    private Map<String, AddressAreaDTO> calculateAreaStatistics(List<LandAddress> addressList) {
        return addressList.stream()
                .collect(Collectors.toMap(
                        LandAddress::getName,
                        address -> {
                            double area = calculateAddressArea(address);
                            return new AddressAreaDTO(address.getCode(), address.getName(), address.getLevel(), address.getPcode(), address.getCategory(), area);
                        }
                ));
    }

    /**
     * 计算单个地址下的地块面积总和
     *
     * @param address 地址对象
     * @return 面积总和
     */
    private double calculateAddressArea(LandAddress address) {
        try {
            List<LandPlot> landPlots = query()
                    .like("address", address.getName())
                    .eq("is_deleted", Constants.NOT_DELETED)
                    .list();

            return landPlots.stream()
                    .map(LandPlot::getLandArea)
                    .map(Object::toString)
                    .mapToDouble(Double::parseDouble)
                    .sum();
        } catch (Exception e) {
            throw new LandPlotException("计算地址面积失败：" + e.getMessage(), e);
        }
    }

}
