package com.agri.cms.web.controller.trajectory;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.domain.model.LoginUser;
import com.agri.cms.common.core.page.TableDataInfo;
import com.agri.cms.common.utils.StringUtils;
import com.agri.cms.common.utils.SecurityUtils;
import com.agri.cms.system.domain.EffectivePoint;
import com.agri.cms.system.domain.vo.SortMessage;
import com.agri.cms.system.domain.vo.Trajectory;
import com.agri.cms.system.service.InfluxDBEffectiveTrajectoryService;
import com.agri.cms.system.service.LandPlotService;
import com.agri.cms.system.service.TrajectoryInfoService;
import com.agri.cms.system.service.TrajectoryPointsService;
import com.agri.cms.system.service.impl.CheckPathServiceImpl;
import com.agri.cms.system.service.impl.GaodeMapService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author 姚锦伦
 */
@RestController
@Tag(name = "有效轨迹API")
@RequestMapping("effectiveTrajectory")
public class EffectiveTrajectoryController extends BaseController {
    @Autowired
    private InfluxDBEffectiveTrajectoryService influxDBEffectiveTrajectoryService;
    @Autowired
    private TrajectoryPointsService trajectoryPointsService;
    @Autowired
    private CheckPathServiceImpl checkPathService;

    /**
     * 查询当前用户所有的作业轨迹信息
     *
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/9/24
     */
    @PostMapping
    @Operation(summary = "查询当前用户的轨迹")
    @PreAuthorize("@ss.hasPermi('trajectory:effective:list')")
    public AjaxResult queryEffectiveTrajectory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "6") Integer pageSize) {
        AjaxResult ajaxResult = AjaxResult.success();
        LoginUser loginUser = getLoginUser();
        //根据当前用户手机号查询轨迹信息
        Page<Trajectory> trajectories = trajectoryPointsService.queryAll(loginUser.getUsername(), pageNum, pageSize);
        ajaxResult.put("total", trajectories.getTotal());
        return ajaxResult.put("data", trajectories.getRecords());
    }


    /**
     * 通过条件查询有效轨迹信息
     *
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/9/25
     */
    @PostMapping("/conditions")
    @Operation(summary = "通过条件查询轨迹信息")
    @PreAuthorize("@ss.hasPermi('trajectory:effective:search')")
    public AjaxResult queryEffectiveTrajectoryByConditions(@RequestBody SortMessage sortMessage,
                                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "6") Integer pageSize) {
        AjaxResult ajaxResult = AjaxResult.success();
        LoginUser loginUser = getLoginUser();
        Page<Trajectory> trajectories = new Page<>();
        if (StringUtils.isEmpty(sortMessage.getPhone())) {
            trajectories = trajectoryPointsService.queryByConditions(sortMessage, loginUser.getUsername(), pageNum, pageSize);
        } else {
            if (!sortMessage.getPhone().equals(loginUser.getUsername()) && !SecurityUtils.isAdmin(getUserId()) && !SecurityUtils.hasRole("admin")) {
                return AjaxResult.error(403,"无权查询其他终端的轨迹");
            }
            trajectories = trajectoryPointsService.queryByConditions(sortMessage, sortMessage.getPhone(), pageNum, pageSize);
        }

        if (trajectories.getTotal() == 0) {
            return AjaxResult.success("没查询到数据");
        }
        ajaxResult.put("total", trajectories.getTotal());
        ajaxResult.put("data", trajectories.getRecords());

        return ajaxResult;
    }

    /**
     * 生成轨迹
     *
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/9/27
     */
    @GetMapping("/byPhoneNumber")
    @Operation(summary = "生成轨迹")
    @PreAuthorize("@ss.hasPermi('trajectory:effective:generate')")
    public AjaxResult queryEffectiveTrajectoryByPhoneNumber(LocalDateTime startTime, LocalDateTime endTime) {
        //判断时间是否为空
        if (ObjectUtil.isEmpty(startTime) || ObjectUtil.isEmpty(endTime) || startTime.isAfter(endTime)){
            return AjaxResult.error("时间为空或格式错误");
        }
        influxDBEffectiveTrajectoryService.createTrajectory(getLoginUser().getUsername(), startTime, endTime);

        return AjaxResult.success();
    }

    @DeleteMapping("/signOut/{username}")
    @Operation(summary = "注销该用户所有轨迹")
    @PreAuthorize("@ss.hasPermi('trajectory:effective:signout')")
    public AjaxResult deleteByUsername(@PathVariable String username) {
        influxDBEffectiveTrajectoryService.loginOut(username);
        return AjaxResult.success();
    }
}
