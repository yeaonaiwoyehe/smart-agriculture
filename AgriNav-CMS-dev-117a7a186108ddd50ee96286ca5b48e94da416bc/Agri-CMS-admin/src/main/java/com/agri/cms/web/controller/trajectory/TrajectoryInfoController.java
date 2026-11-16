package com.agri.cms.web.controller.trajectory;

import com.agri.cms.common.annotation.Anonymous;
import com.agri.cms.common.core.controller.BaseController;
import com.agri.cms.common.core.domain.AjaxResult;
import com.agri.cms.common.core.domain.model.LoginUser;
import com.agri.cms.system.domain.TrajectoryInfo;
import com.agri.cms.system.service.SearchEngineService;
import com.agri.cms.system.service.TrajectoryInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * @author 姚锦伦
 */
@RestController
@RequestMapping("/trajectoryInfo")
@Tag(name = "轨迹信息")
public class TrajectoryInfoController extends BaseController {
    @Autowired
    private TrajectoryInfoService trajectoryInfoService;


    @Resource
    private SearchEngineService searchEngineService;

    /**
     * 根据轨迹信息上传轨迹信息
     *
     * @param trajectoryInfo
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/9/24
     */
    @PostMapping
    @Operation(summary = "上传轨迹信息")
    public AjaxResult add(@RequestBody TrajectoryInfo trajectoryInfo) {
        AjaxResult result = AjaxResult.success();
        LoginUser loginUser = getLoginUser();
        trajectoryInfo.setPhoneNumber(loginUser.getUsername());
        trajectoryInfo.setCreatedBy(loginUser.getUsername());
        trajectoryInfo.setCreatedTime(LocalDateTime.now());
        boolean isInsert = trajectoryInfoService.saveOrUpdate(trajectoryInfo);
        if (!isInsert) {
            return AjaxResult.error("插入失败");
        }
        return result;
    }

    /**
     * 生成轨迹id
     *
     * @param phoneNumber
     * @return AjaxResult
     * @author 姚锦伦
     * @since 2024/9/26
     */
    @Operation(summary = "生成轨迹id")
    @GetMapping("/create")
    public AjaxResult createTrajectoryId(String phoneNumber) {
        AjaxResult result = AjaxResult.success();
        //生成轨迹id
        String trajectoryId = phoneNumber + ";" + Instant.now().toEpochMilli();
        //将轨迹id插入到轨迹信息表中
        TrajectoryInfo trajectoryInfo = new TrajectoryInfo();
        trajectoryInfo.setTrajectoryId(trajectoryId);
        boolean isInsert = trajectoryInfoService.saveOrUpdate(trajectoryInfo);
        if (!isInsert) {
            return AjaxResult.error("生成轨迹ID失败");
        }
        return result.put("trajectoryId", trajectoryId);
    }

    @GetMapping("/picture")
    @Anonymous
    public AjaxResult getTrajectoryPictures(String phoneNumber, LocalDateTime startTime, LocalDateTime endTime) {
        return success(searchEngineService.findByClientIdAndTimeRange(phoneNumber, startTime, endTime));
    }

}
